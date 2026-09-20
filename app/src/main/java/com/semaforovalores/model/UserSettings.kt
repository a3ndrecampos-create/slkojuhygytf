package com.semaforovalores.model

data class UserSettings(
    // Limites de cor — R$/km
    val greenMinRatePerKm: Double = 1.80,
    val yellowMinRatePerKm: Double = 1.40,

    // Limites de cor — R$/hora
    val greenMinRatePerHour: Double = 40.0,
    val yellowMinRatePerHour: Double = 30.0,

    // Limites de cor — nota do passageiro
    val greenMinPassengerRating: Double = 4.90,
    val yellowMinPassengerRating: Double = 4.30,

    // Limites de cor — % de lucro (após combustível)
    val greenMinProfitPercent: Double = 50.0,
    val yellowMinProfitPercent: Double = 30.0,

    // Combustível
    val fuelPricePerLiter: Double = 6.50,       // R$/litro
    val vehicleConsumptionKmL: Double = 12.0,   // km/litro

    // Apps monitorados
    val monitorUber: Boolean = true,
    val monitorNinetyNine: Boolean = true,
    val monitorInDrive: Boolean = true,

    // Ganho mínimo por corrida (0 = desativado)
    val minTripValue: Double = 0.0,

    // Visual
    val showFuelDetails: Boolean = true
) {
    /** Custo de combustível por km calculado a partir do veículo */
    fun fuelCostPerKm(): Double =
        if (vehicleConsumptionKmL > 0) fuelPricePerLiter / vehicleConsumptionKmL else 0.42
}
