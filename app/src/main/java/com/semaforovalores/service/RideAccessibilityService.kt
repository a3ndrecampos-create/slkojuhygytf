package com.semaforovalores.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.semaforovalores.model.SourceApp
import com.semaforovalores.model.TripOffer
import com.semaforovalores.util.OfferParser
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Serviço de Acessibilidade que monitora Uber, 99 e inDrive.
 * Extrai os textos da tela, interpreta com [OfferParser] e envia a oferta
 * ao OverlayService via broadcast restrito ao próprio app.
 */
class RideAccessibilityService : AccessibilityService() {

    companion object {
        const val ACTION_TRIP_OFFER = "com.semaforovalores.TRIP_OFFER"
        const val EXTRA_DISTANCE = "extra_distance"
        const val EXTRA_DURATION = "extra_duration"
        const val EXTRA_FARE = "extra_fare"
        const val EXTRA_RATING = "extra_rating"
        const val EXTRA_SOURCE = "extra_source"

        // Package names dos apps monitorados
        private val UBER_PACKAGES = setOf("com.ubercab.driver", "com.ubercab.eats.driver")
        private val NINETY_NINE_PACKAGES = setOf("com.taxis99.motorista", "com.ninety9")
        private val INDRIVE_PACKAGES = setOf("sinet.startup.inDriver")

        /** Não reenvia a mesma oferta antes deste intervalo (ms). */
        private const val REPEAT_INTERVAL_MS = 4_000L

        // ---- Diagnóstico (mostrado na tela inicial do app) ----
        /** Quantos eventos de apps de corrida o serviço já recebeu. */
        val eventCount = MutableStateFlow(0)
        /** Pacote do último app monitorado que gerou evento. */
        val lastPackage = MutableStateFlow("")
        /** Últimos textos lidos da tela do app de corrida. */
        val lastTexts = MutableStateFlow<List<String>>(emptyList())
        /** Última oferta interpretada com sucesso (resumo em texto). */
        val lastParsed = MutableStateFlow("")
    }

    private var lastOffer: TripOffer? = null
    private var lastSentAt = 0L

    override fun onServiceConnected() {
        val info = serviceInfo ?: AccessibilityServiceInfo()
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        info.packageNames = (UBER_PACKAGES + NINETY_NINE_PACKAGES + INDRIVE_PACKAGES).toTypedArray()
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        info.notificationTimeout = 100
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName?.toString() ?: return
        val sourceApp = detectSourceApp(packageName) ?: return

        eventCount.value = eventCount.value + 1
        lastPackage.value = packageName

        val rootNode = rootInActiveWindow ?: return
        val texts = try {
            getAllTexts(rootNode)
        } catch (e: Exception) {
            return
        }
        if (texts.isEmpty()) return
        if (texts != lastTexts.value) lastTexts.value = texts.take(80)

        val offer = OfferParser.parse(texts, sourceApp) ?: return
        lastParsed.value = "R$ %.2f · %.1f km · %d min · nota %.2f".format(
            offer.fareEstimated, offer.distanceKm, offer.durationMin, offer.passengerRating
        )
        broadcastOffer(offer)
    }

    private fun detectSourceApp(pkg: String): SourceApp? = when {
        UBER_PACKAGES.contains(pkg) -> SourceApp.UBER
        NINETY_NINE_PACKAGES.contains(pkg) -> SourceApp.NINETY_NINE
        INDRIVE_PACKAGES.contains(pkg) -> SourceApp.INDRIVE
        else -> null
    }

    /** Coleta text e contentDescription de todos os nós visíveis. */
    private fun getAllTexts(root: AccessibilityNodeInfo): List<String> {
        val texts = mutableListOf<String>()
        fun traverse(n: AccessibilityNodeInfo?) {
            if (n == null) return
            n.text?.toString()?.trim()?.let { if (it.isNotEmpty()) texts.add(it) }
            n.contentDescription?.toString()?.trim()?.let {
                if (it.isNotEmpty() && !texts.contains(it)) texts.add(it)
            }
            for (i in 0 until n.childCount) traverse(n.getChild(i))
        }
        traverse(root)
        return texts
    }

    private fun broadcastOffer(offer: TripOffer) {
        val now = SystemClock.elapsedRealtime()
        if (offer == lastOffer && now - lastSentAt < REPEAT_INTERVAL_MS) return
        lastOffer = offer
        lastSentAt = now

        val intent = Intent(ACTION_TRIP_OFFER).apply {
            setPackage(packageName) // broadcast restrito ao próprio app
            putExtra(EXTRA_DISTANCE, offer.distanceKm)
            putExtra(EXTRA_DURATION, offer.durationMin)
            putExtra(EXTRA_FARE, offer.fareEstimated)
            putExtra(EXTRA_RATING, offer.passengerRating)
            putExtra(EXTRA_SOURCE, offer.sourceApp.name)
        }
        sendBroadcast(intent)
    }

    override fun onInterrupt() {}
}
