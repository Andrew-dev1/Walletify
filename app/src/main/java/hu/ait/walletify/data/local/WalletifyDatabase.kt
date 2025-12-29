package hu.ait.walletify.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Room database for local caching.
 * Note: Currently not actively used as all data is stored in Firestore.
 * This is kept for potential future offline support.
 */
@Database(
    entities = [TransactionEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(CategoryListConverter::class)
abstract class WalletifyDatabase : RoomDatabase() {
    
    abstract fun transactionDao(): TransactionDao
    
    companion object {
        @Volatile
        private var INSTANCE: WalletifyDatabase? = null
        
        fun getDatabase(context: Context): WalletifyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WalletifyDatabase::class.java,
                    "walletify_database"
                )
                    .fallbackToDestructiveMigration(false) // For development only
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
