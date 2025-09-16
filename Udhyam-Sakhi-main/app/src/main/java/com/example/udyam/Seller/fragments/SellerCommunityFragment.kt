package com.example.udyam.Seller.fragments
import UThread
import UThreadAdapter
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.udyam.R
import com.example.udyam.Seller.AddThreadActivity
import com.example.udyam.databinding.FragmentSellerCommunityBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A fragment that displays the seller community feed.
 *
 * This fragment shows a list of threads posted by sellers, ordered by timestamp.
 * It uses a snapshot listener to get real-time updates from Firestore.
 * A floating action button allows sellers to add new threads.
 */
class SellerCommunityFragment : Fragment() {

    private lateinit var binding: FragmentSellerCommunityBinding
    private lateinit var adapter: UThreadAdapter
    private val threadList = mutableListOf<UThread>()
    private val db = FirebaseFirestore.getInstance()

    /**
     * Called to have the fragment instantiate its user interface view.
     *
     * @param inflater The LayoutInflater object that can be used to inflate
     * any views in the fragment,
     * @param container If non-null, this is the parent view that the fragment's
     * UI should be attached to. The fragment should not add the view itself,
     * but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     *
     * @return Return the View for the fragment's UI, or null.
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_seller_community, container, false)
        return binding.root
    }

    /**
     * Called immediately after [.onCreateView] has returned, but before any
     * saved state has been restored in to the view.
     *
     * @param view The View returned by [.onCreateView].
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup RecyclerView
        adapter = UThreadAdapter(threadList)
        binding.updatesRv.layoutManager = LinearLayoutManager(requireContext())
        binding.updatesRv.adapter = adapter

        // FAB to add new thread
        binding.fabAdd.setOnClickListener {
            val intent = Intent(requireContext(), AddThreadActivity::class.java)
            startActivity(intent)
        }

        // Fetch threads
        fetchThreads()
    }



    /**
     * Fetches threads from Firestore in real-time and updates the RecyclerView.
     */
    private fun fetchThreads() {
        db.collection("threads")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener

                threadList.clear()
                snapshot?.forEach { doc ->
                    val thread = doc.toObject(UThread::class.java)

                    // Safely read timestamp as Long
                    val timestampLong = doc.getLong("timestamp") // <-- handles Firestore Long

                    val formattedTime = timestampLong?.let {
                        val date = Date(it) // if it's in milliseconds
                        // If your value is in seconds, use: Date(it * 1000)
                        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(date)
                    } ?: ""

//                    thread.timestamp = formattedTime
                    threadList.add(thread)
                }
                adapter.notifyDataSetChanged()
            }
    }


}
