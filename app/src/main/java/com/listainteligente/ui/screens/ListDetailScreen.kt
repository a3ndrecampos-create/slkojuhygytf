package com.listainteligente.ui.screens

import android.content.Intent
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
import androidx.compose.ui.platform.LocalContext
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
    onScanItem: (itemId: Long) -> Unit,
    onBack: () -> Unit,
    vm: ShoppingViewModel = hiltViewModel()
) {
    LaunchedEffect(listId) { vm.selectList(listId) }
    val state by vm.detailState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showCompletionDialog by remember { mutableStateOf(false) }
    var purchaseSavings by remember { mutableStateOf<Double?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var itemFilter by remember { mutableStateOf(ItemFilter.TODOS) }
    val context = LocalContext.current

    val isComplete = state.summary.totalItems > 0 && state.summary.checkedItems == state.summary.totalItems

    LaunchedEffect(showCompletionDialog) {
        if (showCompletionDialog) {
            purchaseSavings = state.list?.let { vm.computePurchaseSavings(state.items, it.createdAt) }
        }
    }

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
                    if (isComplete) {
                        IconButton(onClick = { showCompletionDialog = true }) {
                            Icon(Icons.Default.TaskAlt, null, tint = Green700)
                        }
                    }
                    IconButton(onClick = {
                        val text = buildShareText(state.list, state.items)
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, text)
                        }
                        context.startActivity(Intent.createChooser(intent, "Compartilhar lista"))
                    }) {
                        Icon(Icons.Default.Share, null)
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, null)
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Repetir lista") },
                                leadingIcon = { Icon(Icons.Default.Replay, null) },
                                onClick = {
                                    state.list?.let { vm.repeatList(it) }
                                    showMenu = false
                                    android.widget.Toast.makeText(
                                        context, "Lista repetida! Veja na tela inicial.",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Limpar comprados") },
                                leadingIcon = { Icon(Icons.Default.DeleteSweep, null) },
                                onClick = {
                                    vm.deleteChecked(listId)
                                    showMenu = false
                                }
                            )
                        }
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
            if (state.items.isEmpty()) {
                EmptyListHint(
                    listType = state.list?.listType ?: ListTypes.MERCADO,
                    onScan = onScan,
                    onAdd = { showAddDialog = true }
                )
            } else {
                SearchAndFilterBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    filter = itemFilter,
                    onFilterChange = { itemFilter = it }
                )

                val filteredItems = state.items.filter { item ->
                    val matchesQuery = searchQuery.isBlank() ||
                        item.name.contains(searchQuery, ignoreCase = true)
                    val matchesFilter = when (itemFilter) {
                        ItemFilter.TODOS      -> true
                        ItemFilter.PENDENTES  -> !item.checked
                        ItemFilter.COMPRADOS  -> item.checked
                    }
                    matchesQuery && matchesFilter
                }
                val grouped = filteredItems.groupBy { it.category }

                if (filteredItems.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "Nenhum item encontrado",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
                                    item        = item,
                                    onScanPrice = { onScanItem(item.id) },
                                    onUncheck   = { vm.toggleChecked(item) },
                                    onDelete    = { vm.deleteItem(item) },
                                    onQtyUp     = { vm.updateItemQty(item, item.quantity + 1) },
                                    onQtyDown   = { vm.updateItemQty(item, item.quantity - 1) }
                                )
                            }
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
            onAdd   = { name, qty, price, label, unit, category ->
                vm.addItem(listId, name, qty, price, label, unit, category)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }

    // Resumo ao finalizar a compra
    if (showCompletionDialog) {
        PurchaseCompletionDialog(
            summary  = state.summary,
            savings  = purchaseSavings,
            onDismiss = { showCompletionDialog = false }
        )
    }
}

enum class ItemFilter { TODOS, PENDENTES, COMPRADOS }

@Composable
private fun SearchAndFilterBar(
    query: String,
    onQueryChange: (String) -> Unit,
    filter: ItemFilter,
    onFilterChange: (ItemFilter) -> Unit
) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Procurar produto") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Close, null)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = filter == ItemFilter.TODOS, onClick = { onFilterChange(ItemFilter.TODOS) },
                label = { Text("Todos") })
            FilterChip(selected = filter == ItemFilter.PENDENTES, onClick = { onFilterChange(ItemFilter.PENDENTES) },
                label = { Text("Pendentes") })
            FilterChip(selected = filter == ItemFilter.COMPRADOS, onClick = { onFilterChange(ItemFilter.COMPRADOS) },
                label = { Text("Comprados") })
        }
    }
}

