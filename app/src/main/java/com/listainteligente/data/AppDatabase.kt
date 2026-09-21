package com.listainteligente.data

import androidx.room.*
import com.listainteligente.model.ShoppingItem
import com.listainteligente.model.ShoppingList
import kotlinx.coroutines.flow.Flow

// ─── DAOs ────────────────────────────────────────────────────────────────────

@Dao
interface ShoppingListDao {
    @Query("SELECT * FROM shopping_lists WHERE isActive = 1 ORDER BY createdAt DESC")
    fun getActiveLists(): Flow<List<ShoppingList>>

    @Query("SELECT * FROM shopping_lists ORDER BY createdAt DESC")
    fun getAllLists(): Flow<List<ShoppingList>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(list: ShoppingList): Long

    @Update
    suspend fun update(list: ShoppingList)

    @Delete
    suspend fun delete(list: ShoppingList)

    @Query("SELECT * FROM shopping_lists WHERE id = :id")
    suspend fun getById(id: Long): ShoppingList?
}

@Dao
interface ShoppingItemDao {
    @Query("SELECT * FROM shopping_items WHERE listId = :listId ORDER BY category, name")
    fun getItemsByList(listId: Long): Flow<List<ShoppingItem>>

    @Query("SELECT * FROM shopping_items WHERE listId = :listId AND checked = 0 ORDER BY category, name")
    fun getPendingItems(listId: Long): Flow<List<ShoppingItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ShoppingItem): Long

    @Update
    suspend fun update(item: ShoppingItem)

    @Delete
    suspend fun delete(item: ShoppingItem)

    @Query("UPDATE shopping_items SET checked = :checked WHERE id = :id")
    suspend fun setChecked(id: Long, checked: Boolean)

    @Query("UPDATE shopping_items SET quantity = :qty WHERE id = :id")
    suspend fun updateQty(id: Long, qty: Int)

    @Query("DELETE FROM shopping_items WHERE listId = :listId AND checked = 1")
    suspend fun deleteChecked(listId: Long)

    @Query("SELECT SUM(selectedPrice * quantity) FROM shopping_items WHERE listId = :listId")
    fun getTotalByList(listId: Long): Flow<Double?>

    @Query("SELECT COUNT(*) FROM shopping_items WHERE listId = :listId")
    fun countByList(listId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM shopping_items WHERE listId = :listId AND checked = 1")
    fun countCheckedByList(listId: Long): Flow<Int>

    @Query("SELECT * FROM shopping_items")
    fun getAllItems(): Flow<List<ShoppingItem>>
}

// ─── Database ─────────────────────────────────────────────────────────────────

@Database(
    entities  = [ShoppingList::class, ShoppingItem::class],
    version   = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun listDao(): ShoppingListDao
    abstract fun itemDao(): ShoppingItemDao
}
