package com.clevmania.appruvecam.di

import com.clevmania.appruvecam.AppruveApiService
import com.clevmania.appruvecam.DocumentDataSource
import com.clevmania.appruvecam.DocumentRepository
import com.clevmania.appruvecam.UploadDocumentService
import com.clevmania.appruvecam.app.AppruveCamApp
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import javax.inject.Singleton

/**
 * @author by Lawrence on 5/9/20.
 * for AppruveCam
 */

@Module(includes = [ViewModelModule::class])
class AppModule(val app : AppruveCamApp){

    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()

    @Provides
    fun provideOkHttpClient(interceptor: HttpLoggingInterceptor): OkHttpClient =
        OkHttpClient.Builder().addInterceptor(interceptor).build()

    @Provides
    fun provideLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

    @Provides
    @Singleton
    fun provideRetrofitBuilder(): Retrofit {
        return AppruveApiService()
    }

    @Provides
    fun provideDocumentDataSource(retrofit: Retrofit): DocumentDataSource {
        val service = retrofit.create(UploadDocumentService::class.java)
        return DocumentRepository(service)
    }
}