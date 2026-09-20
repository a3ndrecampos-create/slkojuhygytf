package com.semaforovalores.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.semaforovalores.model.SourceApp
import com.semaforovalores.model.TripOffer

/**
 * Serviço de Acessibilidade que monitora Uber, 99 e inDrive.
 * Extrai dados da corrida da tela e os envia ao OverlayService.
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
    }

    override fun onServiceConnected() {
        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            packageNames = (UBER_PACKAGES + NINETY_NINE_PACKAGES + INDRIVE_PACKAGES).toTypedArray()
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 100
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: return
        val sourceApp = detectSourceApp(packageName) ?: return
        val rootNode = rootInActiveWindow ?: return

        val offer = when (sourceApp) {
            SourceApp.UBER -> extractUberOffer(rootNode)
            SourceApp.NINETY_NINE -> extractNinetyNineOffer(rootNode)
            SourceApp.INDRIVE -> extractInDriveOffer(rootNode)
        }

        offer?.let { broadcastOffer(it) }
    }

    private fun detectSourceApp(pkg: String): SourceApp? = when {
        UBER_PACKAGES.contains(pkg) -> SourceApp.UBER
        NINETY_NINE_PACKAGES.contains(pkg) -> SourceApp.NINETY_NINE
        INDRIVE_PACKAGES.contains(pkg) -> SourceApp.INDRIVE
        else -> null
    }

    /**
     * Extração Uber: busca nós com padrões de texto de oferta de corrida.
     * Adapte os viewIds conforme versão atual do app Uber Driver.
     */
    private fun extractUberOffer(root: AccessibilityNodeInfo): TripOffer? {
        return try {
            // Buscar texto que contenha "km" e "min"
            val allTexts = getAllTexts(root)

            val distanceText = allTexts.firstOrNull { it.contains("km", ignoreCase = true) }
            val durationText = allTexts.firstOrNull { it.contains("min", ignoreCase = true) }
            val fareText = allTexts.firstOrNull { it.startsWith("R$") || it.startsWith("$") }

            val distance = parseDistance(distanceText) ?: return null
            val duration = parseDuration(durationText) ?: return null
            val fare = parseFare(fareText) ?: return null
            val rating = parseRating(allTexts) ?: 5.0

            TripOffer(distance, duration, fare, rating, SourceApp.UBER)
        } catch (e: Exception) {
            null
        }
    }

    private fun extractNinetyNineOffer(root: AccessibilityNodeInfo): TripOffer? {
        // Implementação similar ao Uber para o app 99
        // TODO: ajustar IDs de acordo com o layout atual do app 99
        return extractUberOffer(root)?.copy(sourceApp = SourceApp.NINETY_NINE)
    }

    private fun extractInDriveOffer(root: AccessibilityNodeInfo): TripOffer? {
        // inDrive usa modelo de negociação — extrair o valor proposto pelo passageiro
        // TODO: ajustar IDs de acordo com o layout atual do inDrive
        return extractUberOffer(root)?.copy(sourceApp = SourceApp.INDRIVE)
    }

    private fun getAllTexts(node: AccessibilityNodeInfo): List<String> {
        val texts = mutableListOf<String>()
        fun traverse(n: AccessibilityNodeInfo?) {
            n ?: return
            n.text?.toString()?.trim()?.let { if (it.isNotEmpty()) texts.add(it) }
            for (i in 0 until n.childCount) traverse(n.getChild(i))
        }
        traverse(node)
        return texts
    }

    private fun parseDistance(text: String?): Double? {
        if (text == null) return null
        return Regex("([\\d,\\.]+)\\s*km").find(text)
            ?.groupValues?.get(1)
            ?.replace(",", ".")
            ?.toDoubleOrNull()
    }

    private fun parseDuration(text: String?): Int? {
        if (text == null) return null
        return Regex("(\\d+)\\s*min").find(text)
            ?.groupValues?.get(1)
            ?.toIntOrNull()
    }

    private fun parseFare(text: String?): Double? {
        if (text == null) return null
        return Regex("R\\$\\s*([\\d,\\.]+)").find(text)
            ?.groupValues?.get(1)
            ?.replace(",", ".")
            ?.toDoubleOrNull()
    }

    private fun parseRating(texts: List<String>): Double? {
        return texts.firstOrNull { Regex("^[45][\\.\\,]\\d{1,2}$").matches(it) }
            ?.replace(",", ".")?.toDoubleOrNull()
    }

    private fun broadcastOffer(offer: TripOffer) {
        val intent = Intent(ACTION_TRIP_OFFER).apply {
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
