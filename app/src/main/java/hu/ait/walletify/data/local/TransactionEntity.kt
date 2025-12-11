package hu.ait.walletify.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import hu.ait.walletify.data.model.TransactionItem

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val merchant: String,
    val category: String,
    val amount: Double,
    val date: String,
    val isDebit: Boolean,
    val accountId: String
)

// Extension functions to convert between Entity and Model
fun TransactionEntity.toTransactionItem() = TransactionItem(
    id = id,
    merchant = merchant,
    category = category,
    amount = amount,
    date = date,
    isDebit = isDebit,
    accountId = accountId
)

fun TransactionItem.toEntity() = TransactionEntity(
    id = id,
    merchant = merchant,
    category = category,
    amount = amount,
    date = date,
    isDebit = isDebit,
    accountId = accountId
)