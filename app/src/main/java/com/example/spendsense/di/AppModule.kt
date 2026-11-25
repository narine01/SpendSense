package com.example.spendsense.di

import android.content.Context
import androidx.room.Room
import com.example.spendsense.AppDatabase
import com.example.spendsense.BackendApi
import com.example.spendsense.ExpenseDao
import com.google.android.gms.tasks.Tasks
import com.google.firebase.appcheck.FirebaseAppCheck
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // Your deployed backend. For local testing use "http://127.0.0.1:8000/" with
    // `adb reverse tcp:8000 tcp:8000`.
    private const val BASE_URL = "https://spendsense-backend-hgtl.onrender.com/"

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "spendsense.db").build()

    @Provides
    fun provideExpenseDao(db: AppDatabase): ExpenseDao = db.expenseDao()

    @Provides
    @Singleton
    fun provideOkHttp(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .callTimeout(120, TimeUnit.SECONDS) // hard ceiling for one whole call, streams included
        .addInterceptor { chain ->
            val appCheckToken = try {
                Tasks.await(FirebaseAppCheck.getInstance().getAppCheckToken(false)).token
            } catch (error: Exception) {
                throw java.io.IOException("Unable to obtain Firebase App Check token", error)
            }
            chain.proceed(
                chain.request().newBuilder()
                    .header("X-Firebase-AppCheck", appCheckToken)
                    .build()
            )
        }
        .build()

    @Provides
    @Singleton
    fun provideBackendApi(client: OkHttpClient): BackendApi =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BackendApi::class.java)
}
