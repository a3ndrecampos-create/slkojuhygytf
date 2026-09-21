package com.listainteligente.model

import androidx.room.Entity
import androidx.room.PrimaryKey

// ─── Opção de preço escaneada da etiqueta ───────────────────────────────────

data class PriceOption(
    val label: String,          // "Varejo", "Atacado", "Clube Max", "App"
    val price: Double,          // preço unitário
    val minQty: Int = 1,        // quantidade mínima para este preço
    val maxQty: Int? = null,    // limite (ex: 50 unidades)
    val requiresApp: Boolean = false,
    val requiresCard: Boolean = false
)

// ─── Item da lista de compras ───────────────────────────────────────────────

@Entity(tableName = "shopping_items")
data class ShoppingItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val listId: Long,
    val name: String,
    val quantity: Int = 1,
    val unit: String = "un",            // "un", "kg", "g", "L", "ml"
    val selectedPrice: Double = 0.0,
    val selectedPriceLabel: String = "Varejo",
    val allPricesJson: String = "[]",   // JSON das PriceOption disponíveis
    val checked: Boolean = false,
    val note: String = "",
    val category: String = "Outros",
    val imageUri: String = ""
) {
    val totalPrice: Double get() = selectedPrice * quantity
}

// ─── Lista de compras ────────────────────────────────────────────────────────

@Entity(tableName = "shopping_lists")
data class ShoppingList(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val budget: Double = 0.0,           // orçamento definido pelo usuário
    val store: String = "",
    val isActive: Boolean = true
)

// ─── Resultado do OCR da etiqueta ───────────────────────────────────────────

data class ScannedLabel(
    val productName: String,
    val ean: String = "",
    val priceOptions: List<PriceOption>,
    val rawText: String = ""
)

// ─── Resumo financeiro da lista ──────────────────────────────────────────────

data class ListSummary(
    val totalItems: Int,
    val checkedItems: Int,
    val subtotal: Double,
    val budget: Double,
    val savings: Double            // economia comparando varejo vs preço escolhido
) {
    val remaining: Double get() = budget - subtotal
    val isOverBudget: Boolean get() = budget > 0 && subtotal > budget
    val progressPercent: Float get() = if (budget > 0) (subtotal / budget).toFloat().coerceIn(0f, 1f) else 0f
}

// ─── Categorias padrão ──────────────────────────────────────────────────────

object Categories {
    val all = listOf(
        "Hortifruti", "Carnes", "Laticínios", "Padaria",
        "Bebidas", "Limpeza", "Higiene", "Mercearia",
        "Frios", "Congelados", "Outros"
    )
}
