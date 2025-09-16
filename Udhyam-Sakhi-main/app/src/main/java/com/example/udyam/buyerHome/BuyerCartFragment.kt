package com.example.udyam.Buyer

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.udyam.R

/**
 * A fragment that displays the buyer's shopping cart.
 *
 * This fragment is currently a placeholder and only displays a layout.
 * It is intended to be developed further to show the items in the buyer's cart.
 */
class BuyerCartFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_buyer_cart, container, false)
    }

}