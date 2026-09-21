package com.listainteligente.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.listainteligente.model.*
import com.listainteligente.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ListDetailScreen(
    listId: Long,
    onScan: () -> Unit,
    onBack: () -> Unit,
    vm: ShoppingViewModel = hiltViewModel()
) {
    LaunchedEffect(listId) { vm.selectList(listId) }
    val state by vm.detailState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(state.list?.name ?: "Lista", fontWeight = FontWeight.Bold)
                        state.list?.store?.takeIf { it.isNotEmpty() }?.let {
                            Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
                },
                actions = {
                    IconButton(onClick = { vm.deleteChecked(listId) }) {
                        Icon(Icons.Default.DeleteSweep, null)
                    }
                }
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // FAB: escanear etiqueta
                FloatingActionButton(
                    onClick = onScan,
                    containerColor = MaterialTheme.colorScheme.secondary
                ) {
                    Icon(Icons.Default.QrCodeScanner, null, tint = Color.White)
                }
                // FAB: adicionar manual
                FloatingActionButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, null)
                }
            }
        }
    ) { padding ->

        Column(Modifier.fillMaxSize().padding(padding)) {

            // ── Totalizador fixo no topo ────────────────────────────────────
            BudgetSummaryBar(summary = state.summary)

            // ── Lista de itens por categoria ────────────────────────────────
            val grouped = state.items.groupBy { it.category }

            if (state.items.isEmpty()) {
                EmptyListHint(onScan = onScan, onAdd = { showAddDialog = true })
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 140.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    grouped.forEach { (category, items) ->
                        stickyHeader {
                            CategoryHeader(category)
                        }
                        items(items, key = { it.id }) { item ->
                            ShoppingItemRow(
                                item      = item,
                                onCheck   = { vm.toggleChecked(item) },
                                onDelete  = { vm.deleteItem(item) },
                                onQtyUp   = { vm.updateItemQty(item, item.quantity + 1) },
                                onQtyDown = { vm.updateItemQty(item, item.quantity - 1) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialog de adição manual
    if (showAddDialog) {
        AddItemDialog(
            listId  = listId,
            onAdd   = { name, qty, price, label ->
                vm.addItem(listId, name, qty, price, label)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }
}

// ── Barra de orçamento/totalizador ──────────────────────────────────────────

@Composable
fun BudgetSummaryBar(summary: ListSummary) {
    val bgColor = when {
        summary.isOverBudget                  -> Red100
        summary.progressPercent > 0.85f       -> Orange100
        else                                  -> Green100
    }
    val textColor = when {
        summary.isOverBudget            -> Red700
        summary.progressPercent > 0.85f -> Orange700
        else                            -> Green700
    }

    Surface(color = bgColor, shadowElevation = 4.dp) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Total da lista",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "R$ ${"%.2f".format(summary.subtotal).replace(".", ",")}",
                        fontWeight = FontWeight.Bold,
                        fontSize   = 28.sp,
                        color      = textColor
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "${summary.checkedItems}/${summary.totalItems} itens",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (summary.budget > 0) {
                        Text(
                            if (summary.isOverBudget)
                                "⚠ Acima do orçamento"
                            else
                                "Resta R$ ${"%.2f".format(summary.remaining).replace(".", ",")}",
                            fontSize   = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = textColor
                        )
                    }
                }
            }

            if (summary.budget > 0) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { summary.progressPercent },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color    = textColor,
                    trackColor = textColor.copy(alpha = 0.2f)
                )
            }
        }
    }
}

// ── Cabeçalho de categoria ───────────────────────────────────────────────────

@Composable
fun CategoryHeader(category: String) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Text(
            category,
            fontWeight = FontWeight.SemiBold,
            fontSize   = 13.sp,
            color      = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier   = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
        )
    }
}

// ── Linha de item da lista ───────────────────────────────────────────────────

@Composable
fun ShoppingItemRow(
    item: ShoppingItem,
    onCheck: () -> Unit,
    onDelete: () -> Unit,
    onQtyUp: () -> Unit,
    onQtyDown: () -> Unit
) {
    val textDecoration = if (item.checked) TextDecoration.LineThrough else TextDecoration.None
    val textColor = if (item.checked) MaterialTheme.colorScheme.onSurfaceVariant
                   else MaterialTheme.colorScheme.onSurface

    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        shape    = RoundedCornerShape(12.dp),
        color    = if (item.checked) MaterialTheme.colorScheme.surfaceVariant
                   else MaterialTheme.colorScheme.surface,
        shadowElevation = if (item.checked) 0.dp else 2.dp
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = item.checked, onCheckedChange = { onCheck() })

            Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                Text(item.name, fontWeight = FontWeight.Medium,
                    textDecoration = textDecoration, color = textColor)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        item.selectedPriceLabel,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        " · R$ ${"%.2f".format(item.selectedPrice).replace(".", ",")} cada",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Controle de quantidade
            Row(verticalAlignment = Alignment.CenterVertically) {
                SmallQtyButton(Icons.Default.Remove, onClick = onQtyDown)
                Text(
                    "${item.quantity}",
                    fontWeight = FontWeight.Bold,
                    modifier   = Modifier.padding(horizontal = 8.dp),
                    minLines   = 1
                )
                SmallQtyButton(Icons.Default.Add, onClick = onQtyUp)
            }

            // Total do item
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(start = 8.dp)) {
                Text(
                    "R$ ${"%.2f".format(item.totalPrice).replace(".", ",")}",
                    fontWeight = FontWeight.Bold,
                    color      = Green700
                )
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Close, null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun SmallQtyButton(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, modifier = Modifier.size(16.dp))
    }
}

// ── Dica quando lista vazia ──────────────────────────────────────────────────

@Composable
fun EmptyListHint(onScan: () -> Unit, onAdd: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.ShoppingCart, null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(16.dp))
        Text("Lista vazia", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Spacer(Modifier.height(8.dp))
        Text("Escaneie uma etiqueta ou adicione itens manualmente",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onScan, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.QrCodeScanner, null)
            Spacer(Modifier.width(8.dp))
            Text("Escanear etiqueta")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onAdd, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Edit, null)
            Spacer(Modifier.width(8.dp))
            Text("Adicionar manualmente")
        }
    }
}

// ── Dialog de adição manual ──────────────────────────────────────────────────

@Composable
fun AddItemDialog(
    listId: Long,
    onAdd: (name: String, qty: Int, price: Double, label: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name  by remember { mutableStateOf("") }
    var qty   by remember { mutableStateOf("1") }
    var price by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adicionar item") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Nome do produto") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = qty, onValueChange = { qty = it },
                        label = { Text("Qtd") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = price, onValueChange = { price = it },
                        label = { Text("Preço R$") },
                        modifier = Modifier.weight(2f)
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val p = price.replace(",", ".").toDoubleOrNull() ?: 0.0
                val q = qty.toIntOrNull() ?: 1
                if (name.isNotBlank()) onAdd(name, q, p, "Manual")
            }) { Text("Adicionar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
