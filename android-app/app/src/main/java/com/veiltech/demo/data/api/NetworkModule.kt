package com.veiltech.demo.data.api

import com.veiltech.demo.security.SecureStorage
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class JwtInterceptor(private val secureStorage: SecureStorage) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = secureStorage.token()
        val request = chain.request().newBuilder().apply {
            if (!token.isNullOrBlank()) header("Authorization", "Bearer $token")
        }.build()
        return chain.proceed(request)
    }
}

object NetworkModule {
    fun api(baseUrl: String, secureStorage: SecureStorage): ApiService {
        val client = OkHttpClient.Builder().addInterceptor(JwtInterceptor(secureStorage)).build()
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