@Composable
fun PurchaseCompletionDialog(summary: ListSummary, savings: Double?, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon  = { Icon(Icons.Default.TaskAlt, null, tint = Green700) },
        title = { Text("Compra finalizada!") },
        text = {
            Column {
                Text("${summary.totalItems} produtos · ${summary.checkedItems} encontrados",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                Spacer(Modifier.height(12.dp))
                Text(
                    "R$ ${"%.2f".format(summary.subtotal).replace(".", ",")}",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(Modifier.height(10.dp))
                when {
                    savings == null -> Text(
                        "Sem histórico suficiente pra calcular economia ainda.",
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    savings > 0.009 -> Text(
                        "Você economizou R$ ${"%.2f".format(savings).replace(".", ",")} comparado ao menor preço visto antes desta lista.",
                        color = Green700, fontWeight = FontWeight.SemiBold, fontSize = 13.sp
                    )
                    savings < -0.009 -> Text(
                        "Pagou R$ ${"%.2f".format(-savings).replace(".", ",")} a mais do que o menor preço já visto antes.",
                        color = Orange700, fontSize = 13.sp
                    )
                    else -> Text(
                        "Pagou exatamente o menor preço já visto antes.",
                        fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("Fechar") } }
    )
}

// ── Texto formatado pra compartilhar a lista (WhatsApp, etc.) ───────────────

private fun buildShareText(list: ShoppingList?, items: List<ShoppingItem>): String {
    val sb = StringBuilder()
    sb.appendLine("🛒 ${list?.name ?: "Lista de compras"}")
    if (!list?.store.isNullOrEmpty()) sb.appendLine("📍 ${list?.store}")
    sb.appendLine()

    items.groupBy { it.category }.forEach { (category, categoryItems) ->
        sb.appendLine("• $category")
        categoryItems.forEach { item ->
            val mark = if (item.checked) "✅" else "⬜"
            val priceInfo = if (item.selectedPrice > 0)
                " — R$ ${"%.2f".format(item.totalPrice).replace(".", ",")}" else ""
            sb.appendLine("  $mark ${item.name} (${item.quantity}x)$priceInfo")
        }
        sb.appendLine()
    }

    val total = items.sumOf { it.totalPrice }
    sb.appendLine("Total: R$ ${"%.2f".format(total).replace(".", ",")}")
    sb.appendLine()
    sb.append("Feito com Lista Inteligente 📱")
    return sb.toString()
}

// ── Barra de orçamento/totalizador ──────────────────────────────────────────

@Composable
fun BudgetSummaryBar(summary: ListSummary) {
    val accentColor = when {
        summary.isOverBudget                  -> Red700
        summary.progressPercent > 0.85f       -> Orange700
        else                                  -> Green700
    }
    val bgColor = when {
        summary.isOverBudget                  -> Red100
        summary.progressPercent > 0.85f       -> Orange100
        else                                  -> Green100
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 3.dp
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (summary.isOverBudget) Icons.Default.PriorityHigh else Icons.Default.Payments,
                    null, tint = accentColor
                )
            }

            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                Text(
                    "Total da lista",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "R$ ${"%.2f".format(summary.subtotal).replace(".", ",")}",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (summary.budget > 0) {
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress   = { summary.progressPercent },
                        modifier   = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
                        color      = accentColor,
                        trackColor = accentColor.copy(alpha = 0.15f)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(start = 8.dp)) {
                Text(
                    "${summary.checkedItems}/${summary.totalItems}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    "itens",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (summary.budget > 0) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (summary.isOverBudget)
                            "Acima do orçamento"
                        else
                            "Resta R$ ${"%.2f".format(summary.remaining).replace(".", ",")}",
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = accentColor
                    )
                }
            }
        }
    }
}

// ── Cabeçalho de categoria ───────────────────────────────────────────────────

@Composable
fun CategoryHeader(category: String) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(width = 3.dp, height = 14.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Green700)
            )
            Text(
                category.uppercase(),
                fontWeight = FontWeight.SemiBold,
                fontSize   = 12.sp,
                letterSpacing = 0.5.sp,
                color      = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier   = Modifier.padding(start = 8.dp)
            )
        }
    }
}

