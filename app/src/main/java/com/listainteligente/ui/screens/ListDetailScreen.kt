package com.listainteligente.ui.screens

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.listainteligente.model.*
import com.listainteligente.ui.asInput
import com.listainteligente.ui.brl
import com.listainteligente.ui.parseMoney
import com.listainteligente.ui.theme.*

enum class ItemFilter { TODOS, PENDENTES, COMPRADOS }

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
    val context = LocalContext.current

    var showAddDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showBudgetDialog by remember { mutableStateOf(false) }
    var showCompletionDialog by remember { mutableStateOf(false) }
    var purchaseSavings by remember { mutableStateOf<Double?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var itemFilter by remember { mutableStateOf(ItemFilter.TODOS) }

    // Itens em ação: exclusão (sempre com confirmação), escolha do jeito de informar o preço, preço manual
    var itemToDelete by remember { mutableStateOf<ShoppingItem?>(null) }
    var itemForPriceMethod by remember { mutableStateOf<ShoppingItem?>(null) }
    var itemForManualPrice by remember { mutableStateOf<ShoppingItem?>(null) }

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
                        Text(state.list?.name ?: "Lista", fontWeight = FontWeight.Bold,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        state.list?.store?.takeIf { it.isNotEmpty() }?.let {
                            Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Voltar") }
                },
                actions = {
                    IconButton(onClick = {
                        val text = buildShareText(state.list, state.items)
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, text)
                        }
                        context.startActivity(Intent.createChooser(intent, "Compartilhar lista"))
                    }) {
                        Icon(Icons.Default.Share, "Compartilhar")
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, "Mais opções")
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text(if ((state.list?.budget ?: 0.0) > 0) "Alterar orçamento" else "Definir orçamento") },
                                leadingIcon = { Icon(Icons.Default.Savings, null) },
                                onClick = {
                                    showMenu = false
                                    showBudgetDialog = true
                                }
                            )
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
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SmallFloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(Icons.Default.Add, "Adicionar item")
                }
                ExtendedFloatingActionButton(
                    onClick = onScan,
                    icon = { Icon(Icons.Default.QrCodeScanner, null) },
                    text = { Text("Escanear etiqueta") },
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                )
            }
        }
    ) { padding ->

        if (state.items.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(padding)) {
                SummaryHero(
                    summary = state.summary,
                    isComplete = false,
                    onEditBudget = { showBudgetDialog = true },
                    onFinish = {}
                )
                EmptyListHint(
                    listType = state.list?.listType ?: ListTypes.MERCADO,
                    onScan = onScan,
                    onAdd = { showAddDialog = true }
                )
            }
        } else {
            val filteredItems = state.items.filter { item ->
                val matchesQuery = searchQuery.isBlank() ||
                    item.name.contains(searchQuery, ignoreCase = true)
                val matchesFilter = when (itemFilter) {
                    ItemFilter.TODOS     -> true
                    ItemFilter.PENDENTES -> !item.checked
                    ItemFilter.COMPRADOS -> item.checked
                }
                matchesQuery && matchesFilter
            }
            // Dentro de cada categoria, o que ainda falta comprar vem primeiro
            val grouped = filteredItems
                .groupBy { it.category }
                .mapValues { (_, list) -> list.sortedBy { it.checked } }

            LazyColumn(
                contentPadding = PaddingValues(bottom = 180.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
                item(key = "hero") {
                    SummaryHero(
                        summary = state.summary,
                        isComplete = isComplete,
                        onEditBudget = { showBudgetDialog = true },
                        onFinish = { showCompletionDialog = true }
                    )
                }
                item(key = "filters") {
                    SearchAndFilterBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        filter = itemFilter,
                        onFilterChange = { itemFilter = it },
                        pendingCount = state.summary.totalItems - state.summary.checkedItems,
                        boughtCount = state.summary.checkedItems,
                        totalCount = state.summary.totalItems
                    )
                }

                if (filteredItems.isEmpty()) {
                    item(key = "empty") {
                        Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                            Text("Nenhum item encontrado", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    grouped.forEach { (category, categoryItems) ->
                        stickyHeader(key = "cat-$category") {
                            CategoryHeader(category)
                        }
                        items(categoryItems, key = { it.id }) { item ->
                            ShoppingItemRow(
                                item        = item,
                                modifier    = Modifier.animateItemPlacement(),
                                // Marcar: abre a escolha entre câmera e preço manual.
                                // Desmarcar: só desmarca.
                                onCheckClick = {
                                    if (item.checked) vm.toggleChecked(item)
                                    else itemForPriceMethod = item
                                },
                                onRequestDelete = { itemToDelete = item },
                                onQtyUp     = { vm.updateItemQty(item, item.quantity + 1) },
                                onQtyDown   = {
                                    // Em 1 unidade, "−" pergunta se quer excluir (nunca apaga sozinho)
                                    if (item.quantity <= 1) itemToDelete = item
                                    else vm.updateItemQty(item, item.quantity - 1)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // ── Diálogos e sheets ────────────────────────────────────────────────────

    itemToDelete?.let { item ->
        ConfirmDeleteDialog(
            title = "Excluir item?",
            message = "\"${item.name}\" será removido desta lista.",
            onConfirm = {
                vm.deleteItem(item)
                itemToDelete = null
            },
            onDismiss = { itemToDelete = null }
        )
    }

    itemForPriceMethod?.let { item ->
        PriceMethodSheet(
            item = item,
            onCamera = {
                itemForPriceMethod = null
                onScanItem(item.id)
            },
            onManual = {
                itemForPriceMethod = null
                itemForManualPrice = item
            },
            onDismiss = { itemForPriceMethod = null }
        )
    }

    itemForManualPrice?.let { item ->
        ManualPriceDialog(
            item = item,
            onConfirm = { qty, price ->
                vm.setManualPriceAndCheck(item, qty, price)
                itemForManualPrice = null
            },
            onDismiss = { itemForManualPrice = null }
        )
    }

    if (showBudgetDialog) {
        BudgetDialog(
            current = state.list?.budget ?: 0.0,
            onSave = { value ->
                state.list?.let { vm.updateListBudget(it, value) }
                showBudgetDialog = false
            },
            onDismiss = { showBudgetDialog = false }
        )
    }

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

    if (showCompletionDialog) {
        PurchaseCompletionDialog(
            summary  = state.summary,
            savings  = purchaseSavings,
            onDismiss = { showCompletionDialog = false }
        )
    }
}

// ── Resumo no topo (total, orçamento, progresso) ─────────────────────────────

@Composable
fun SummaryHero(
    summary: ListSummary,
    isComplete: Boolean,
    onEditBudget: () -> Unit,
    onFinish: () -> Unit
) {
    val over = summary.isOverBudget
    val nearLimit = summary.progressPercent > 0.85f
    val gradient = when {
        over      -> listOf(Red700, Orange700)
        nearLimit -> listOf(Orange700, Orange500)
        else      -> listOf(Green900, Green500)
    }

    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(gradient))
            .padding(20.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ShoppingCart, null, tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Total da compra", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
                Spacer(Modifier.weight(1f))
                Text(
                    "${summary.checkedItems} de ${summary.totalItems} no carrinho",
                    color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                summary.subtotal.brl(),
                color = Color.White,
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(Modifier.height(12.dp))

            if (summary.budget > 0) {
                LinearProgressIndicator(
                    progress   = { summary.progressPercent },
                    modifier   = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color      = Color.White,
                    trackColor = Color.White.copy(alpha = 0.28f)
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth().clickable(onClick = onEditBudget),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (over) "${(summary.subtotal - summary.budget).brl()} acima do orçamento"
                        else "Ainda pode gastar ${summary.remaining.brl()}",
                        color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Text("de ${summary.budget.brl()}", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Default.Edit, "Alterar orçamento", tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(14.dp))
                }
            } else {
                // Sem orçamento ainda: convite claro pra definir agora (não precisa ser na criação)
                Surface(
                    onClick = onEditBudget,
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.20f)
                ) {
                    Row(
                        Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Savings, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Definir orçamento", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }
            }

            if (isComplete) {
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onFinish,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Green900)
                ) {
                    Icon(Icons.Default.TaskAlt, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Ver resumo da compra")
                }
            }
        }
    }
}

// ── Busca e filtros (com contagem) ───────────────────────────────────────────

@Composable
private fun SearchAndFilterBar(
    query: String,
    onQueryChange: (String) -> Unit,
    filter: ItemFilter,
    onFilterChange: (ItemFilter) -> Unit,
    pendingCount: Int,
    boughtCount: Int,
    totalCount: Int
) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Procurar produto") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Close, "Limpar busca")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = filter == ItemFilter.TODOS, onClick = { onFilterChange(ItemFilter.TODOS) },
                label = { Text("Todos ($totalCount)") })
            FilterChip(selected = filter == ItemFilter.PENDENTES, onClick = { onFilterChange(ItemFilter.PENDENTES) },
                label = { Text("Falta ($pendingCount)") })
            FilterChip(selected = filter == ItemFilter.COMPRADOS, onClick = { onFilterChange(ItemFilter.COMPRADOS) },
                label = { Text("No carrinho ($boughtCount)") })
        }
    }
}

// ── Cabeçalho de categoria ───────────────────────────────────────────────────

@Composable
fun CategoryHeader(category: String) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(width = 4.dp, height = 16.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Green700)
            )
            Text(
                category.uppercase(),
                fontWeight = FontWeight.Bold,
                fontSize   = 12.sp,
                letterSpacing = 0.8.sp,
                color      = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier   = Modifier.padding(start = 8.dp)
            )
        }
    }
}

