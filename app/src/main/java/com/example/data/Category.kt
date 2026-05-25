package com.example.data

data class FinanceCategory(
    val name: String,
    val iconName: String,
    val colorHex: String,
    val type: String, // INCOME, EXPENSE, BOTH
    val parentName: String? = null // For subcategories
)

object Categories {
    val list = listOf(
        FinanceCategory("Ăn uống", "Restaurant", "#FF9800", "EXPENSE"),
        FinanceCategory("Di chuyển", "DirectionsCar", "#2196F3", "EXPENSE"),
        FinanceCategory("Mua sắm", "ShoppingBag", "#E91E63", "EXPENSE"),
        FinanceCategory("Hóa đơn", "Receipt", "#FFC107", "EXPENSE"),
        FinanceCategory("Giải trí", "SportsEsports", "#9C27B0", "EXPENSE"),
        FinanceCategory("Giáo dục", "School", "#03A9F4", "EXPENSE"),
        FinanceCategory("Sức khỏe", "LocalHospital", "#F44336", "EXPENSE"),
        FinanceCategory("Nhà cửa", "Home", "#795548", "EXPENSE"),
        FinanceCategory("Lương", "Work", "#4CAF50", "INCOME"),
        FinanceCategory("Thưởng", "CardGiftcard", "#8BC34A", "INCOME"),
        FinanceCategory("Kinh doanh", "Storefront", "#009688", "INCOME"),
        FinanceCategory("Khác", "Category", "#607D8B", "BOTH")
    )

    fun getByCategoryName(name: String): FinanceCategory {
        return list.firstOrNull { it.name.lowercase() == name.lowercase() }
            ?: FinanceCategory("Khác", "Category", "#607D8B", "BOTH")
    }
}
