package com.example.udyam.utils

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.udyam.models.OrderNotification

/**
 * A singleton object that manages the history of order notifications.
 *
 * This manager stores order notifications in a LiveData object, allowing other
 * parts of the app to observe changes to the order history. It also prevents
 * duplicate orders from being added by tracking seen payment IDs.
 */
object OrderHistoryManager {
    private val _orderHistory = MutableLiveData<MutableList<OrderNotification>>(mutableListOf())

    /**
     * A LiveData object that holds the list of order notifications.
     */
    val orderHistory: LiveData<MutableList<OrderNotification>> = _orderHistory

    private val seenPaymentIds = mutableSetOf<String>()

    /**
     * Adds an order to the history if it has not been seen before.
     *
     * @param order The [OrderNotification] to add.
     * @param paymentId The payment ID of the order, used to prevent duplicates.
     */
    fun addOrder(order: OrderNotification, paymentId: String) {
        if (!seenPaymentIds.contains(paymentId)) {
            seenPaymentIds.add(paymentId)
            val current = _orderHistory.value ?: mutableListOf()
            current.add(0, order) // add latest at top
            _orderHistory.postValue(current)
        }
    }
}
