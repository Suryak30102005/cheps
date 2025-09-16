package com.example.udyam.Seller.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.example.udyam.R
import com.example.udyam.Seller.AddProductActivity
import com.example.udyam.adapters.ProductAdapter
import com.example.udyam.databinding.FragmentSellerProductsBinding
import com.example.udyam.models.Product
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * A fragment that displays the products of the currently logged-in seller.
 *
 * This fragment fetches the seller's products from a 'myProducts' sub-collection
 * in Firestore and displays them in a RecyclerView. It also provides a floating
 * action button to add new products.
 */
class SellerProductsFragment : Fragment() {

    private lateinit var binding: FragmentSellerProductsBinding
    private lateinit var adapter: ProductAdapter
    private val productList = ArrayList<Product>()

    private val db by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

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
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_seller_products, container, false)
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

        // Pass a click listener, even if just a simple toast or placeholder
        adapter = ProductAdapter(productList) { clickedProduct ->
            Toast.makeText(requireContext(), "Clicked: ${clickedProduct.name}", Toast.LENGTH_SHORT).show()
            // Optional: Navigate to a detail/edit screen here if needed
        }

        binding.productsRv.layoutManager = GridLayoutManager(requireContext(), 1)
        binding.productsRv.adapter = adapter

        fetchMyProducts()

        binding.addProductFab.setOnClickListener {
            val intent = Intent(activity, AddProductActivity::class.java)
            startActivity(intent)
            requireActivity().finish()
        }
    }

    /**
     * Fetches the seller's products from Firestore and updates the RecyclerView.
     */
    private fun fetchMyProducts() {
        val currentUser = auth.currentUser ?: return
        db.collection("users")
            .document(currentUser.uid)
            .collection("myProducts")
            .get()
            .addOnSuccessListener { querySnapshot ->
                productList.clear()
                for (doc in querySnapshot.documents) {
                    val product = doc.toObject(Product::class.java)
                    product?.let { productList.add(it) }
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load products", Toast.LENGTH_SHORT).show()
            }
    }
}
