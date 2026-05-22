package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wallets")
data class Wallet(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val type: String, // CASH, BANK, WALLET, SAVINGS
    val balance: Double,
    val colorHex: String,
    val iconName: String
)

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val walletId: Int,
    val walletName: String,
    val type: String, // INCOME, EXPENSE
    val amount: Double,
    val categoryName: String,
    val categoryIcon: String,
    val categoryColor: String,
    val note: String,
    val timestamp: Long,
    val isRecurring: Boolean = false,
    val recurrencePeriod: String = "NONE" // NONE, DAILY, WEEKLY, MONTHLY
)

@Entity(tableName = "budgets")
data class Budget(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val categoryName: String,
    val categoryIcon: String,
    val categoryColor: String,
    val limitAmount: Double,
    val spentAmount: Double = 0.0,
    val month: String // Format: "YYYY-MM"
)

@Entity(tableName = "savings_goals")
data class SavingsGoal(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val targetAmount: Double,
    val currentAmount: Double = 0.0,
    val targetDate: Long,
    val note: String = ""
)

@Entity(tableName = "settings")
data class AppSetting(
    @PrimaryKey val key: String,
    val value: String
)
