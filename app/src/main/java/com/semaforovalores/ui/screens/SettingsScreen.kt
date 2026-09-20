package com.semaforovalores.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.semaforovalores.model.UserSettings
import com.semaforovalores.semaforoApp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val repo = remember { context.semaforoApp().settingsRepository }
    val scope = rememberCoroutineScope()
    var draft by remember { mutableStateOf<UserSettings?>(null) }

    LaunchedEffect(Unit) { draft = repo.settings.first() }

    val current = draft
    if (current == null) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        SettingsContent(
            s = current,
            onChange = { new ->
                draft = new
                scope.launch { repo.save(new) }
            },
            modifier = modifier
        )
    }
}

@Composable
private fun SettingsContent(
    s: UserSettings,
    onChange: (UserSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Ajustes",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            "As alterações são salvas automaticamente.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Section("R$ por km") {
            NumberField("Verde a partir de", s.greenMinRatePerKm) { onChange(s.copy(greenMinRatePerKm = it)) }
            NumberField("Amarelo a partir de", s.yellowMinRatePerKm) { onChange(s.copy(yellowMinRatePerKm = it)) }
        }

        Section("R$ por hora") {
            NumberField("Verde a partir de", s.greenMinRatePerHour) { onChange(s.copy(greenMinRatePerHour = it)) }
            NumberField("Amarelo a partir de", s.yellowMinRatePerHour) { onChange(s.copy(yellowMinRatePerHour = it)) }
        }

        Section("Nota do passageiro") {
            NumberField("Verde a partir de", s.greenMinPassengerRating) { onChange(s.copy(greenMinPassengerRating = it)) }
            NumberField("Amarelo a partir de", s.yellowMinPassengerRating) { onChange(s.copy(yellowMinPassengerRating = it)) }
        }

        Section("Lucro após combustível (%)") {
            NumberField("Verde a partir de", s.greenMinProfitPercent) { onChange(s.copy(greenMinProfitPercent = it)) }
            NumberField("Amarelo a partir de", s.yellowMinProfitPercent) { onChange(s.copy(yellowMinProfitPercent = it)) }
        }

        Section("Combustível e veículo") {
            NumberField("Preço do litro (R$)", s.fuelPricePerLiter) { onChange(s.copy(fuelPricePerLiter = it)) }
            NumberField("Consumo (km/l)", s.vehicleConsumptionKmL) { onChange(s.copy(vehicleConsumptionKmL = it)) }
            Text(
                "Custo por km: R$ ${"%.3f".format(s.fuelCostPerKm())}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Section("Valor mínimo") {
            NumberField("Valor mínimo por corrida (R$, 0 = desativado)", s.minTripValue) { onChange(s.copy(minTripValue = it)) }
        }

        Section("Apps monitorados") {
            SwitchRow("Uber", s.monitorUber) { onChange(s.copy(monitorUber = it)) }
            SwitchRow("99", s.monitorNinetyNine) { onChange(s.copy(monitorNinetyNine = it)) }
            SwitchRow("inDrive", s.monitorInDrive) { onChange(s.copy(monitorInDrive = it)) }
        }

        Section("Visual") {
            SwitchRow("Mostrar detalhes de combustível no card", s.showFuelDetails) {
                onChange(s.copy(showFuelDetails = it))
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

@Composable
private fun NumberField(label: String, value: Double, onValue: (Double) -> Unit) {
    var text by remember { mutableStateOf(formatNumber(value)) }
    OutlinedTextField(
        value = text,
        onValueChange = { input ->
            text = input
            input.replace(',', '.').toDoubleOrNull()?.let(onValue)
        },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun formatNumber(v: Double): String =
    if (v % 1.0 == 0.0) v.toInt().toString() else v.toString()
