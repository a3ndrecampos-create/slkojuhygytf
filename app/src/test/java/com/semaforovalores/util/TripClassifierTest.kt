package com.semaforovalores.util

import com.semaforovalores.model.SourceApp
import com.semaforovalores.model.TripColor
import com.semaforovalores.model.TripOffer
import com.semaforovalores.model.UserSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class TripClassifierTest {

    private val settings = UserSettings()

    private fun offer(km: Double, min: Int, fare: Double, rating: Double = 4.95) =
        TripOffer(km, min, fare, rating, SourceApp.UBER)

    @Test
    fun boaCorrida_ficaVerde() {
        // R$3,00/km, R$60/h, lucro ~82%
        assertEquals(TripColor.GREEN, TripClassifier.classify(offer(5.0, 15, 15.0), settings))
    }

    @Test
    fun corridaFraca_ficaVermelha() {
        // R$0,80/km
        assertEquals(TripColor.RED, TripClassifier.classify(offer(10.0, 30, 8.0), settings))
    }

    @Test
    fun corridaIntermediaria_ficaAmarela() {
        // R$1,50/km (amarelo), R$30/h (amarelo), demais verdes
        assertEquals(TripColor.YELLOW, TripClassifier.classify(offer(10.0, 30, 15.0), settings))
    }

    @Test
    fun notaBaixa_puxaParaVermelho() {
        assertEquals(TripColor.RED, TripClassifier.classify(offer(5.0, 15, 15.0, rating = 4.0), settings))
    }

    @Test
    fun valorMinimo_derrubaParaVermelho() {
        val s = settings.copy(minTripValue = 20.0)
        assertEquals(TripColor.RED, TripClassifier.classify(offer(5.0, 15, 15.0), s))
    }

    @Test
    fun calculosDaOferta() {
        val o = offer(10.0, 30, 20.0)
        assertEquals(2.0, o.ratePerKm(), 0.0001)
        assertEquals(40.0, o.ratePerHour(), 0.0001)
        assertEquals(5.0, o.fuelCost(0.5), 0.0001)
        assertEquals(15.0, o.netProfit(0.5), 0.0001)
        assertEquals(75.0, o.profitPercent(0.5), 0.0001)
    }
}
