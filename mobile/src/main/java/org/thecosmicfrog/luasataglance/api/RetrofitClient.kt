package org.thecosmicfrog.luasataglance.api

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "https://api.thecosmicfrog.org/cgi-bin/"

    private val client = OkHttpClient.Builder()
        .addInterceptor(HttpInterceptor())
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val apiMethods: ApiMethods = retrofit.create(ApiMethods::class.java)
}
