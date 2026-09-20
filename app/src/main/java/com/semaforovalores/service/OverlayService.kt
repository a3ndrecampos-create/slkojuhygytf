package com.semaforovalores.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.semaforovalores.R
import com.semaforovalores.model.SourceApp
import com.semaforovalores.model.TripColor
import com.semaforovalores.model.TripHistory
import com.semaforovalores.model.TripOffer
import com.semaforovalores.model.UserSettings
import com.semaforovalores.semaforoApp
import com.semaforovalores.ui.overlay.TripOverlayCard
import com.semaforovalores.ui.screens.MainActivity
import com.semaforovalores.ui.theme.SemaforoTheme
import com.semaforovalores.util.TripClassifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

private data class OverlayState(
    val offer: TripOffer,
    val color: TripColor,
    val settings: UserSettings
)

/**
 * Serviço em primeiro plano: escuta as ofertas enviadas pelo
 * RideAccessibilityService e mostra o card do semáforo sobre os outros apps.
 */
class OverlayService : Service() {

    companion object {
        /** Usado pela tela inicial para saber se o monitoramento está ligado. */
        val running = MutableStateFlow(false)

        private const val CHANNEL_ID = "semaforo_overlay"
        private const val NOTIFICATION_ID = 1001
        private const val AUTO_HIDE_MS = 15_000L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val handler = Handler(Looper.getMainLooper())
    private val hideRunnable = Runnable { hideOverlay() }
    private val lifecycleOwner = OverlayLifecycleOwner()

    private lateinit var windowManager: WindowManager
    private var overlayView: ComposeView? = null
    private var settings = UserSettings()
    private var overlayState by mutableStateOf<OverlayState?>(null)

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent != null) handleOffer(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        createNotificationChannel()
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(),
            if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST
        )

        lifecycleOwner.onCreate()
        lifecycleOwner.onResume()

        ContextCompat.registerReceiver(
            this,
            receiver,
            IntentFilter(RideAccessibilityService.ACTION_TRIP_OFFER),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        scope.launch {
            semaforoApp().settingsRepository.settings.collect { settings = it }
        }

        running.value = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        running.value = false
        runCatching { unregisterReceiver(receiver) }
        handler.removeCallbacks(hideRunnable)
        hideOverlay()
        lifecycleOwner.onDestroy()
        scope.cancel()
        super.onDestroy()
    }

    // ------------------------------------------------------------------ ofertas

    private fun handleOffer(intent: Intent) {
        if (!Settings.canDrawOverlays(this)) return

        val source = runCatching {
            SourceApp.valueOf(intent.getStringExtra(RideAccessibilityService.EXTRA_SOURCE) ?: "")
        }.getOrNull() ?: return

        val s = settings
        val monitored = when (source) {
            SourceApp.UBER -> s.monitorUber
            SourceApp.NINETY_NINE -> s.monitorNinetyNine
            SourceApp.INDRIVE -> s.monitorInDrive
        }
        if (!monitored) return

        val offer = TripOffer(
            distanceKm = intent.getDoubleExtra(RideAccessibilityService.EXTRA_DISTANCE, 0.0),
            durationMin = intent.getIntExtra(RideAccessibilityService.EXTRA_DURATION, 0),
            fareEstimated = intent.getDoubleExtra(RideAccessibilityService.EXTRA_FARE, 0.0),
            passengerRating = intent.getDoubleExtra(RideAccessibilityService.EXTRA_RATING, 5.0),
            sourceApp = source
        )
        if (offer.distanceKm <= 0.0 || offer.fareEstimated <= 0.0) return

        // Mesma oferta já na tela: só reinicia o timer
        if (overlayState?.offer == offer && overlayView != null) {
            scheduleAutoHide()
            return
        }

        showOverlay(OverlayState(offer, TripClassifier.classify(offer, s), s))
    }

    // ------------------------------------------------------------------ overlay

    private fun showOverlay(state: OverlayState) {
        overlayState = state
        if (overlayView == null) {
            val view = ComposeView(this).apply {
                setViewTreeLifecycleOwner(lifecycleOwner)
                setViewTreeViewModelStoreOwner(lifecycleOwner)
                setViewTreeSavedStateRegistryOwner(lifecycleOwner)
                setContent {
                    OverlayContent(
                        state = overlayState,
                        onAccept = { record(true) },
                        onDecline = { record(false) }
                    )
                }
            }
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP
                y = 96
            }
            runCatching { windowManager.addView(view, params) }
                .onSuccess { overlayView = view }
        }
        scheduleAutoHide()
    }

    private fun scheduleAutoHide() {
        handler.removeCallbacks(hideRunnable)
        handler.postDelayed(hideRunnable, AUTO_HIDE_MS)
    }

    private fun hideOverlay() {
        handler.removeCallbacks(hideRunnable)
        overlayView?.let { view ->
            runCatching { windowManager.removeView(view) }
        }
        overlayView = null
        overlayState = null
    }

    private fun record(accepted: Boolean) {
        val state = overlayState
        hideOverlay()
        if (state == null) return
        val costPerKm = state.settings.fuelCostPerKm()
        val trip = TripHistory(
            distanceKm = state.offer.distanceKm,
            durationMin = state.offer.durationMin,
            fareEstimated = state.offer.fareEstimated,
            passengerRating = state.offer.passengerRating,
            fuelCost = state.offer.fuelCost(costPerKm),
            netProfit = state.offer.netProfit(costPerKm),
            color = state.color,
            accepted = accepted,
            sourceApp = state.offer.sourceApp
        )
        scope.launch(Dispatchers.IO) {
            semaforoApp().database.tripDao().insert(trip)
        }
    }

    // ------------------------------------------------------------- notificação

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_stat_semaforo)
        .setContentTitle(getString(R.string.notification_title))
        .setContentText(getString(R.string.notification_text))
        .setOngoing(true)
        .setContentIntent(
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        )
        .build()
}

@Composable
private fun OverlayContent(
    state: OverlayState?,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    SemaforoTheme {
        if (state != null) {
            TripOverlayCard(
                offer = state.offer,
                settings = state.settings,
                color = state.color,
                onAccept = onAccept,
                onDecline = onDecline,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}
