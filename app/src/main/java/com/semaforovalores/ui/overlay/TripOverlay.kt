package com.semaforovalores.ui.overlay

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.semaforovalores.model.TripColor
import com.semaforovalores.model.TripOffer
import com.semaforovalores.model.UserSettings

@Composable
fun TripOverlayCard(
    offer: TripOffer,
    settings: UserSettings,
    color: TripColor,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = when (color) {
        TripColor.GREEN -> Color(0xFF22C55E)
        TripColor.YELLOW -> Color(0xFFF59E0B)
        TripColor.RED -> Color(0xFFEF4444)
    }
    val actionLabel = when (color) {
        TripColor.GREEN -> "● Aceitar"
        TripColor.YELLOW -> "● Avaliar"
        TripColor.RED -> "● Recusar"
    }

    val costPerKm = settings.fuelCostPerKm()
    val fuelCost = offer.fuelCost(costPerKm)
    val netProfit = offer.netProfit(costPerKm)

    AnimatedVisibility(
        visible = true,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut()
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .border(2.dp, borderColor, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Cabeçalho: app + distância + botão de ação
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${offer.durationMin} min · ${"%.1f".format(offer.distanceKm)} km",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = if (color == TripColor.RED) onDecline else onAccept,
                        colors = ButtonDefaults.buttonColors(containerColor = borderColor),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(actionLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Grade de métricas
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    MetricItem("R$/Km", "%.2f".format(offer.ratePerKm()), borderColor)
                    MetricItem("R$/Hora", "%.0f".format(offer.ratePerHour()), borderColor)
                    MetricItem("Lucro%", "%.0f".format(offer.profitPercent(costPerKm)), borderColor)
                    MetricItem("Nota", "%.2f".format(offer.passengerRating), borderColor)
                    MetricItem("Líquido", "R$%.0f".format(netProfit), borderColor)
                }

                // Barra de progresso de lucro
                Spacer(Modifier.height(6.dp))
                val profitRatio = (offer.profitPercent(costPerKm) / 100.0).coerceIn(0.0, 1.0)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(profitRatio.toFloat())
                            .fillMaxHeight()
                            .background(borderColor)
                    )
                }

                // Detalhes de combustível (opcional)
                if (settings.showFuelDetails) {
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(6.dp))
                    FuelDetails(
                        fareEstimated = offer.fareEstimated,
                        fuelCost = fuelCost,
                        netProfit = netProfit,
                        distanceKm = offer.distanceKm,
                        costPerKm = costPerKm
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun FuelDetails(
    fareEstimated: Double,
    fuelCost: Double,
    netProfit: Double,
    distanceKm: Double,
    costPerKm: Double
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        MiniStat("Bruto", "R$%.2f".format(fareEstimated), Color(0xFF22C55E))
        MiniStat("Combustível", "-R$%.2f".format(fuelCost), Color(0xFFEF4444))
        MiniStat("Líquido", "R$%.2f".format(netProfit),
            if (netProfit >= 0) Color(0xFF22C55E) else Color(0xFFEF4444))
        MiniStat("R$/km custo", "%.3f".format(costPerKm), MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MiniStat(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = color)
    }
}
