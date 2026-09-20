package com.semaforovalores.util

import com.semaforovalores.model.SourceApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class OfferParserTest {

    /** Textos da oferta real do Uber (print do usuário). */
    private val uberTexts = listOf(
        "UberX",
        "R$ 16,07",
        "R$1,89/km aprox.",
        "4,93 (742)",
        "Verificado",
        "+R$ 2,25 incluído",
        "6 min (3.7 km)",
        "Rua Maria Bento De Lemos, Cidade Intercap, Taboão da Serra",
        "13 minutos (4.8 km)",
        "Rua Izaias Felix dos Santos, 59, Taboão da Serra, Taboão da Serra",
        "Selecionar"
    )

    @Test
    fun ofertaUber_lidaCorretamente() {
        val o = OfferParser.parse(uberTexts, SourceApp.UBER)
        assertNotNull(o)
        assertEquals(16.07, o!!.fareEstimated, 0.001)
        assertEquals(8.5, o.distanceKm, 0.001)   // 3.7 busca + 4.8 viagem
        assertEquals(19, o.durationMin)          // 6 + 13
        assertEquals(4.93, o.passengerRating, 0.001)
        // bate com o "R$1,89/km aprox." do próprio Uber
        assertEquals(1.89, o.fareEstimated / o.distanceKm, 0.01)
    }

    @Test
    fun ignoraValorPorKmEBonus() {
        assertNull(OfferParser.parseFare("R$1,89/km aprox."))
        assertNull(OfferParser.parseFare("+R$ 2,25 incluído"))
        assertEquals(16.07, OfferParser.parseFare("R$ 16,07")!!, 0.001)
        assertEquals(1234.5, OfferParser.parseFare("R$ 1.234,50")!!, 0.001)
    }

    @Test
    fun semValor_retornaNulo() {
        assertNull(OfferParser.parse(listOf("Radar de Viagens", "Selecionar"), SourceApp.UBER))
    }

    @Test
    fun duracaoComHoras() {
        val o = OfferParser.parse(listOf("R$ 80,00", "1 h 5 min (40 km)"), SourceApp.UBER)
        assertEquals(65, o!!.durationMin)
    }
}
