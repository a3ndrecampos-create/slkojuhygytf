package com.listainteligente.ui.screens

import android.Manifest
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.accompanist.permissions.*
import com.listainteligente.model.PriceOption
import com.listainteligente.model.ScannedLabel
import com.listainteligente.service.CameraService
import com.listainteligente.ui.theme.*

/**
 * Tela de escaneamento de etiqueta.
 * Mostra preview da câmera + resultado OCR + seleção de preço.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ScannerScreen(
    listId: Long,
    onItemAdded: (name: String, qty: Int, price: Double, priceLabel: String) -> Unit,
    onBack: () -> Unit
) {
    val context         = LocalContext.current
    val lifecycleOwner  = LocalLifecycleOwner.current
    val cameraService   = remember { CameraService(context.applicationContext) }
    val previewView     = remember { PreviewView(context) }

    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)
    var scannedLabel    by remember { mutableStateOf<ScannedLabel?>(null) }

    LaunchedEffect(Unit) {
        if (!cameraPermission.status.isGranted) cameraPermission.launchPermissionRequest()
    }

    // Só liga a câmera + OCR depois que a permissão for concedida.
    // Sem isso, o PreviewView fica na tela mas nunca recebe frames.
    LaunchedEffect(cameraPermission.status.isGranted) {
        if (cameraPermission.status.isGranted) {
            cameraService.startScanning(previewView, lifecycleOwner)
                .collect { label ->
                    // Ignora novas detecções enquanto o card de resultado já está aberto,
                    // pra não trocar a etiqueta escaneada debaixo do usuário.
                    if (scannedLabel == null) scannedLabel = label
                }
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {

        // ── Preview da câmera ──────────────────────────────────────────────
        if (cameraPermission.status.isGranted) {
            AndroidView(
                factory  = { previewView },
                modifier = Modifier.fillMaxSize()
            )
        }

        // ── Guia de enquadramento ──────────────────────────────────────────
        ScanOverlay()

        // ── Toolbar ───────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, null, tint = Color.White)
            }
            Text(
                "Escanear etiqueta",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        // ── Resultado do scan ──────────────────────────────────────────────
        AnimatedVisibility(
            visible  = scannedLabel != null,
            enter    = slideInVertically { it },
            exit     = slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            scannedLabel?.let { label ->
                ScannedResultCard(
                    label    = label,
                    onSelect = { option, qty ->
                        onItemAdded(label.productName, qty, option.price, option.label)
                        scannedLabel = null
                    },
                    onDismiss = { scannedLabel = null }
                )
            }
        }

        // ── Sem permissão ──────────────────────────────────────────────────
        if (!cameraPermission.status.isGranted) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CameraAlt, null, tint = Color.White, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("Permissão de câmera necessária", color = Color.White)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { cameraPermission.launchPermissionRequest() }) {
                        Text("Permitir câmera")
                    }
                }
            }
        }
    }
}

// ── Moldura de guia de enquadramento ────────────────────────────────────────

@Composable
private fun BoxScope.ScanOverlay() {
    Box(
        modifier = Modifier
            .align(Alignment.Center)
            .size(width = 300.dp, height = 160.dp)
            .border(2.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
    ) {
        // Cantos destacados
        listOf(
            Alignment.TopStart, Alignment.TopEnd,
            Alignment.BottomStart, Alignment.BottomEnd
        ).forEach { Corner(it) }
    }
    Text(
        "Aponte para a etiqueta de preço",
        color    = Color.White,
        fontSize = 13.sp,
        modifier = Modifier
            .align(Alignment.Center)
            .offset(y = 100.dp)
    )
}

@Composable
private fun BoxScope.Corner(alignment: Alignment) {
    Box(
        modifier = Modifier
            .align(alignment)
            .size(20.dp)
            .border(3.dp, Green500, RoundedCornerShape(
                topStart     = if (alignment == Alignment.TopStart) 12.dp else 0.dp,
                topEnd       = if (alignment == Alignment.TopEnd) 12.dp else 0.dp,
                bottomStart  = if (alignment == Alignment.BottomStart) 12.dp else 0.dp,
                bottomEnd    = if (alignment == Alignment.BottomEnd) 12.dp else 0.dp
            ))
    )
}

// ── Card com resultado do escaneamento + seleção de preço ───────────────────

@Composable
fun ScannedResultCard(
    label: ScannedLabel,
    onSelect: (PriceOption, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedOption by remember { mutableStateOf(label.priceOptions.minByOrNull { it.price }) }
    var qty by remember { mutableStateOf(selectedOption?.minQty ?: 1) }

    Surface(
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 16.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp)) {

            // Handle + título
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(Modifier.size(40.dp, 4.dp).clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.outline))
            }
            Spacer(Modifier.height(12.dp))
            Text(label.productName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(16.dp))

            // Opções de preço
            Text("Selecione o preço:", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))

            label.priceOptions.forEach { option ->
                PriceOptionCard(
                    option   = option,
                    selected = option == selectedOption,
                    qty      = qty,
                    onClick  = {
                        selectedOption = option
                        if (qty < option.minQty) qty = option.minQty
                    }
                )
                Spacer(Modifier.height(8.dp))
            }

            // Seletor de quantidade
            Spacer(Modifier.height(8.dp))
            QtySelector(qty = qty, onQtyChange = { qty = it })

            // Total
            Spacer(Modifier.height(12.dp))
            val total = (selectedOption?.price ?: 0.0) * qty
            Row(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Green100)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Total do item", fontWeight = FontWeight.Medium)
                Text(
                    "R$ ${"%.2f".format(total).replace(".", ",")}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Green700
                )
            }

            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text("Cancelar")
                }
                Button(
                    onClick = { selectedOption?.let { onSelect(it, qty) } },
                    modifier = Modifier.weight(1f),
                    enabled = selectedOption != null
                ) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(4.dp))
                    Text("Adicionar")
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

// ── Card de opção de preço ───────────────────────────────────────────────────

@Composable
fun PriceOptionCard(
    option: PriceOption,
    selected: Boolean,
    qty: Int,
    onClick: () -> Unit
) {
    val borderColor = if (selected) Green700 else MaterialTheme.colorScheme.outline
    val bgColor     = if (selected) Green100 else MaterialTheme.colorScheme.surface
    val qtyOk       = qty >= option.minQty

    Surface(
        onClick      = onClick,
        shape        = RoundedCornerShape(12.dp),
        color        = bgColor,
        border       = BorderStroke(if (selected) 2.dp else 1.dp, borderColor),
        modifier     = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(option.label, fontWeight = FontWeight.SemiBold)
                    if (option.requiresApp) {
                        Spacer(Modifier.width(6.dp))
                        Chip("APP", Blue700, Blue100)
                    }
                    if (option.requiresCard) {
                        Spacer(Modifier.width(6.dp))
                        Chip("CARTÃO", Orange700, Orange100)
                    }
                }
                if (option.minQty > 1) {
                    Text(
                        "Mínimo ${option.minQty} unidades",
                        fontSize = 11.sp,
                        color = if (qtyOk) Green700 else Orange700
                    )
                }
                option.maxQty?.let {
                    Text("Limitado a $it unidades", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(
                "R$ ${"%.2f".format(option.price).replace(".", ",")}",
                fontWeight = FontWeight.Bold,
                fontSize   = 22.sp,
                color      = if (selected) Green700 else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// ── Seletor de quantidade ────────────────────────────────────────────────────

@Composable
fun QtySelector(qty: Int, onQtyChange: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Quantidade:", Modifier.weight(1f))
        IconButton(onClick = { if (qty > 1) onQtyChange(qty - 1) }) {
            Icon(Icons.Default.Remove, null)
        }
        Text(
            "$qty",
            fontWeight = FontWeight.Bold,
            fontSize   = 20.sp,
            modifier   = Modifier.padding(horizontal = 16.dp)
        )
        IconButton(onClick = { onQtyChange(qty + 1) }) {
            Icon(Icons.Default.Add, null)
        }
    }
}

// ── Chip de badge (APP / CARTÃO) ─────────────────────────────────────────────

@Composable
fun Chip(text: String, textColor: Color, bgColor: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = textColor)
    }
}
