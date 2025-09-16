package com.example.udyam.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.udyam.R
import com.example.udyam.models.Product

/**
 * A RecyclerView adapter for displaying a list of products.
 *
 * @param productList The list of [Product] objects to display.
 * @param onItemClick A lambda function to be invoked when an item is clicked.
 */
class ProductAdapter(
    private val productList: List<Product>,
    private val onItemClick: (Product) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    /**
     * A ViewHolder that holds the views for a product item.
     *
     * @param itemView The view for the product item.
     */
    inner class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgProduct: ImageView = itemView.findViewById(R.id.imgExploreProduct)
        val tvName: TextView = itemView.findViewById(R.id.tvExploreProductName)
        val tvPrice: TextView = itemView.findViewById(R.id.tvExploreProductPrice)
    }

    /**
     * Called when RecyclerView needs a new [ProductViewHolder] of the given type to represent
     * an item.
     *
     * @param parent The ViewGroup into which the new View will be added after it is bound to
     * an adapter position.
     * @param viewType The view type of the new View.
     *
     * @return A new ProductViewHolder that holds a View of the given view type.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_producy, parent, false)
        return ProductViewHolder(view)
    }

    /**
     * Called by RecyclerView to display the data at the specified position.
     *
     * @param holder The ViewHolder which should be updated to represent the contents of the
     * item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = productList[position]
        holder.tvName.text = product.name
        holder.tvPrice.text = "₹${product.price}"

        Glide.with(holder.itemView.context)
            .load(product.imageUrl)
            .placeholder(R.drawable.loginimage)
            .into(holder.imgProduct)

        holder.itemView.setOnClickListener {
            onItemClick(product)
        }
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     *
     * @return The total number of items in this adapter.
     */
    override fun getItemCount(): Int = productList.size
}
