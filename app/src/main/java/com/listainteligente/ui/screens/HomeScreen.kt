package com.listainteligente.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.listainteligente.model.ListProgress
import com.listainteligente.model.ListTypes
import com.listainteligente.model.ShoppingList
import com.listainteligente.ui.brl
import com.listainteligente.ui.parseMoney
import com.listainteligente.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenList: (Long) -> Unit,
    vm: ShoppingViewModel = hiltViewModel()
) {
    val state by vm.listState.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var listToDelete by remember { mutableStateOf<ShoppingList?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Lista Inteligente", fontWeight = FontWeight.Bold)
                        Text("O que vamos comprar hoje? 🛒", fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Nova lista") }
            )
        }
    ) { padding ->

        if (state.lists.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Text("🛒", fontSize = 64.sp)
                    Spacer(Modifier.height(16.dp))
                    Text("Nenhuma lista ainda", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Crie sua primeira lista, escaneie as etiquetas e acompanhe quanto vai gastar.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Criar lista")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 110.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(key = "hero") { HomeHero(state) }
                items(state.lists, key = { it.id }) { list ->
                    ShoppingListCard(
                        list        = list,
                        progress    = state.progressByList[list.id] ?: ListProgress(),
                        onClick     = { onOpenList(list.id) },
                        onRequestDelete = { listToDelete = list },
                        onDuplicate = { vm.duplicateList(list) }
                    )
                }
            }
        }
    }

    listToDelete?.let { list ->
        ConfirmDeleteDialog(
            title = "Excluir lista?",
            message = "\"${list.name}\" e todos os itens dela serão removidos.",
            onConfirm = {
                vm.deleteList(list)
                listToDelete = null
            },
            onDismiss = { listToDelete = null }
        )
    }

    if (showCreateDialog) {
        CreateListDialog(
            onCreate  = { name, budget, store, listType ->
                vm.createList(name, budget, store, listType)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false }
        )
    }
}

// ── Resumo geral no topo da Home ─────────────────────────────────────────────

@Composable
private fun HomeHero(state: ListScreenState) {
    val pendingItems = state.progressByList.values.sumOf { it.totalItems - it.checkedItems }
    val totalEstimado = state.progressByList.values.sumOf { it.subtotal }

    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(Green900, Green500)))
            .padding(20.dp)
    ) {
        Column {
            Text("Total estimado das suas listas", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
            Spacer(Modifier.height(2.dp))
            Text(
                totalEstimado.brl(),
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HeroStat(
                    Modifier.weight(1f), Icons.Default.ListAlt,
                    "${state.lists.size}",
                    if (state.lists.size == 1) "lista ativa" else "listas ativas"
                )
                HeroStat(
                    Modifier.weight(1f), Icons.Default.ShoppingBasket,
                    "$pendingItems",
                    if (pendingItems == 1) "item falta" else "itens faltam"
                )
            }
        }
    }
}

@Composable
private fun HeroStat(modifier: Modifier, icon: ImageVector, value: String, label: String) {
    Row(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.18f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Column {
            Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(label, color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp)
        }
    }
}

// ── Cartão de lista ──────────────────────────────────────────────────────────

