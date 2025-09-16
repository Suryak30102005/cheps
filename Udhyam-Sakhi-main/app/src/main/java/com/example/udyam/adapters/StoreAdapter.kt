package com.example.udyam.Buyer

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.udyam.R
import com.example.udyam.buyerHome.StoresProductActivity
import com.example.udyam.models.StoreModel

/**
 * A RecyclerView adapter for displaying a list of stores.
 *
 * @param storeList The list of [StoreModel] objects to display.
 */
class StoreAdapter(private val storeList: List<StoreModel>) :
    RecyclerView.Adapter<StoreAdapter.StoreViewHolder>() {

    /**
     * A ViewHolder that holds the views for a store item.
     *
     * @param itemView The view for the store item.
     */
    class StoreViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val storeImage: ImageView = itemView.findViewById(R.id.imgStore)
        val storeName: TextView = itemView.findViewById(R.id.tvStoreName)
        val storeLocation: TextView = itemView.findViewById(R.id.tvStoreLocation)
    }

    /**
     * Called when RecyclerView needs a new [StoreViewHolder] of the given type to represent
     * an item.
     *
     * @param parent The ViewGroup into which the new View will be added after it is bound to
     * an adapter position.
     * @param viewType The view type of the new View.
     *
     * @return A new StoreViewHolder that holds a View of the given view type.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoreViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_store_card, parent, false)
        return StoreViewHolder(view)
    }

    /**
     * Called by RecyclerView to display the data at the specified position.
     *
     * @param holder The ViewHolder which should be updated to represent the contents of the
     * item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    override fun onBindViewHolder(holder: StoreViewHolder, position: Int) {
        val store = storeList[position]
        holder.storeName.text = store.storeName
        holder.storeLocation.text = store.location

        Glide.with(holder.itemView.context)
            .load(store.imageResId)
            .placeholder(R.drawable.handmade1) // 👈 your placeholder image here
            .into(holder.storeImage)


        // On click, open StoresProductActivity and pass the UID
        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, StoresProductActivity::class.java)
            intent.putExtra("sellerUid", store.uid)
            context.startActivity(intent)
        }
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     *
     * @return The total number of items in this adapter.
     */
    override fun getItemCount(): Int = storeList.size
}
