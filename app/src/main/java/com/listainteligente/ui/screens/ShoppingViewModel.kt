package com.listainteligente.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.listainteligente.data.ShoppingRepository
import com.listainteligente.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─── Estados de UI ─────────────────────────────────────────────────────────

data class ListScreenState(
    val lists: List<ShoppingList> = emptyList(),
    val isLoading: Boolean = true
)

data class DetailScreenState(
    val list: ShoppingList? = null,
    val items: List<ShoppingItem> = emptyList(),
    val summary: ListSummary = ListSummary(0, 0, 0.0, 0.0, 0.0),
    val isLoading: Boolean = true
)

// ─── ViewModel ──────────────────────────────────────────────────────────────

@HiltViewModel
class ShoppingViewModel @Inject constructor(
    private val repo: ShoppingRepository
) : ViewModel() {

    // Tela de listas
    val listState: StateFlow<ListScreenState> = repo.getActiveLists()
        .map { ListScreenState(lists = it, isLoading = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ListScreenState())

    // Lista selecionada
    private val _selectedListId = MutableStateFlow<Long?>(null)

    val detailState: StateFlow<DetailScreenState> = _selectedListId
        .filterNotNull()
        .flatMapLatest { listId ->
            combine(
                flow { emit(repo.getListById(listId)) },
                repo.getItems(listId)
            ) { list, items ->
                val budget = list?.budget ?: 0.0
                DetailScreenState(
                    list      = list,
                    items     = items,
                    isLoading = false,
                    summary   = computeSummary(items, budget)
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DetailScreenState())

    fun selectList(id: Long) { _selectedListId.value = id }

    // ── Listas ───────────────────────────────────────────────────────────────

    fun createList(name: String, budget: Double = 0.0, store: String = "") =
        viewModelScope.launch { repo.createList(name, budget, store) }

    fun deleteList(list: ShoppingList) =
        viewModelScope.launch { repo.deleteList(list) }

    fun updateListBudget(list: ShoppingList, budget: Double) =
        viewModelScope.launch { repo.updateList(list.copy(budget = budget)) }

    // ── Itens ────────────────────────────────────────────────────────────────

    fun addItem(
        listId: Long,
        name: String,
        qty: Int,
        price: Double,
        priceLabel: String,
        unit: String = "un",
        category: String = "Outros",
        note: String = ""
    ) = viewModelScope.launch {
        repo.addItem(ShoppingItem(
            listId            = listId,
            name              = name,
            quantity          = qty,
            selectedPrice     = price,
            selectedPriceLabel = priceLabel,
            unit              = unit,
            category          = category,
            note              = note
        ))
    }

    fun updateItemPrice(item: ShoppingItem, price: Double, priceLabel: String) =
        viewModelScope.launch {
            repo.updateItem(item.copy(selectedPrice = price, selectedPriceLabel = priceLabel))
        }

    fun findItem(id: Long): ShoppingItem? = detailState.value.items.find { it.id == id }

    /** Chamado após escanear a etiqueta de um item já existente na lista:
     *  renomeia o item para o que está na etiqueta (fica fácil conferir que é
     *  o produto certo), grava o preço/quantidade lidos e marca como encontrado. */
    fun setScannedPriceAndCheck(item: ShoppingItem, scannedName: String, qty: Int, price: Double, priceLabel: String) =
        viewModelScope.launch {
            // Só troca o nome se o OCR conseguiu ler algo de verdade — se caiu no
            // fallback genérico "Produto", mantém o nome que o usuário já tinha.
            val newName = if (scannedName.isNotBlank() && !scannedName.equals("Produto", ignoreCase = true))
                scannedName else item.name

            repo.updateItem(item.copy(
                name               = newName,
                quantity           = qty,
                selectedPrice      = price,
                selectedPriceLabel = priceLabel,
                checked            = true
            ))
        }

    fun updateItemQty(item: ShoppingItem, qty: Int) =
        viewModelScope.launch {
            if (qty <= 0) repo.deleteItem(item)
            else repo.updateItem(item.copy(quantity = qty))
        }

    fun toggleChecked(item: ShoppingItem) =
        viewModelScope.launch { repo.setChecked(item.id, !item.checked) }

    fun deleteItem(item: ShoppingItem) =
        viewModelScope.launch { repo.deleteItem(item) }

    fun deleteChecked(listId: Long) =
        viewModelScope.launch { repo.deleteChecked(listId) }

    // ── Cálculo local de resumo ──────────────────────────────────────────────

    private fun computeSummary(items: List<ShoppingItem>, budget: Double): ListSummary {
        val subtotal = items.sumOf { it.totalPrice }
        return ListSummary(
            totalItems   = items.size,
            checkedItems = items.count { it.checked },
            subtotal     = subtotal,
            budget       = budget,
            savings      = 0.0
        )
    }
}
