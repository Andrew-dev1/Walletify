package hu.ait.walletify.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import hu.ait.walletify.data.local.TransactionDao
import hu.ait.walletify.data.local.WalletifyDatabase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): WalletifyDatabase {
        return WalletifyDatabase.getDatabase(context)
    }

    @Provides
    fun provideTransactionDao(database: WalletifyDatabase): TransactionDao {
        return database.transactionDao()
    }
}