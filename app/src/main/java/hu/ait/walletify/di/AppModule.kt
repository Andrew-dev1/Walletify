package hu.ait.walletify.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import hu.ait.walletify.data.repository.AuthRepository
import hu.ait.walletify.data.repository.FakeAuthRepository
import hu.ait.walletify.data.repository.FakeFinanceRepository
import hu.ait.walletify.data.repository.FinanceRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: FakeAuthRepository): AuthRepository

    @Binds
    @Singleton
    abstract fun bindFinanceRepository(impl: FakeFinanceRepository): FinanceRepository
}


