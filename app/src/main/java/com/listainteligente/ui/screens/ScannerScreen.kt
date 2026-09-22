package com.listainteligente.ui.screens

import android.Manifest
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.accompanist.permissions.*
import com.listainteligente.model.PriceOption
import com.listainteligente.model.ScannedLabel
import com.listainteligente.service.CameraService
import com.listainteligente.service.ScanEvent
import com.listainteligente.ui.theme.*

/**
 * Tela de escaneamento de etiqueta.
 * Mostra preview da câmera + resultado OCR + seleção de preço.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ScannerScreen(
    listId: Long,
    currentItemName: String? = null,
    onLookupLowestPrice: suspend (String) -> Double? = { null },
    onItemAdded: (name: String, qty: Int, price: Double, priceLabel: String) -> Unit,
    onBack: () -> Unit
) {
    val context         = LocalContext.current
    val lifecycleOwner  = LocalLifecycleOwner.current
    val cameraService   = remember { CameraService(context.applicationContext) }
    val previewView     = remember { PreviewView(context) }

    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)
    var scannedLabel    by remember { mutableStateOf<ScannedLabel?>(null) }
    var scanHint        by remember { mutableStateOf<String?>(null) }
    var historicalLow   by remember { mutableStateOf<Double?>(null) }

    LaunchedEffect(Unit) {
        if (!cameraPermission.status.isGranted) cameraPermission.launchPermissionRequest()
    }

    // Busca o menor preço já visto pra esse produto assim que uma etiqueta é detectada
    LaunchedEffect(scannedLabel) {
        historicalLow = scannedLabel?.let { onLookupLowestPrice(it.productName) }
    }

    // Só liga a câmera + OCR depois que a permissão for concedida.
    // Sem isso, o PreviewView fica na tela mas nunca recebe frames.
    LaunchedEffect(cameraPermission.status.isGranted) {
        if (cameraPermission.status.isGranted) {
            cameraService.startScanning(previewView, lifecycleOwner)
                .collect { event ->
                    when (event) {
                        is ScanEvent.Detected -> {
                            // Ignora novas detecções enquanto o card de resultado já está
                            // aberto, pra não trocar a etiqueta escaneada debaixo do usuário.
                            if (scannedLabel == null) {
                                scannedLabel = event.label
                                scanHint = null
                            }
                        }
                        is ScanEvent.NoLabel -> scanHint = null
                        is ScanEvent.Error -> {
                            // Na 1ª execução o modelo de OCR pode ainda estar sendo baixado
                            // pelo Play Services — mostra isso em vez de ficar mudo.
                            scanHint = "Preparando leitor de texto… aponte para a etiqueta"
                        }
                    }
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

        // ── Guia de enquadramento (máscara escurecida + linha de scan) ──────
        ScanOverlay(scanHint = scanHint)

        // ── Toolbar com gradiente pra legibilidade sobre a câmera ───────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.65f), Color.Transparent)
                    )
                )
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                }
                Text(
                    "Escanear etiqueta",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
            // Mostra pra qual item da lista o preço vai ser gravado
            if (currentItemName != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White.copy(alpha = 0.15f),
                    modifier = Modifier.padding(start = 44.dp, top = 2.dp)
                ) {
                    Text(
                        "Precificando: $currentItemName",
                        color = Color.White,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
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
                    label           = label,
                    currentItemName = currentItemName,
                    historicalLow   = historicalLow,
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

// ── Moldura de guia de enquadramento (máscara + linha animada) ──────────────

private val ScanFrameWidth  = 300.dp
private val ScanFrameHeight = 180.dp

@Composable
private fun BoxScope.ScanOverlay(scanHint: String?) {
    val scrim = Color.Black.copy(alpha = 0.55f)

    // Escurece tudo ao redor do quadro de leitura, deixando o "recorte" claro
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val sideWidth = ((maxWidth - ScanFrameWidth) / 2).coerceAtLeast(0.dp)
        val topHeight = ((maxHeight - ScanFrameHeight) / 2).coerceAtLeast(0.dp)

        Box(Modifier.align(Alignment.TopCenter).fillMaxWidth().height(topHeight).background(scrim))
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(topHeight).background(scrim))
        Box(Modifier.align(Alignment.CenterStart).width(sideWidth).height(ScanFrameHeight).background(scrim))
        Box(Modifier.align(Alignment.CenterEnd).width(sideWidth).height(ScanFrameHeight).background(scrim))
    }

    Box(
        modifier = Modifier
            .align(Alignment.Center)
            .size(ScanFrameWidth, ScanFrameHeight)
            .border(2.dp, Color.White.copy(alpha = 0.9f), RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
    ) {
        ScanLine()
    }

    // Cantos destacados (por cima da máscara, sem clip, pra ficarem nítidos)
    Box(
        modifier = Modifier.align(Alignment.Center).size(ScanFrameWidth, ScanFrameHeight)
    ) {
        listOf(
            Alignment.TopStart, Alignment.TopEnd,
            Alignment.BottomStart, Alignment.BottomEnd
        ).forEach { Corner(it) }
    }

    Text(
        scanHint ?: "Aponte para a etiqueta de preço",
        color      = Color.White,
        fontSize   = 13.sp,
        textAlign  = TextAlign.Center,
        fontWeight = if (scanHint != null) FontWeight.Medium else FontWeight.Normal,
        modifier = Modifier
            .align(Alignment.Center)
            .offset(y = ScanFrameHeight / 2 + 28.dp)
            .padding(horizontal = 32.dp)
            .fillMaxWidth()
    )
}

// ── Linha de varredura animada dentro do quadro ─────────────────────────────

@Composable
private fun ScanLine() {
    val transition = rememberInfiniteTransition(label = "scanline")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scanlineProgress"
    )

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val y = maxHeight * progress
        Box(
            Modifier
                .offset(y = y)
                .fillMaxWidth()
                .height(2.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, Green500, Color.Transparent)
                    )
                )
        )
    }
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
    currentItemName: String? = null,
    historicalLow: Double? = null,
    onSelect: (PriceOption, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedOption by remember { mutableStateOf(label.priceOptions.minByOrNull { it.price }) }
    var qty by remember { mutableStateOf(selectedOption?.minQty ?: 1) }

    val willRename = currentItemName != null &&
        !currentItemName.equals(label.productName, ignoreCase = true) &&
        !label.productName.equals("Produto", ignoreCase = true)

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
            // Deixa claro que o nome do item vai ser atualizado pro que está na etiqueta
            if (willRename) {
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Edit, null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Renomeando \"$currentItemName\" → \"${label.productName}\"",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            // Comparação com o menor preço já visto pra esse produto
            val currentBestPrice = label.priceOptions.minByOrNull { it.price }?.price
            if (historicalLow != null && currentBestPrice != null && historicalLow > 0) {
                PriceHistoryBadge(currentPrice = currentBestPrice, historicalLow = historicalLow)
                Spacer(Modifier.height(12.dp))
            }

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

// ── Selo de comparação com o histórico de preços ─────────────────────────────

@Composable
fun PriceHistoryBadge(currentPrice: Double, historicalLow: Double) {
    val diffPercent = ((currentPrice - historicalLow) / historicalLow) * 100

    val (bgColor, textColor, icon, message) = when {
        diffPercent <= -1.0 -> {
            // Preço atual é mais barato que qualquer preço já visto antes
            Quadruple(Green100, Green700, Icons.Default.TrendingDown,
                "Menor preço já visto! ${"%.0f".format(-diffPercent)}% mais barato")
        }
        diffPercent >= 1.0 -> {
            Quadruple(Orange100, Orange700, Icons.Default.TrendingUp,
                "${"%.0f".format(diffPercent)}% acima do menor preço já visto")
        }
        else -> {
            Quadruple(Blue100, Blue700, Icons.Default.TrendingFlat,
                "Igual ao menor preço já visto")
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = textColor, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(message, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = textColor)
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

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
