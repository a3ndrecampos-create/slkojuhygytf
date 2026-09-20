package com.semaforovalores.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TripColor { GREEN, YELLOW, RED }
enum class SourceApp { UBER, NINETY_NINE, INDRIVE }

data class TripOffer(
    val distanceKm: Double,
    val durationMin: Int,
    val fareEstimated: Double,
    val passengerRating: Double,
    val sourceApp: SourceApp
) {
    fun ratePerKm() = if (distanceKm > 0) fareEstimated / distanceKm else 0.0
    fun ratePerHour() = if (durationMin > 0) (fareEstimated / durationMin) * 60 else 0.0
    fun fuelCost(costPerKm: Double) = distanceKm * costPerKm
    fun netProfit(costPerKm: Double) = fareEstimated - fuelCost(costPerKm)
    fun profitPercent(costPerKm: Double) =
        if (fareEstimated > 0) (netProfit(costPerKm) / fareEstimated) * 100 else 0.0
}

@Entity(tableName = "trip_history")
data class TripHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val distanceKm: Double,
    val durationMin: Int,
    val fareEstimated: Double,
    val passengerRating: Double,
    val fuelCost: Double,
    val netProfit: Double,
    val color: TripColor,
    val accepted: Boolean,
    val sourceApp: SourceApp
)
