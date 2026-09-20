package com.semaforovalores.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import com.semaforovalores.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.settingsDataStore by preferencesDataStore(name = "semaforo_settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val greenKm = doublePreferencesKey("green_min_rate_per_km")
        val yellowKm = doublePreferencesKey("yellow_min_rate_per_km")
        val greenHour = doublePreferencesKey("green_min_rate_per_hour")
        val yellowHour = doublePreferencesKey("yellow_min_rate_per_hour")
        val greenRating = doublePreferencesKey("green_min_rating")
        val yellowRating = doublePreferencesKey("yellow_min_rating")
        val greenProfit = doublePreferencesKey("green_min_profit")
        val yellowProfit = doublePreferencesKey("yellow_min_profit")
        val fuelPrice = doublePreferencesKey("fuel_price_per_liter")
        val consumption = doublePreferencesKey("vehicle_consumption")
        val monitorUber = booleanPreferencesKey("monitor_uber")
        val monitor99 = booleanPreferencesKey("monitor_99")
        val monitorInDrive = booleanPreferencesKey("monitor_indrive")
        val minTripValue = doublePreferencesKey("min_trip_value")
        val showFuel = booleanPreferencesKey("show_fuel_details")
    }

    val settings: Flow<UserSettings> = context.settingsDataStore.data
        .catch { e ->
            if (e is IOException) emit(emptyPreferences()) else throw e
        }
        .map { p ->
            val d = UserSettings()
            UserSettings(
                greenMinRatePerKm = p[Keys.greenKm] ?: d.greenMinRatePerKm,
                yellowMinRatePerKm = p[Keys.yellowKm] ?: d.yellowMinRatePerKm,
                greenMinRatePerHour = p[Keys.greenHour] ?: d.greenMinRatePerHour,
                yellowMinRatePerHour = p[Keys.yellowHour] ?: d.yellowMinRatePerHour,
                greenMinPassengerRating = p[Keys.greenRating] ?: d.greenMinPassengerRating,
                yellowMinPassengerRating = p[Keys.yellowRating] ?: d.yellowMinPassengerRating,
                greenMinProfitPercent = p[Keys.greenProfit] ?: d.greenMinProfitPercent,
                yellowMinProfitPercent = p[Keys.yellowProfit] ?: d.yellowMinProfitPercent,
                fuelPricePerLiter = p[Keys.fuelPrice] ?: d.fuelPricePerLiter,
                vehicleConsumptionKmL = p[Keys.consumption] ?: d.vehicleConsumptionKmL,
                monitorUber = p[Keys.monitorUber] ?: d.monitorUber,
                monitorNinetyNine = p[Keys.monitor99] ?: d.monitorNinetyNine,
                monitorInDrive = p[Keys.monitorInDrive] ?: d.monitorInDrive,
                minTripValue = p[Keys.minTripValue] ?: d.minTripValue,
                showFuelDetails = p[Keys.showFuel] ?: d.showFuelDetails
            )
        }

    suspend fun save(s: UserSettings) {
        context.settingsDataStore.edit { p ->
            p[Keys.greenKm] = s.greenMinRatePerKm
            p[Keys.yellowKm] = s.yellowMinRatePerKm
            p[Keys.greenHour] = s.greenMinRatePerHour
            p[Keys.yellowHour] = s.yellowMinRatePerHour
            p[Keys.greenRating] = s.greenMinPassengerRating
            p[Keys.yellowRating] = s.yellowMinPassengerRating
            p[Keys.greenProfit] = s.greenMinProfitPercent
            p[Keys.yellowProfit] = s.yellowMinProfitPercent
            p[Keys.fuelPrice] = s.fuelPricePerLiter
            p[Keys.consumption] = s.vehicleConsumptionKmL
            p[Keys.monitorUber] = s.monitorUber
            p[Keys.monitor99] = s.monitorNinetyNine
            p[Keys.monitorInDrive] = s.monitorInDrive
            p[Keys.minTripValue] = s.minTripValue
            p[Keys.showFuel] = s.showFuelDetails
        }
    }
}
