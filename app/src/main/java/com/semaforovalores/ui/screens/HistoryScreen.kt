package com.semaforovalores.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.semaforovalores.model.SourceApp
import com.semaforovalores.model.TripHistory
import com.semaforovalores.semaforoApp
import com.semaforovalores.ui.theme.composeColor
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val dao = remember { context.semaforoApp().database.tripDao() }
    val flow = remember { dao.observeAll() }
    val trips by flow.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    val accepted = trips.filter { it.accepted }
    val netTotal = accepted.sumOf { it.netProfit }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Histórico",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            if (trips.isNotEmpty()) {
                OutlinedButton(onClick = { scope.launch { dao.clear() } }) { Text("Limpar") }
            }
        }

        if (trips.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Nenhuma corrida registrada ainda.\nToque em Aceitar/Recusar no card do semáforo para registrar.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            Text(
                "${trips.size} corridas · ${accepted.size} aceitas · lucro líquido aceitas: " +
                        "R$ ${"%.2f".format(netTotal)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(trips, key = { it.id }) { trip -> TripRow(trip) }
            }
        }
    }
}

@Composable
private fun TripRow(trip: TripHistory) {
    val time = remember(trip.timestamp) {
        SimpleDateFormat("dd/MM HH:mm", Locale("pt", "BR")).format(Date(trip.timestamp))
    }
    val appName = when (trip.sourceApp) {
        SourceApp.UBER -> "Uber"
        SourceApp.NINETY_NINE -> "99"
        SourceApp.INDRIVE -> "inDrive"
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("●", color = trip.color.composeColor(), style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "R$ ${"%.2f".format(trip.fareEstimated)} · ${"%.1f".format(trip.distanceKm)} km · ${trip.durationMin} min",
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "$appName · $time · líquido R$ ${"%.2f".format(trip.netProfit)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                if (trip.accepted) "Aceita" else "Recusada",
                style = MaterialTheme.typography.labelMedium,
                color = if (trip.accepted) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
