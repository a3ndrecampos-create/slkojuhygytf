package com.semaforovalores.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.semaforovalores.service.OverlayService
import com.semaforovalores.service.RideAccessibilityService
import com.semaforovalores.ui.theme.SemaforoGreen
import com.semaforovalores.ui.theme.SemaforoRed
import com.semaforovalores.ui.theme.SemaforoYellow

@Composable
fun HomeScreen(refreshTick: Int, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val running by OverlayService.running.collectAsState()

    val overlayOk = remember(refreshTick) { Settings.canDrawOverlays(context) }
    val accessOk = remember(refreshTick) { isAccessibilityEnabled(context) }
    var notifOk by remember(refreshTick) { mutableStateOf(hasNotificationPermission(context)) }
    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> notifOk = granted }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "🚦 Semáforo de Valores",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Mostra sobre o app de corrida se a oferta compensa: verde, amarelo ou vermelho.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // ---- Permissões
        SectionCard("Permissões") {
            PermissionRow(
                title = "Exibir sobre outros apps",
                description = "Necessário para mostrar o card do semáforo.",
                granted = overlayOk,
                onClick = {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                    )
                }
            )
            PermissionRow(
                title = "Serviço de acessibilidade",
                description = "Lê os dados da oferta na tela do app de corrida.",
                granted = accessOk,
                onClick = {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
            )
            if (Build.VERSION.SDK_INT >= 33) {
                PermissionRow(
                    title = "Notificações",
                    description = "Mostra o aviso de que o monitoramento está ativo.",
                    granted = notifOk,
                    onClick = { notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
                )
            }
        }

        // ---- Controle
        SectionCard("Monitoramento") {
            Text(
                if (running) "🟢 Ativo — aguardando ofertas de corrida"
                else "⚪ Desligado",
                style = MaterialTheme.typography.bodyMedium
            )
            Button(
                onClick = {
                    val intent = Intent(context, OverlayService::class.java)
                    if (running) context.stopService(intent)
                    else ContextCompat.startForegroundService(context, intent)
                },
                enabled = overlayOk || running,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (running) SemaforoRed else SemaforoGreen,
                    contentColor = androidx.compose.ui.graphics.Color.White
                )
            ) {
                Text(if (running) "Parar monitoramento" else "Iniciar monitoramento")
            }
            if (!overlayOk) {
                Text(
                    "Conceda a permissão de sobreposição para iniciar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedButton(
                onClick = {
                    if (!running) {
                        Toast.makeText(context, "Inicie o monitoramento primeiro.", Toast.LENGTH_SHORT).show()
                    } else {
                        sendTestOffer(context)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Testar overlay com corrida de exemplo")
            }
        }

        // ---- Diagnóstico
        val eventCount by RideAccessibilityService.eventCount.collectAsState()
        val lastPackage by RideAccessibilityService.lastPackage.collectAsState()
        val lastTexts by RideAccessibilityService.lastTexts.collectAsState()
        val lastParsed by RideAccessibilityService.lastParsed.collectAsState()
        SectionCard("Diagnóstico da leitura") {
            Text(
                "Eventos recebidos dos apps de corrida: $eventCount" +
                        if (lastPackage.isNotEmpty()) " ($lastPackage)" else "",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                if (lastParsed.isNotEmpty()) "✅ Última oferta lida: $lastParsed"
                else "⚠️ Nenhuma oferta interpretada ainda",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                if (lastTexts.isEmpty()) "Nenhum texto lido. Abra o app de corrida com uma oferta na tela e volte aqui."
                else "Textos lidos da tela:\n" + lastTexts.joinToString("\n") { "• $it" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // ---- Legenda
        SectionCard("Como ler o semáforo") {
            LegendRow(SemaforoGreen, "Verde", "Compensa — todas as métricas acima do limite verde.")
            LegendRow(SemaforoYellow, "Amarelo", "Avaliar — alguma métrica ficou abaixo do verde.")
            LegendRow(SemaforoRed, "Vermelho", "Não compensa — alguma métrica abaixo do mínimo.")
            Text(
                "A cor final é sempre a pior entre R$/km, R$/hora, nota, lucro % e valor mínimo. Ajuste os limites na aba Ajustes.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

@Composable
private fun PermissionRow(
    title: String,
    description: String,
    granted: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(if (granted) "✅" else "⚠️", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (!granted) {
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = onClick) { Text("Abrir") }
        }
    }
}

@Composable
private fun LegendRow(color: androidx.compose.ui.graphics.Color, name: String, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("●", color = color, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.width(8.dp))
        Text(
            "$name — $text",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

private fun isAccessibilityEnabled(context: Context): Boolean {
    val enabled = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false
    return enabled.contains(context.packageName) &&
            enabled.contains(RideAccessibilityService::class.java.simpleName)
}

private fun hasNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

/** Simula uma oferta (amarela com as configurações padrão) para testar o overlay. */
private fun sendTestOffer(context: Context) {
    val intent = Intent(RideAccessibilityService.ACTION_TRIP_OFFER).apply {
        setPackage(context.packageName)
        putExtra(RideAccessibilityService.EXTRA_DISTANCE, 6.2)
        putExtra(RideAccessibilityService.EXTRA_DURATION, 18)
        putExtra(RideAccessibilityService.EXTRA_FARE, 14.50)
        putExtra(RideAccessibilityService.EXTRA_RATING, 4.85)
        putExtra(RideAccessibilityService.EXTRA_SOURCE, "UBER")
    }
    context.sendBroadcast(intent)
}
