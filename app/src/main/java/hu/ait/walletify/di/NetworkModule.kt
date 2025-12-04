package hu.ait.walletify.di

import hu.ait.walletify.BuildConfig
//import hu.ait.walletify.data.plaid.PlaidApi
import hu.ait.walletify.data.plaid.PlaidConfig
import hu.ait.walletify.data.plaid.PlaidRepository
//import hu.ait.walletify.data.plaid.PlaidSandboxRepository
import javax.inject.Singleton
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.create
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

//    private const val PLAID_BASE_URL = "https://sandbox.plaid.com/"
//
//    @Provides
//    @Singleton
//    fun provideLoggingInterceptor(): HttpLoggingInterceptor =
//        HttpLoggingInterceptor().apply {
//            level = HttpLoggingInterceptor.Level.BASIC
//        }
//
//    @Provides
//    @Singleton
//    fun provideOkHttpClient(
//        loggingInterceptor: HttpLoggingInterceptor
//    ): OkHttpClient = OkHttpClient.Builder()
//        .addInterceptor(loggingInterceptor)
//        .build()
//
//    @Provides
//    @Singleton
//    fun providePlaidApi(okHttpClient: OkHttpClient): PlaidApi {
//        val contentType = "application/json".toMediaType()
//        return Retrofit.Builder()
//            .baseUrl(PLAID_BASE_URL)
//            .client(okHttpClient)
//            .addConverterFactory(Json { ignoreUnknownKeys = true }.asConverterFactory(contentType))
//            .build()
//            .create()
//    }
//
//    @Provides
//    @Singleton
//    fun providePlaidConfig(): PlaidConfig = PlaidConfig(
//        clientId = BuildConfig.PLAID_CLIENT_ID,
//        secret = BuildConfig.PLAID_SECRET,
//        clientName = BuildConfig.PLAID_CLIENT_NAME
//    )

}
