package com.example.udyam.Buyer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.udyam.R
import com.example.udyam.adapters.OrderNotificationAdapter
import com.example.udyam.utils.OrderHistoryManager
import com.example.udyam.viewmodels.OrderViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton

/**
 * A fragment that displays a list of order notifications for the buyer.
 *
 * This fragment observes the order history from [OrderHistoryManager] and displays
 * it in a RecyclerView. It also provides a refresh button to fetch the latest order.
 */
class BuyerNotifFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: OrderNotificationAdapter
    private lateinit var fabRefresh: FloatingActionButton
    private val orderViewModel: OrderViewModel by viewModels()

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
    ): View? {
        val view = inflater.inflate(R.layout.fragment_buyer_notif, container, false)

        recyclerView = view.findViewById(R.id.recyclerViewOrders)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = OrderNotificationAdapter(emptyList())
        recyclerView.adapter = adapter

        fabRefresh = view.findViewById(R.id.fabRefresh)
        fabRefresh.setOnClickListener {
            orderViewModel.fetchLatestOrderOnce()
        }

        OrderHistoryManager.orderHistory.observe(viewLifecycleOwner, Observer { history ->
            adapter.updateData(history)
        })

        return view
    }
}
