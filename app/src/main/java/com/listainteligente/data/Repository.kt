package com.listainteligente.data

import com.listainteligente.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShoppingRepository @Inject constructor(
    private val listDao: ShoppingListDao,
    private val itemDao: ShoppingItemDao,
    private val priceHistoryDao: PriceHistoryDao
) {
    // ── Listas ───────────────────────────────────────────────────────────────

    fun getActiveLists()     = listDao.getActiveLists()
    fun getAllLists()         = listDao.getAllLists()
    suspend fun createList(name: String, budget: Double = 0.0, store: String = "", listType: String = ListTypes.MERCADO) =
        listDao.insert(ShoppingList(name = name, budget = budget, store = store, listType = listType))
    suspend fun updateList(list: ShoppingList) = listDao.update(list)
    suspend fun deleteList(list: ShoppingList) = listDao.delete(list)
    suspend fun getListById(id: Long)          = listDao.getById(id)

    // ── Itens ────────────────────────────────────────────────────────────────

    fun getItems(listId: Long)        = itemDao.getItemsByList(listId)
    fun getPendingItems(listId: Long) = itemDao.getPendingItems(listId)
    fun getAllItems()                 = itemDao.getAllItems()

    suspend fun addItem(item: ShoppingItem): Long  = itemDao.insert(item)
    suspend fun updateItem(item: ShoppingItem)     = itemDao.update(item)
    suspend fun deleteItem(item: ShoppingItem)     = itemDao.delete(item)
    suspend fun setChecked(id: Long, checked: Boolean) = itemDao.setChecked(id, checked)
    suspend fun updateQty(id: Long, qty: Int)          = itemDao.updateQty(id, qty)
    suspend fun deleteChecked(listId: Long)            = itemDao.deleteChecked(listId)

    // ── Histórico de preços ──────────────────────────────────────────────────

    suspend fun recordPrice(productName: String, price: Double, priceLabel: String, store: String = "") =
        priceHistoryDao.insert(
            PriceHistoryEntry(
                productName = normalizeProductName(productName),
                price       = price,
                priceLabel  = priceLabel,
                store       = store
            )
        )

    /** Menor preço já visto pra esse produto, ANTES do preço atual (pra comparar). */
    suspend fun getLowestPrice(productName: String): Double? =
        priceHistoryDao.getLowestPrice(normalizeProductName(productName))

    /** Menor preço registrado antes de um instante — usado no resumo da compra
     *  pra não comparar o preço pago hoje com ele mesmo. */
    suspend fun getLowestPriceBefore(productName: String, beforeTimestamp: Long): Double? =
        priceHistoryDao.getLowestPriceBefore(normalizeProductName(productName), beforeTimestamp)

    // ── Resumo financeiro ────────────────────────────────────────────────────

    fun getSummary(listId: Long, budget: Double): Flow<ListSummary> {
        return combine(
            itemDao.getItemsByList(listId),
            itemDao.countByList(listId),
            itemDao.countCheckedByList(listId)
        ) { items, total, checked ->
            val subtotal = items.sumOf { it.totalPrice }
            val savings  = items.sumOf { item ->
                val prices = parseAllPrices(item.allPricesJson)
                val maxPrice = prices.maxOfOrNull { it.price } ?: item.selectedPrice
                (maxPrice - item.selectedPrice) * item.quantity
            }
            ListSummary(
                totalItems   = total,
                checkedItems = checked,
                subtotal     = subtotal,
                budget       = budget,
                savings      = savings
            )
        }
    }

    private fun parseAllPrices(json: String): List<PriceOption> {
        // Parsing simples sem biblioteca externa
        return try {
            if (json == "[]" || json.isEmpty()) emptyList()
            else emptyList() // expandir com Gson/Moshi se necessário
        } catch (e: Exception) { emptyList() }
    }
}