private fun typeTint(type: String): Pair<Color, Color> = when (type) {
    ListTypes.MERCADO  -> Green100 to Green700
    ListTypes.FARMACIA -> Blue100 to Blue700
    ListTypes.CASA     -> Orange100 to Orange700
    else               -> Gray100 to Gray600
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListCard(
    list: ShoppingList,
    progress: ListProgress,
    onClick: () -> Unit,
    onRequestDelete: () -> Unit,
    onDuplicate: () -> Unit
) {
    val fmt = remember { SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")) }
    val (tintBg, tintFg) = typeTint(list.listType)

    // Deslizar pra excluir nunca apaga direto: pede confirmação e o cartão volta pro lugar.
    val currentRequestDelete by rememberUpdatedState(onRequestDelete)
    val currentDuplicate by rememberUpdatedState(onDuplicate)
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.EndToStart -> currentRequestDelete()
                SwipeToDismissBoxValue.StartToEnd -> currentDuplicate()
                else -> Unit
            }
            false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val isDuplicateSide = dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isDuplicateSide) Green100 else MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 24.dp),
                contentAlignment = if (isDuplicateSide) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                Icon(
                    if (isDuplicateSide) Icons.Default.ContentCopy else Icons.Default.DeleteOutline,
                    null,
                    tint = if (isDuplicateSide) Green700 else MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    ) {
        ElevatedCard(
            onClick   = onClick,
            modifier  = Modifier.fillMaxWidth(),
            shape     = RoundedCornerShape(20.dp),
            elevation = CardDefaults.elevatedCardElevation(2.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(tintBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(ListTypes.emoji(list.listType), fontSize = 24.sp)
                    }
                    Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                        Text(list.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            buildString {
                                append(list.listType)
                                if (list.store.isNotEmpty()) append(" · ${list.store}")
                                append(" · ")
                                append(fmt.format(Date(list.createdAt)))
                            },
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(Icons.Default.ChevronRight, null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                if (progress.totalItems > 0) {
                    Spacer(Modifier.height(14.dp))
                    LinearProgressIndicator(
                        progress = { progress.progressPercent },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = tintFg,
                        trackColor = tintBg
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "${progress.checkedItems} de ${progress.totalItems} no carrinho",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            progress.subtotal.brl(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = tintFg
                        )
                    }
                } else {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Lista vazia — toque pra começar",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (list.budget > 0) {
                    Spacer(Modifier.height(8.dp))
                    Surface(shape = RoundedCornerShape(8.dp), color = tintBg) {
                        Row(
                            Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Savings, null, tint = tintFg, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "Orçamento ${list.budget.brl()}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = tintFg
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Criar lista ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateListDialog(
    onCreate: (name: String, budget: Double, store: String, listType: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name     by remember { mutableStateOf("") }
    var budget   by remember { mutableStateOf("") }
    var store    by remember { mutableStateOf("") }
    var listType by remember { mutableStateOf(ListTypes.MERCADO) }
    val focus = remember { FocusRequester() }

    val budgetValue = budget.parseMoney() ?: 0.0
    val suggestions = listOf(100, 200, 300, 500, 1000)

    LaunchedEffect(Unit) { focus.requestFocus() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Cabeçalho
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Green100),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PlaylistAdd, null, tint = Green700)
                }
                Column(Modifier.padding(start = 12.dp)) {
                    Text("Nova lista", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Dê um nome e escolha o tipo de compra",
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Nome
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nome da lista") },
                placeholder = { Text("Ex: Compras do mês") },
                leadingIcon = { Icon(Icons.Default.Edit, null) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth().focusRequester(focus)
            )

            // Tipo da lista
            Column {
                Text("Tipo", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ListTypes.all.forEach { type ->
                        val selected = listType == type
                        val (bg, fg) = typeTint(type)
                        Surface(
                            onClick = { listType = type },
                            shape = RoundedCornerShape(16.dp),
                            color = if (selected) bg else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (selected) BorderStroke(2.dp, fg) else null,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                Modifier.padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(ListTypes.emoji(type), fontSize = 24.sp)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    type, fontSize = 12.sp, maxLines = 1,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected) fg else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Estabelecimento
            OutlinedTextField(
                value = store,
                onValueChange = { store = it },
                label = { Text("Estabelecimento (opcional)") },
                placeholder = { Text("Ex: Max Atacadista") },
                leadingIcon = { Icon(Icons.Default.Store, null) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Orçamento
            Column {
                OutlinedTextField(
                    value = budget,
                    onValueChange = { budget = it },
                    label = { Text("Orçamento (opcional)") },
                    placeholder = { Text("0,00") },
                    prefix = { Text("R$ ") },
                    leadingIcon = { Icon(Icons.Default.Savings, null) },
                    supportingText = { Text("Pode deixar em branco e definir depois, dentro da lista.") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    suggestions.forEach { v ->
                        SuggestionChip(onClick = { budget = "$v" }, label = { Text("R$ $v") })
                    }
                }
            }

            Button(
                enabled = name.isNotBlank(),
                onClick = { onCreate(name.trim(), budgetValue, store.trim(), listType) },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(54.dp)
            ) {
                Icon(Icons.Default.Check, null)
                Spacer(Modifier.width(8.dp))
                Text("Criar lista", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}
