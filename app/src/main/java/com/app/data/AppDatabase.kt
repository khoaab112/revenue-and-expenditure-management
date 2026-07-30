package com.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE transactions ADD COLUMN eventId INTEGER")
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `events` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `name` TEXT NOT NULL, 
                `description` TEXT NOT NULL, 
                `startDate` INTEGER NOT NULL, 
                `endDate` INTEGER, 
                `limitAmount` REAL
            )
        """.trimIndent())
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE events ADD COLUMN colorHex TEXT NOT NULL DEFAULT '#FF9800'")
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE transactions ADD COLUMN destinationWalletId INTEGER")
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `debts` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `personName` TEXT NOT NULL, 
                `type` TEXT NOT NULL, 
                `totalAmount` REAL NOT NULL, 
                `remainingAmount` REAL NOT NULL, 
                `walletId` INTEGER NOT NULL, 
                `creationDate` INTEGER NOT NULL, 
                `dueDate` INTEGER, 
                `note` TEXT NOT NULL, 
                `status` TEXT NOT NULL
            )
        """.trimIndent())
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE debts ADD COLUMN repaymentType TEXT NOT NULL DEFAULT 'FLEXIBLE'")
        db.execSQL("ALTER TABLE debts ADD COLUMN periodicAmount REAL")
        db.execSQL("ALTER TABLE debts ADD COLUMN periodType TEXT")
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE wallets ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE wallets ADD COLUMN isClosed INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE wallets ADD COLUMN targetAmount REAL")
    }
}

val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE transactions ADD COLUMN debtId INTEGER")
    }
}

val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE transactions ADD COLUMN notificationKey TEXT")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_transactions_notificationKey ON transactions(notificationKey)")
    }
}

val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `categories` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `name` TEXT NOT NULL, 
                `iconName` TEXT NOT NULL, 
                `colorHex` TEXT NOT NULL, 
                `type` TEXT NOT NULL, 
                `parentName` TEXT, 
                `isCustom` INTEGER NOT NULL,
                `displayOrder` INTEGER NOT NULL
            )
        """.trimIndent())

        // Insert default categories
        val defaultCategories = com.app.data.Categories.list
        for ((index, cat) in defaultCategories.withIndex()) {
            db.execSQL(
                "INSERT INTO categories (name, iconName, colorHex, type, parentName, isCustom, displayOrder) VALUES (?, ?, ?, ?, ?, 0, ?)",
                arrayOf(cat.name, cat.iconName, cat.colorHex, cat.type, cat.parentName, index)
            )
        }

        // Migrate custom categories
        val cursor = db.query("SELECT value FROM settings WHERE `key` = 'custom_categories'")
        var customCatsStr = ""
        if (cursor.moveToFirst()) {
            customCatsStr = cursor.getString(0)
        }
        cursor.close()

        if (customCatsStr.isNotEmpty()) {
            val parts = customCatsStr.split(";;")
            var customIndex = defaultCategories.size
            for (p in parts) {
                if (p.isBlank()) continue
                val segs = p.split("|")
                if (segs.size >= 4) {
                    val name = segs[0]
                    val icon = segs[1]
                    val color = segs[2]
                    val type = segs[3]
                    val parentName = if (segs.size >= 5 && segs[4].isNotEmpty()) segs[4] else null
                    
                    db.execSQL(
                        "INSERT INTO categories (name, iconName, colorHex, type, parentName, isCustom, displayOrder) VALUES (?, ?, ?, ?, ?, 1, ?)",
                        arrayOf(name, icon, color, type, parentName, customIndex++)
                    )
                }
            }
        }
        
        // Delete the setting
        db.execSQL("DELETE FROM settings WHERE `key` = 'custom_categories'")
    }
}

val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE transactions ADD COLUMN categoryId INTEGER DEFAULT NULL")
        db.execSQL("ALTER TABLE budgets ADD COLUMN categoryId INTEGER DEFAULT NULL")
        db.execSQL("UPDATE transactions SET categoryId = (SELECT id FROM categories WHERE name = transactions.categoryName LIMIT 1) WHERE categoryName IS NOT NULL")
        db.execSQL("UPDATE budgets SET categoryId = (SELECT id FROM categories WHERE name = budgets.categoryName LIMIT 1) WHERE categoryName IS NOT NULL")
    }
}

@Database(
    entities = [
        Wallet::class,
        Transaction::class,
        Budget::class,
        SavingsGoal::class,
        AppSetting::class,
        Event::class,
        Debt::class,
        CategoryEntity::class
    ],
    version = 14,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun financeDao(): FinanceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "finance_database"
                )
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

