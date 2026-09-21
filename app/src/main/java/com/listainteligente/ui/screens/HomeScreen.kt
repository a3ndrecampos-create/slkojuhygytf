package com.listainteligente.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.listainteligente.model.ListProgress
import com.listainteligente.model.ShoppingList
import com.listainteligente.ui.theme.Green100
import com.listainteligente.ui.theme.Green700
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Lista Inteligente", fontWeight = FontWeight.Bold)
                        Text("Economize no mercado", fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
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
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.PlaylistAdd, null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.height(16.dp))
                    Text("Nenhuma lista ainda", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Crie sua primeira lista de compras",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = { showCreateDialog = true }) {
                        Text("Criar lista")
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp,
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = 100.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.lists, key = { it.id }) { list ->
                    ShoppingListCard(
                        list     = list,
                        progress = state.progressByList[list.id] ?: ListProgress(),
                        onClick  = { onOpenList(list.id) },
                        onDelete = { vm.deleteList(list) }
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateListDialog(
            onCreate  = { name, budget, store ->
                vm.createList(name, budget, store)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListCard(
    list: ShoppingList,
    progress: ListProgress,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val fmt = remember { SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")) }
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(Icons.Default.DeleteOutline, null, tint = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
    ) {
        ElevatedCard(
            onClick   = onClick,
            modifier  = Modifier.fillMaxWidth(),
            shape     = RoundedCornerShape(16.dp),
            elevation = CardDefaults.elevatedCardElevation(1.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Green100),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.ShoppingCart, null, tint = Green700, modifier = Modifier.size(24.dp))
                    }
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(list.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            buildString {
                                if (list.store.isNotEmpty()) append("${list.store} · ")
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
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = Green700,
                        trackColor = Green100
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "${progress.checkedItems}/${progress.totalItems} itens encontrados",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "R$ ${"%.2f".format(progress.subtotal).replace(".", ",")}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Green700
                        )
                    }
                } else if (list.budget > 0) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Orçamento: R$ ${"%.2f".format(list.budget).replace(".", ",")}",
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color      = Green700
                    )
                }
            }
        }
    }
}

@Composable
fun CreateListDialog(
    onCreate: (name: String, budget: Double, store: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name   by remember { mutableStateOf("") }
    var budget by remember { mutableStateOf("") }
    var store  by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nova lista de compras") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Nome da lista") },
                    placeholder = { Text("Ex: Compras do mês") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = store, onValueChange = { store = it },
                    label = { Text("Supermercado (opcional)") },
                    placeholder = { Text("Ex: Max Atacadista") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = budget, onValueChange = { budget = it },
                    label = { Text("Orçamento R$ (opcional)") },
                    placeholder = { Text("Ex: 500,00") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isNotBlank()) {
                    val b = budget.replace(",", ".").toDoubleOrNull() ?: 0.0
                    onCreate(name, b, store)
                }
            }) { Text("Criar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
