package hu.ait.walletify.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import hu.ait.walletify.data.model.TransactionItem

/**
 * Room entity for local transaction caching.
 * Note: This is kept for potential future offline support, but currently not actively used.
 */
@Entity(tableName = "transactions")
@TypeConverters(CategoryListConverter::class)
data class TransactionEntity(
    @PrimaryKey val transactionId: String,
    val accountId: String,
    val amount: Double,
    val date: Long, // Timestamp in milliseconds
    val name: String,
    val category: List<String>, // Plaid category hierarchy
    val pending: Boolean = false,
    val merchantName: String? = null,
    val syncedAt: Long = System.currentTimeMillis()
)

/**
 * Type converter for Room to handle List<String> category field.
 * Converts List<String> to/from comma-separated String for Room storage.
 */
class CategoryListConverter {
    @androidx.room.TypeConverter
    fun fromString(value: String): List<String> {
        return if (value.isEmpty()) emptyList() else value.split(",")
    }

    @androidx.room.TypeConverter
    fun toString(list: List<String>): String {
        return list.joinToString(",")
    }
}

// Extension functions to convert between Entity and Model
fun TransactionEntity.toTransactionItem() = TransactionItem(
    transactionId = transactionId,
    accountId = accountId,
    amount = amount,
    date = date,
    name = name,
    category = category,
    pending = pending,
    merchantName = merchantName,
    syncedAt = syncedAt
)

fun TransactionItem.toEntity() = TransactionEntity(
    transactionId = transactionId,
    accountId = accountId,
    amount = amount,
    date = date,
    name = name,
    category = category,
    pending = pending,
    merchantName = merchantName,
    syncedAt = syncedAt
)