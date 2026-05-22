package com.example.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

object IconMapper {
    fun getIconByName(name: String): ImageVector {
        return when (name) {
            "Restaurant" -> Icons.Default.Restaurant
            "DirectionsCar" -> Icons.Default.DirectionsCar
            "ShoppingBag" -> Icons.Default.ShoppingBag
            "Receipt" -> Icons.Default.Receipt
            "SportsEsports" -> Icons.Default.SportsEsports
            "School" -> Icons.Default.School
            "LocalHospital" -> Icons.Default.LocalHospital
            "Home" -> Icons.Default.Home
            "Work" -> Icons.Default.Work
            "CardGiftcard" -> Icons.Default.CardGiftcard
            "Storefront" -> Icons.Default.Storefront
            "Payments" -> Icons.Default.Payments
            "AccountBalance" -> Icons.Default.AccountBalance
            "AccountBalanceWallet" -> Icons.Default.AccountBalanceWallet
            "Savings" -> Icons.Default.Savings
            "TrendingUp" -> Icons.Default.TrendingUp
            "TrendingDown" -> Icons.Default.TrendingDown
            "Lock" -> Icons.Default.Lock
            "Settings" -> Icons.Default.Settings
            else -> Icons.Default.Category
        }
    }
}
