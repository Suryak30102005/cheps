package com.example.udyam.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.udyam.R
import com.example.udyam.models.OrderNotification

/**
 * A RecyclerView adapter for displaying a list of order notifications.
 *
 * @param orderList The list of [OrderNotification] objects to display.
 */
class OrderNotificationAdapter(
    private var orderList: List<OrderNotification>
) : RecyclerView.Adapter<OrderNotificationAdapter.OrderViewHolder>() {

    /**
     * A ViewHolder that holds the views for an order notification item.
     *
     * @param itemView The view for the order notification item.
     */
    inner class OrderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val status = itemView.findViewById<TextView>(R.id.orderStatus)
        val itemName = itemView.findViewById<TextView>(R.id.orderItemName)
        val amount = itemView.findViewById<TextView>(R.id.orderAmount)
        val address = itemView.findViewById<TextView>(R.id.orderAddress)
        val username = itemView.findViewById<TextView>(R.id.orderUser)
    }

    /**
     * Called when RecyclerView needs a new [OrderViewHolder] of the given type to represent
     * an item.
     *
     * @param parent The ViewGroup into which the new View will be added after it is bound to
     * an adapter position.
     * @param viewType The view type of the new View.
     *
     * @return A new OrderViewHolder that holds a View of the given view type.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_order_notification, parent, false)
        return OrderViewHolder(view)
    }

    /**
     * Called by RecyclerView to display the data at the specified position.
     *
     * @param holder The ViewHolder which should be updated to represent the contents of the
     * item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val order = orderList[position]
        holder.status.text = "Ordered on ${order.timestamp}"
        holder.itemName.text = "Item: ${order.itemName}"
        holder.amount.text = "Amount: ${order.amount}"
        holder.address.text = "Address: ${order.address}"
        holder.username.text = "Ordered by: ${order.username}"
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     *
     * @return The total number of items in this adapter.
     */
    override fun getItemCount() = orderList.size

    /**
     * Updates the data set of the adapter and notifies the RecyclerView of the change.
     *
     * @param newList The new list of [OrderNotification] objects.
     */
    fun updateData(newList: List<OrderNotification>) {
        orderList = newList
        notifyDataSetChanged()
    }
}