// ── Cartão de item ───────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingItemRow(
    item: ShoppingItem,
    onCheckClick: () -> Unit,
    onRequestDelete: () -> Unit,
    onQtyUp: () -> Unit,
    onQtyDown: () -> Unit,
    modifier: Modifier = Modifier
) {
    val textDecoration = if (item.checked) TextDecoration.LineThrough else TextDecoration.None
    val textColor = if (item.checked) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface

    // Deslizar NUNCA exclui direto: pede confirmação e o cartão volta pro lugar.
    // rememberUpdatedState garante que o callback usado é sempre o mais recente.
    val currentRequestDelete by rememberUpdatedState(onRequestDelete)
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) currentRequestDelete()
            false
        },
        positionalThreshold = { totalDistance -> totalDistance * 0.35f }
    )

    Box(modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 3.dp)) {
        SwipeToDismissBox(
            state = dismissState,
            enableDismissFromStartToEnd = false,
            backgroundContent = {
                Row(
                    Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(horizontal = 22.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Excluir", color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Default.DeleteOutline, null, tint = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(18.dp),
                color    = if (item.checked) MaterialTheme.colorScheme.surfaceVariant
                           else MaterialTheme.colorScheme.surface,
                shadowElevation = if (item.checked) 0.dp else 2.dp
            ) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {

                    // Linha de cima: caixa de seleção grande + nome + lixeira
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(if (item.checked) Green700 else Color.Transparent)
                                .border(
                                    width = if (item.checked) 0.dp else 2.dp,
                                    color = if (item.checked) Color.Transparent else MaterialTheme.colorScheme.outline,
                                    shape = CircleShape
                                )
                                .clickable(onClick = onCheckClick),
                            contentAlignment = Alignment.Center
                        ) {
                            if (item.checked) {
                                Icon(Icons.Default.Check, "Desmarcar", tint = Color.White,
                                    modifier = Modifier.size(20.dp))
                            }
                        }

                        Column(
                            Modifier
                                .weight(1f)
                                .clickable(onClick = onCheckClick)
                                .padding(horizontal = 12.dp)
                        ) {
                            Text(
                                item.name,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                textDecoration = textDecoration,
                                color = textColor,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                if (item.selectedPrice > 0)
                                    "${item.selectedPrice.brl()} cada · ${item.selectedPriceLabel}"
                                else
                                    "Toque no círculo pra informar o preço",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(onClick = onRequestDelete, modifier = Modifier.size(36.dp)) {
                            Icon(
                                Icons.Default.DeleteOutline, "Excluir item",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // Linha de baixo: quantidade (esquerda) e total do item (direita)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        QtyStepper(
                            quantity = item.quantity,
                            unit = item.unit,
                            onUp = onQtyUp,
                            onDown = onQtyDown
                        )
                        Spacer(Modifier.weight(1f))
                        if (item.selectedPrice > 0) {
                            Text(
                                item.totalPrice.brl(),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = if (item.checked) MaterialTheme.colorScheme.onSurfaceVariant else Green700
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QtyStepper(quantity: Int, unit: String, onUp: () -> Unit, onDown: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StepButton(Icons.Default.Remove, "Diminuir", onDown)
        Text(
            "$quantity $unit",
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp)
        )
        StepButton(Icons.Default.Add, "Aumentar", onUp)
    }
}

@Composable
private fun StepButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, description, modifier = Modifier.size(18.dp))
    }
}

// ── Confirmação de exclusão (usada em itens e listas) ───────────────────────

@Composable
fun ConfirmDeleteDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon  = { Icon(Icons.Default.DeleteOutline, null, tint = MaterialTheme.colorScheme.error) },
        title = { Text(title) },
        text  = { Text(message) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) { Text("Excluir") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

// ── Como informar o preço: câmera ou digitado ───────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PriceMethodSheet(
    item: ShoppingItem,
    onCamera: () -> Unit,
    onManual: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column {
                Text("Qual é o preço?", style = MaterialTheme.typography.titleLarge)
                Text(
                    item.name,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            MethodOption(
                icon = Icons.Default.CameraAlt,
                title = "Ler pela câmera",
                subtitle = "Aponte pra etiqueta — pego o preço em destaque",
                container = MaterialTheme.colorScheme.primaryContainer,
                onClick = onCamera
            )
            MethodOption(
                icon = Icons.Default.Keyboard,
                title = "Digitar o preço",
                subtitle = "Informe o valor e a quantidade na mão",
                container = MaterialTheme.colorScheme.surfaceVariant,
                onClick = onManual
            )
        }
    }
}

@Composable
private fun MethodOption(
    icon: ImageVector,
    title: String,
    subtitle: String,
    container: Color,
    onClick: () -> Unit
) {
    Surface(onClick = onClick, shape = RoundedCornerShape(18.dp), color = container) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = Green700)
            }
            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ── Preço digitado ───────────────────────────────────────────────────────────

@Composable
private fun ManualPriceDialog(
    item: ShoppingItem,
    onConfirm: (qty: Int, price: Double) -> Unit,
    onDismiss: () -> Unit
) {
    var price by remember { mutableStateOf(if (item.selectedPrice > 0) item.selectedPrice.asInput() else "") }
    var qty   by remember { mutableStateOf(item.quantity.toString()) }

    val priceValue = price.parseMoney()
    val qtyValue   = qty.toIntOrNull()?.coerceAtLeast(1) ?: 1
    val valid      = priceValue != null && priceValue > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(item.name, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Preço de cada unidade") },
                    prefix = { Text("R$ ") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = qty,
                    onValueChange = { qty = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Quantidade (${item.unit})") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                if (valid) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Green100)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total do item", fontWeight = FontWeight.Medium, color = Green900)
                        Text((priceValue!! * qtyValue).brl(), fontWeight = FontWeight.Bold, color = Green700)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = valid,
                onClick = { priceValue?.let { onConfirm(qtyValue, it) } }
            ) { Text("Colocar no carrinho") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

// ── Orçamento (pode ser definido depois de criar a lista) ───────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetDialog(
    current: Double,
    onSave: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(if (current > 0) current.asInput() else "") }
    val value = text.parseMoney()
    val suggestions = listOf(100, 200, 300, 500, 1000)

    AlertDialog(
        onDismissRequest = onDismiss,
        icon  = { Icon(Icons.Default.Savings, null, tint = Green700) },
        title = { Text("Orçamento da lista") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Quanto você quer gastar? Dá pra mudar quando quiser.",
                    fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Valor") },
                    prefix = { Text("R$ ") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(suggestions) { s ->
                        SuggestionChip(onClick = { text = "$s" }, label = { Text("R$ $s") })
                    }
                }
                if (current > 0) {
                    TextButton(onClick = { onSave(0.0) }) {
                        Text("Remover orçamento", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = value != null && value > 0,
                onClick = { value?.let(onSave) }
            ) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

// ── Resumo ao finalizar ──────────────────────────────────────────────────────

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
                Text(summary.subtotal.brl(), style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(10.dp))
                when {
                    savings == null -> Text(
                        "Sem histórico suficiente pra calcular economia ainda.",
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    savings > 0.009 -> Text(
                        "Você economizou ${savings.brl()} comparado ao menor preço visto antes desta lista.",
                        color = Green700, fontWeight = FontWeight.SemiBold, fontSize = 13.sp
                    )
                    savings < -0.009 -> Text(
                        "Pagou ${(-savings).brl()} a mais do que o menor preço já visto antes.",
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
            val priceInfo = if (item.selectedPrice > 0) " — ${item.totalPrice.brl()}" else ""
            sb.appendLine("  $mark ${item.name} (${item.quantity}x)$priceInfo")
        }
        sb.appendLine()
    }

    sb.appendLine("Total: ${items.sumOf { it.totalPrice }.brl()}")
    sb.appendLine()
    sb.append("Feito com Lista Inteligente 📱")
    return sb.toString()
}

// ── Lista vazia ──────────────────────────────────────────────────────────────

@Composable
fun EmptyListHint(listType: String = ListTypes.MERCADO, onScan: () -> Unit, onAdd: () -> Unit) {
    val subtitle = when (listType) {
        ListTypes.FARMACIA -> "Escaneie a caixa do produto ou adicione manualmente"
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
        Text("Sua lista está vazia", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Spacer(Modifier.height(8.dp))
        Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
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

// ── Adição manual ────────────────────────────────────────────────────────────

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
                        value = qty, onValueChange = { qty = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Qtd") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = price, onValueChange = { price = it },
                        label = { Text("Preço R$ (opcional)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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
                val p = price.parseMoney() ?: 0.0
                val q = (qty.toIntOrNull() ?: 1).coerceAtLeast(1)
                if (name.isNotBlank()) onAdd(name.trim(), q, p, "Manual", unit, category)
            }) { Text("Adicionar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
