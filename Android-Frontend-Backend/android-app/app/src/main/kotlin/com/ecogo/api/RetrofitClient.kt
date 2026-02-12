package com.ecogo.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Retrofit client singleton
 */
object RetrofitClient {

    /**
     * OkHttp client
     * Optimized: logging enabled only in Debug mode, using BASIC level for better performance
     */
    private val okHttpClient: OkHttpClient by lazy {
        val builder = OkHttpClient.Builder()
            .connectTimeout(ApiConfig.CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(ApiConfig.READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(ApiConfig.WRITE_TIMEOUT, TimeUnit.SECONDS)
        
        // Add logging interceptor (development environment)
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY // Changed to BODY to see full response/request details for debugging
        }
        builder.addInterceptor(loggingInterceptor)

        // Add authentication interceptor
        builder.addInterceptor { chain ->
            val original = chain.request()
            val requestBuilder = original.newBuilder()
            
            // Auto-inject Token
            val token = com.ecogo.auth.TokenManager.getToken()
            if (!token.isNullOrEmpty()) {
                requestBuilder.header("Authorization", "Bearer $token")
            }
            
            val request = requestBuilder.build()

            // Execute request
            val response = chain.proceed(request)
            
            // Check for 401 Unauthorized
            if (response.code == 401) {
                // Token is invalid/expired
                // Clear token and logout
                com.ecogo.auth.TokenManager.logout()
            }
            
            response
        }
        
        builder.build()
    }
    
    /**
     * Retrofit instance
     */
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(ApiConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    /**
     * API service instance
     */
    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
    val badgeApiService: BadgeApiService by lazy {
        retrofit.create(BadgeApiService::class.java)
    }
}
