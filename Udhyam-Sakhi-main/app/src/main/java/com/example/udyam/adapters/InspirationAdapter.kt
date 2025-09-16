package com.example.udyam.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.udyam.R
import com.example.udyam.models.InspirationStory

/**
 * A RecyclerView adapter for displaying a list of inspirational stories.
 *
 * @param items The list of [InspirationStory] objects to display.
 */
class InspirationAdapter(
    private val items: List<InspirationStory>
) : RecyclerView.Adapter<InspirationAdapter.InspirationViewHolder>() {

    /**
     * A ViewHolder that holds the views for an inspiration story item.
     *
     * @param itemView The view for the inspiration story item.
     */
    inner class InspirationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textName: TextView = itemView.findViewById(R.id.text_name)
        val textDescription: TextView = itemView.findViewById(R.id.text_description)
        val imagePerson: ImageView = itemView.findViewById(R.id.image_person)
    }

    /**
     * Called when RecyclerView needs a new [InspirationViewHolder] of the given type to represent
     * an item.
     *
     * @param parent The ViewGroup into which the new View will be added after it is bound to
     * an adapter position.
     * @param viewType The view type of the new View.
     *
     * @return A new InspirationViewHolder that holds a View of the given view type.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InspirationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_inspiration, parent, false)
        return InspirationViewHolder(view)
    }

    /**
     * Called by RecyclerView to display the data at the specified position.
     *
     * @param holder The ViewHolder which should be updated to represent the contents of the
     * item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    override fun onBindViewHolder(holder: InspirationViewHolder, position: Int) {
        val item = items[position]
        holder.textName.text = item.name
        holder.textDescription.text = item.description
        holder.imagePerson.setImageResource(item.imageResId)
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     *
     * @return The total number of items in this adapter.
     */
    override fun getItemCount() = items.size
}
