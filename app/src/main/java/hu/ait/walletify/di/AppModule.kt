package hu.ait.walletify.di

import com.google.firebase.functions.FirebaseFunctions
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import hu.ait.walletify.data.plaid.FirebasePlaidRepository
import hu.ait.walletify.data.plaid.PlaidRepository
import hu.ait.walletify.data.repository.AuthRepository
import hu.ait.walletify.data.repository.FakeFinanceRepository
import hu.ait.walletify.data.repository.FinanceRepository
import hu.ait.walletify.data.repository.FirebaseAuthRepository

import javax.inject.Singleton





@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: FirebaseAuthRepository): AuthRepository


    @Binds
    @Singleton
    abstract fun bindPlaidRepository(
        impl: FirebasePlaidRepository
    ): PlaidRepository

    @Binds
    @Singleton
    abstract fun bindFinanceRepository(impl: FakeFinanceRepository): FinanceRepository

    companion object {
        @Provides
        @Singleton
        fun provideFirebaseFunctions(): FirebaseFunctions =
        // If your Firebase Functions are deployed to a different region,
        // change this to: FirebaseFunctions.getInstance("your-region")
        // Common regions: "us-central1", "us-east1", "europe-west1", "asia-northeast1"
        FirebaseFunctions.getInstance("us-central1")
    }





}


