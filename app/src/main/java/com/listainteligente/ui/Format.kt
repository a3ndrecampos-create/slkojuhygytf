package com.listainteligente.ui

import java.text.NumberFormat
import java.util.Locale

private val brFormat: NumberFormat = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

/** "R$ 1.234,56" — formato de moeda brasileiro, igual em todas as telas. */
fun Double.brl(): String = brFormat.format(this)

/** "12,99" — usado para preencher campos de texto editáveis (sem símbolo de moeda). */
fun Double.asInput(): String = String.format(Locale.US, "%.2f", this).replace('.', ',')

/** Converte o que o usuário digitou ("12,99", "12.99", "12") em Double. Null se inválido. */
fun String.parseMoney(): Double? = trim().replace(',', '.').toDoubleOrNull()
