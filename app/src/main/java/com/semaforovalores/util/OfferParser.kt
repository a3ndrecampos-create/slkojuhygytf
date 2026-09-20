package com.semaforovalores.util

import com.semaforovalores.model.SourceApp
import com.semaforovalores.model.TripOffer

/**
 * Interpreta a lista de textos visíveis na tela de oferta e monta um [TripOffer].
 *
 * Layout do Uber (motorista) usado como referência:
 *   "R$ 16,07"                      -> valor da corrida
 *   "R$1,89/km aprox."              -> ignorado (não é o valor)
 *   "4,93 (742)"                    -> nota do passageiro
 *   "+R$ 2,25 incluído"             -> ignorado
 *   "6 min (3.7 km)"                -> trecho 1: buscar o passageiro
 *   "13 minutos (4.8 km)"           -> trecho 2: a viagem
 *
 * Distância e tempo usados no cálculo = busca + viagem (é o que o motorista
 * realmente roda; bate com o "R$/km aprox." mostrado pelo próprio Uber).
 */
object OfferParser {

    private val FARE = Regex("""^R\$\s*(\d{1,3}(?:\.\d{3})*(?:,\d{1,2})?|\d+(?:,\d{1,2})?)$""")
    private val LEG = Regex(
        """(?:(\d+)\s*h(?:oras?)?\s*)?(\d+)\s*min(?:utos?)?\s*\(\s*([\d.,]+)\s*km\s*\)""",
        RegexOption.IGNORE_CASE
    )
    private val RATING = Regex("""^([1-5][.,]\d{1,2})(?:\s*\(\s*[\d.,]+\s*\))?$""")

    data class Leg(val minutes: Int, val km: Double)

    fun parse(texts: List<String>, source: SourceApp): TripOffer? {
        val fare = texts.firstNotNullOfOrNull { parseFare(it) } ?: return null

        val legs = texts.flatMap { text ->
            LEG.findAll(text).mapNotNull { m ->
                val hours = m.groupValues[1].toIntOrNull() ?: 0
                val mins = m.groupValues[2].toIntOrNull() ?: return@mapNotNull null
                val km = m.groupValues[3].replace(',', '.').toDoubleOrNull()
                    ?: return@mapNotNull null
                Leg(hours * 60 + mins, km)
            }.toList()
        }
        if (legs.isEmpty()) return null

        // Usa no máximo os dois primeiros trechos (busca + viagem)
        val used = legs.take(2)
        val distance = used.sumOf { it.km }
        val duration = used.sumOf { it.minutes }
        if (distance <= 0.0 || duration <= 0) return null

        val rating = texts.firstNotNullOfOrNull { parseRating(it) } ?: 5.0

        return TripOffer(distance, duration, fare, rating, source)
    }

    fun parseFare(text: String): Double? {
        val m = FARE.find(text.trim()) ?: return null
        return m.groupValues[1].replace(".", "").replace(',', '.').toDoubleOrNull()
    }

    fun parseRating(text: String): Double? {
        val m = RATING.find(text.trim()) ?: return null
        return m.groupValues[1].replace(',', '.').toDoubleOrNull()
    }
}
