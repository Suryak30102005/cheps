import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.udyam.databinding.ItemPostsBinding

/**
 * A RecyclerView adapter for displaying a list of threads.
 *
 * @param threadList The list of [UThread] objects to display.
 */
class UThreadAdapter(
    private val threadList: List<UThread>
) : RecyclerView.Adapter<UThreadAdapter.UThreadViewHolder>() {

    /**
     * A ViewHolder that holds the views for a thread item.
     *
     * @param binding The view binding for the thread item layout.
     */
    inner class UThreadViewHolder(val binding: ItemPostsBinding) :
        RecyclerView.ViewHolder(binding.root)

    /**
     * Called when RecyclerView needs a new [UThreadViewHolder] of the given type to represent
     * an item.
     *
     * @param parent The ViewGroup into which the new View will be added after it is bound to
     * an adapter position.
     * @param viewType The view type of the new View.
     *
     * @return A new UThreadViewHolder that holds a View of the given view type.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UThreadViewHolder {
        val binding = ItemPostsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UThreadViewHolder(binding)
    }

    /**
     * Called by RecyclerView to display the data at the specified position.
     *
     * @param holder The ViewHolder which should be updated to represent the contents of the
     * item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    override fun onBindViewHolder(holder: UThreadViewHolder, position: Int) {
        val uThread = threadList[position]
        val b = holder.binding

        // Use itemView context for Glide to avoid UnsupportedOperationException
        Glide.with(holder.itemView)
            .load(uThread.userImageUrl)
            .into(b.dp)

        b.namePerson.text = uThread.userName
//        b.time.text = uThread.timestamp
        b.textTitle.text = uThread.title
        b.description.text = uThread.description
        b.viewsCount.text = uThread.viewCount.toString()
        b.commentsCount.text = uThread.commentCount.toString()

        Glide.with(holder.itemView)
            .load(uThread.imageUrl)
            .into(b.threadImg)

        // Click listeners (optional)
        b.like.setOnClickListener {
            // Handle like
        }

        b.sharing.setOnClickListener {
            // Handle share
        }

        b.commenting.setOnClickListener {
            // Handle comment
        }
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     *
     * @return The total number of items in this adapter.
     */
    override fun getItemCount(): Int = threadList.size
}
