package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        CustomerEntity::class,
        TransactionEntity::class,
        InventoryItemEntity::class,
        InvoiceEntity::class,
        InvoiceItemEntity::class,
        CashEntryEntity::class,
        StoreProfileEntity::class,
        AppSettingsEntity::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun customerDao(): CustomerDao
    abstract fun transactionDao(): TransactionDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun cashbookDao(): CashbookDao
    abstract fun storeProfileDao(): StoreProfileDao
    abstract fun appSettingsDao(): AppSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Add party type to customers
                db.execSQL("ALTER TABLE customers ADD COLUMN type TEXT NOT NULL DEFAULT 'CUSTOMER'")

                // 2. Recreate transactions table to drop isCancelled and add paymentMode, linkedToTxId, isCompleted
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS transactions_new (
                        id TEXT NOT NULL PRIMARY KEY,
                        customerId TEXT NOT NULL,
                        amount REAL NOT NULL,
                        type TEXT NOT NULL,
                        note TEXT NOT NULL,
                        date INTEGER NOT NULL,
                        paymentMode TEXT NOT NULL DEFAULT 'CASH',
                        linkedToTxId TEXT DEFAULT NULL,
                        isCompleted INTEGER NOT NULL DEFAULT 0,
                        billNumber TEXT NOT NULL,
                        FOREIGN KEY(customerId) REFERENCES customers(id) ON DELETE CASCADE
                    )
                """.trimIndent())

                db.execSQL("""
                    INSERT INTO transactions_new (id, customerId, amount, type, note, date, paymentMode, linkedToTxId, isCompleted, billNumber)
                    SELECT id, customerId, amount, type, note, date, 'CASH', NULL, 0, billNumber
                    FROM transactions
                """.trimIndent())

                // Map old isCancelled = 1 rows into linked settled payments
                val cursor = db.query("SELECT id, customerId, amount, date FROM transactions WHERE isCancelled = 1 AND type = 'GAVE'")
                while (cursor.moveToNext()) {
                    val billId = cursor.getString(0)
                    val custId = cursor.getString(1)
                    val amt = cursor.getDouble(2)
                    val dt = cursor.getLong(3)
                    val paymentId = java.util.UUID.randomUUID().toString()
                    db.execSQL(
                        "INSERT INTO transactions_new (id, customerId, amount, type, note, date, paymentMode, linkedToTxId, isCompleted, billNumber) VALUES (?, ?, ?, 'GOT', 'Settled (migrated)', ?, 'CASH', ?, 1, '')",
                        arrayOf(paymentId, custId, amt, dt, billId)
                    )
                    db.execSQL("UPDATE transactions_new SET isCompleted = 1 WHERE id = ?", arrayOf(billId))
                }
                cursor.close()

                db.execSQL("DROP TABLE transactions")
                db.execSQL("ALTER TABLE transactions_new RENAME TO transactions")

                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_customerId ON transactions(customerId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_date ON transactions(date)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_linkedToTxId ON transactions(linkedToTxId)")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ghanshyam_billing.db"
                )
                .addMigrations(MIGRATION_1_2)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
