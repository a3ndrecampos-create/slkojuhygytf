package com.semaforovalores.util

import com.semaforovalores.model.TripColor
import com.semaforovalores.model.TripOffer
import com.semaforovalores.model.UserSettings

object TripClassifier {

    /**
     * Classifica uma corrida como VERDE, AMARELA ou VERMELHA.
     * A cor final é a pior entre todas as métricas avaliadas.
     */
    fun classify(offer: TripOffer, settings: UserSettings): TripColor {
        val costPerKm = settings.fuelCostPerKm()

        val colors = listOf(
            classifyMetric(
                value = offer.ratePerKm(),
                greenMin = settings.greenMinRatePerKm,
                yellowMin = settings.yellowMinRatePerKm
            ),
            classifyMetric(
                value = offer.ratePerHour(),
                greenMin = settings.greenMinRatePerHour,
                yellowMin = settings.yellowMinRatePerHour
            ),
            classifyMetric(
                value = offer.passengerRating,
                greenMin = settings.greenMinPassengerRating,
                yellowMin = settings.yellowMinPassengerRating
            ),
            classifyMetric(
                value = offer.profitPercent(costPerKm),
                greenMin = settings.greenMinProfitPercent,
                yellowMin = settings.yellowMinProfitPercent
            )
        )

        // Valor mínimo da viagem
        val minValueColor = if (settings.minTripValue > 0 && offer.fareEstimated < settings.minTripValue)
            TripColor.RED else TripColor.GREEN

        return worstColor(colors + listOf(minValueColor))
    }

    private fun classifyMetric(value: Double, greenMin: Double, yellowMin: Double): TripColor {
        return when {
            value >= greenMin -> TripColor.GREEN
            value >= yellowMin -> TripColor.YELLOW
            else -> TripColor.RED
        }
    }

    private fun worstColor(colors: List<TripColor>): TripColor {
        return when {
            colors.any { it == TripColor.RED } -> TripColor.RED
            colors.any { it == TripColor.YELLOW } -> TripColor.YELLOW
            else -> TripColor.GREEN
        }
    }

    /** Texto da ação sugerida baseado na cor */
    fun actionLabel(color: TripColor) = when (color) {
        TripColor.GREEN -> "Aceitar"
        TripColor.YELLOW -> "Avaliar"
        TripColor.RED -> "Recusar"
    }
}
