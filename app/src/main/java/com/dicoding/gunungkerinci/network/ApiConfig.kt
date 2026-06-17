package com.dicoding.gunungkerinci.network

import android.content.Context
import com.dicoding.gunungkerinci.pref.UserPreference
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiConfig {

    private const val BASE_URL = "http://127.0.0.1:8000/"
    //private const val BASE_URL = "https://tnks.mukhtada.my.id/"

    fun getApiService(context: Context): ApiService {
        val logging = HttpLoggingInterceptor()
        logging.level = HttpLoggingInterceptor.Level.BODY

        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val originalRequest = chain.request()
                val token = UserPreference(context).getToken()

                // Skip if request already has Authorization header or token is empty
                val request = if (!token.isNullOrEmpty() && originalRequest.header("Authorization") == null) {
                    originalRequest.newBuilder()
                        .addHeader("Authorization", "Bearer $token")
                        .build()
                } else {
                    originalRequest
                }

                chain.proceed(request)
            }
            .addInterceptor(logging)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()

        return retrofit.create(ApiService::class.java)
    }
}
