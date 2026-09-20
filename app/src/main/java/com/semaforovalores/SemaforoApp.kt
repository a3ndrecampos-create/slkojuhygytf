package com.semaforovalores

import android.app.Application
import android.content.Context
import com.semaforovalores.data.AppDatabase
import com.semaforovalores.data.SettingsRepository

class SemaforoApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.build(this) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(this) }
}

fun Context.semaforoApp(): SemaforoApp = applicationContext as SemaforoApp
