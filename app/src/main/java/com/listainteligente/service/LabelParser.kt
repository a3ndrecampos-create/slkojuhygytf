package com.listainteligente.service

import com.listainteligente.model.PriceOption
import com.listainteligente.model.ScannedLabel

/**
 * Interpreta o texto bruto do ML Kit OCR e extrai
 * produto, preços e condições da etiqueta.
 *
 * Suporta formatos Max Atacadista, Assaí, Atacadão,
 * e etiquetas padrão de supermercado.
 */
object LabelParser {

    fun parse(rawText: String): ScannedLabel {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val text  = rawText.uppercase()

        val productName = extractProductName(lines)
        val ean         = extractEan(text)
        val prices      = extractPrices(lines, text)

        return ScannedLabel(
            productName  = productName,
            ean          = ean,
            priceOptions = prices,
            rawText      = rawText
        )
    }

    // ── Nome do produto ──────────────────────────────────────────────────────

    private fun extractProductName(lines: List<String>): String {
        // Primeira linha geralmente é o nome
        val raw = lines.firstOrNull { line ->
            line.length > 4 &&
            !line.matches(Regex(".*R\\\$.*")) &&
            !line.contains("VAREJO", ignoreCase = true) &&
            !line.contains("ATACADO", ignoreCase = true) &&
            !line.contains("EAN", ignoreCase = true)
        } ?: return "Produto"

        // Etiquetas reais costumam vir com sujeira no final, ex: "PARAFUSO, ."
        val cleaned = raw
            .trim()
            .trimEnd('.', ',', ' ', '-', ':')
            .replace(Regex("\\s{2,}"), " ")
            .take(60)

        return cleaned.ifBlank { "Produto" }
    }

    // ── EAN / código de barras ───────────────────────────────────────────────

    private fun extractEan(text: String): String {
        return Regex("""EAN[:\s]*(\d{8,14})""").find(text)
            ?.groupValues?.get(1) ?: ""
    }

    // ── Extração de preços ───────────────────────────────────────────────────

    private fun extractPrices(lines: List<String>, text: String): List<PriceOption> {
        val options = mutableListOf<PriceOption>()

        // Estratégia: mapear blocos da etiqueta
        val blocks = segmentBlocks(lines)

        for (block in blocks) {
            val blockText = block.joinToString(" ").uppercase()
            val price = extractPrice(block) ?: continue

            when {
                // Clube / App (sempre aparece no bloco escuro direito)
                blockText.contains("CLUBE") || blockText.contains("APP") -> {
                    val limit = extractLimit(blockText)
                    options.add(PriceOption(
                        label        = if (blockText.contains("APP")) "Preço App" else "Clube",
                        price        = price,
                        minQty       = 1,
                        maxQty       = limit,
                        requiresApp  = true
                    ))
                }

                // Atacado
                blockText.contains("ATACADO") && !blockText.contains("CREDIFFATO") -> {
                    val minQty = extractMinQty(blockText)
                    options.add(PriceOption(
                        label  = "Atacado",
                        price  = price,
                        minQty = minQty
                    ))
                }

                // Crediffato (cartão da loja)
                blockText.contains("CREDIFFATO") -> {
                    val minQty = extractMinQty(blockText)
                    options.add(PriceOption(
                        label         = "Crediffato",
                        price         = price,
                        minQty        = minQty,
                        requiresCard  = true
                    ))
                }

                // Atacado OU Crediffato (mesmo preço)
                blockText.contains("ATACADO") && blockText.contains("CREDIFFATO") -> {
                    val minQty = extractMinQty(blockText)
                    options.add(PriceOption(
                        label         = "Atacado / Crediffato",
                        price         = price,
                        minQty        = minQty,
                        requiresCard  = true
                    ))
                }

                // Varejo (preço cheio)
                blockText.contains("VAREJO") || options.isEmpty() -> {
                    options.add(0, PriceOption(
                        label  = "Varejo",
                        price  = price,
                        minQty = 1
                    ))
                }
            }
        }

        // Fallback: se não encontrou nada estruturado, pega todos os preços
        if (options.isEmpty()) {
            extractAllPrices(text).forEachIndexed { i, p ->
                options.add(PriceOption(label = if (i == 0) "Preço 1" else "Preço 2", price = p))
            }
        }

        return options.sortedByDescending { it.price } // varejo primeiro (maior)
    }

    // ── Segmenta linhas em blocos lógicos ────────────────────────────────────

    private fun segmentBlocks(lines: List<String>): List<List<String>> {
        val blocks   = mutableListOf<MutableList<String>>()
        var current  = mutableListOf<String>()

        val separators = setOf("VAREJO", "ATACADO", "CLUBE", "APP", "CREDIFFATO")

        for (line in lines) {
            val up = line.uppercase()
            if (separators.any { up.contains(it) } && current.isNotEmpty()) {
                blocks.add(current)
                current = mutableListOf()
            }
            current.add(line)
        }
        if (current.isNotEmpty()) blocks.add(current)

        return blocks
    }

    // ── Extrai preço de um bloco de linhas ──────────────────────────────────

    private fun extractPrice(lines: List<String>): Double? {
        val joined = lines.joinToString(" ")
        return Regex("""R\$\s*(\d+)[,\.](\d{2})""")
            .find(joined)
            ?.let { "${it.groupValues[1]}.${it.groupValues[2]}".toDoubleOrNull() }
            ?: Regex("""(\d+)[,\.](\d{2})""")
                .findAll(joined)
                .mapNotNull { "${it.groupValues[1]}.${it.groupValues[2]}".toDoubleOrNull() }
                .firstOrNull { it in 0.50..9999.0 }
    }

    private fun extractAllPrices(text: String): List<Double> =
        Regex("""R\$\s*(\d+)[,\.](\d{2})""").findAll(text)
            .map { "${it.groupValues[1]}.${it.groupValues[2]}".toDouble() }
            .filter { it in 0.50..9999.0 }
            .toList()

    // ── Quantidade mínima para desconto ─────────────────────────────────────

    private fun extractMinQty(text: String): Int {
        return Regex("""A PARTIR DE\s*(\d+)""").find(text)?.groupValues?.get(1)?.toIntOrNull()
            ?: Regex("""PARTIR[^\d]*(\d+)""").find(text)?.groupValues?.get(1)?.toIntOrNull()
            ?: Regex("""MIN[^\d]*(\d+)""").find(text)?.groupValues?.get(1)?.toIntOrNull()
            ?: 1
    }

    // ── Limite máximo de unidades ────────────────────────────────────────────

    private fun extractLimit(text: String): Int? {
        return Regex("""LIMITADO A\s*(\d+)""").find(text)?.groupValues?.get(1)?.toIntOrNull()
            ?: Regex("""LIMIT[^\d]*(\d+)""").find(text)?.groupValues?.get(1)?.toIntOrNull()
    }
}
