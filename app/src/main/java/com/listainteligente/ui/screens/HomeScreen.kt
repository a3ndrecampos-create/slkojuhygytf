package com.listainteligente.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.listainteligente.model.ShoppingList
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
                    ShoppingListCard(list = list, onClick = { onOpenList(list.id) },
                        onDelete = { vm.deleteList(list) })
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

@Composable
fun ShoppingListCard(list: ShoppingList, onClick: () -> Unit, onDelete: () -> Unit) {
    val fmt = remember { SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")) }

    ElevatedCard(
        onClick   = onClick,
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(2.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.ShoppingCart, null,
                tint = Green700,
                modifier = Modifier.size(36.dp)
            )
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(list.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    buildString {
                        if (list.store.isNotEmpty()) append("${list.store} · ")
                        append(fmt.format(Date(list.createdAt)))
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (list.budget > 0) {
                    Text(
                        "Orçamento: R$ ${"%.2f".format(list.budget).replace(".", ",")}",
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color      = Green700
                    )
                }
            }
            Icon(Icons.Default.ChevronRight, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
