package com.example.udyam

import com.example.udyam.models.OrderResponse
import retrofit2.http.GET

/**
 * A Retrofit service interface for fetching order data from the backend API.
 */
interface OrderApiService {

    /**
     * Fetches the latest order from the backend.
     *
     * This is a suspending function, so it must be called from a coroutine or another
     * suspending function.
     *
     * @return An [OrderResponse] object containing the latest order details.
     */
    @GET("api/latest-order")
    suspend fun getLatestOrder(): OrderResponse
}