// ── Linha de item da lista ───────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingItemRow(
    item: ShoppingItem,
    onScanPrice: () -> Unit,
    onUncheck: () -> Unit,
    onDelete: () -> Unit,
    onQtyUp: () -> Unit,
    onQtyDown: () -> Unit
) {
    val textDecoration = if (item.checked) TextDecoration.LineThrough else TextDecoration.None
    val textColor = if (item.checked) MaterialTheme.colorScheme.onSurfaceVariant
                   else MaterialTheme.colorScheme.onSurface

    // Item ainda não encontrado: tocar no checkbox/linha abre a câmera para ler o preço.
    // Item já marcado: tocar de novo só desmarca (não precisa escanear outra vez).
    val onRowTap = { if (item.checked) onUncheck() else onScanPrice() }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else false
        }
    )

    Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 3.dp)) {
        SwipeToDismissBox(
            state = dismissState,
            enableDismissFromStartToEnd = false,
            backgroundContent = {
                Box(
                    Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Icon(Icons.Default.DeleteOutline, null, tint = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onRowTap),
                shape    = RoundedCornerShape(12.dp),
                color    = if (item.checked) MaterialTheme.colorScheme.surfaceVariant
                           else MaterialTheme.colorScheme.surface,
                shadowElevation = if (item.checked) 0.dp else 1.dp
            ) {
                Row(
                    Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Indicador de status: check verde quando encontrado, aro vazio quando pendente
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (item.checked) Green700 else Color.Transparent)
                            .border(
                                width = if (item.checked) 0.dp else 1.5.dp,
                                color = if (item.checked) Color.Transparent else MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable(onClick = onRowTap),
                        contentAlignment = Alignment.Center
                    ) {
                        if (item.checked) {
                            Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }

                    Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                        Text(item.name, fontWeight = FontWeight.Medium,
                            textDecoration = textDecoration, color = textColor)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "${item.quantity} ${item.unit} · ${item.selectedPriceLabel}",
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
                    Text(
                        "R$ ${"%.2f".format(item.totalPrice).replace(".", ",")}",
                        fontWeight = FontWeight.Bold,
                        color      = Green700,
                        modifier   = Modifier.padding(start = 10.dp)
                    )
                }
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
fun EmptyListHint(listType: String = ListTypes.MERCADO, onScan: () -> Unit, onAdd: () -> Unit) {
    val subtitle = when (listType) {
        ListTypes.FARMACIA -> "Escaneie a caixa do medicamento ou adicione manualmente"
        ListTypes.CASA     -> "Escaneie um produto ou adicione o que falta em casa"
        else               -> "Escaneie uma etiqueta ou adicione itens manualmente"
    }
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(ListTypes.emoji(listType), fontSize = 56.sp)
        Spacer(Modifier.height(16.dp))
        Text("Lista vazia", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Spacer(Modifier.height(8.dp))
        Text(subtitle,
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

private val itemUnits = listOf("un", "cx", "pct", "kg", "g", "L", "ml")

@Composable
fun AddItemDialog(
    listId: Long,
    onAdd: (name: String, qty: Int, price: Double, label: String, unit: String, category: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name     by remember { mutableStateOf("") }
    var qty      by remember { mutableStateOf("1") }
    var price    by remember { mutableStateOf("") }
    var unit     by remember { mutableStateOf("un") }
    var category by remember { mutableStateOf("Outros") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adicionar item") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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

                Text("Unidade", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(itemUnits) { u ->
                        FilterChip(selected = unit == u, onClick = { unit = u }, label = { Text(u) })
                    }
                }

                Text("Categoria", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(Categories.all) { c ->
                        FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c) })
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val p = price.replace(",", ".").toDoubleOrNull() ?: 0.0
                val q = qty.toIntOrNull() ?: 1
                if (name.isNotBlank()) onAdd(name, q, p, "Manual", unit, category)
            }) { Text("Adicionar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
