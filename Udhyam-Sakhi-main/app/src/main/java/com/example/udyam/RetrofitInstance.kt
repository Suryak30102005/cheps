package com.example.udyam

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * A singleton object that provides a Retrofit instance for making API calls.
 *
 * This object configures a Retrofit client with a base URL and a Gson converter factory.
 * The `api` property provides a lazy-initialized implementation of the [OrderApiService] interface.
 */
object RetrofitInstance {
    private const val BASE_URL = "https://397c-103-217-237-56.ngrok-free.app.app/"

    /**
     * A lazy-initialized Retrofit service for the [OrderApiService].
     */
    val api: OrderApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OrderApiService::class.java)
    }
}
