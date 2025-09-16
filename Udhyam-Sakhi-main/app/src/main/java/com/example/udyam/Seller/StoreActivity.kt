package com.example.udyam.Seller

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.example.udyam.R
import com.example.udyam.databinding.ActivityStoreBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * An activity where sellers can manage their store details.
 *
 * This activity allows sellers to view and update their store information, such as
 * the store name, address, pincode, and contact details. The data is fetched from
 * and saved to Firebase Firestore.
 */
class StoreActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStoreBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    /**
     * Called when the activity is first created.
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in [onSaveInstanceState].
     *     Otherwise it is null.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = DataBindingUtil.setContentView(this, R.layout.activity_store)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // ✅ Fetch existing store data
        fetchExistingStoreData()

        binding.saveBtn.setOnClickListener {
            saveStoreDetails()
        }
    }

    /**
     * Fetches the existing store data from Firestore and populates the UI fields.
     */
    private fun fetchExistingStoreData() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            firestore.collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.contains("store")) {
                        val store = document.get("store") as Map<*, *>

                        binding.storeNameEt.setText(store["storeName"] as? String ?: "")
                        binding.stLocationEt.setText(store["address"] as? String ?: "")
                        binding.pincodeEt.setText(store["pincode"] as? String ?: "")
                        binding.contact.setText(store["contact"] as? String ?: "")
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to load store info: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    /**
     * Saves the store details entered by the user to Firestore.
     *
     * The data is saved in two locations: under the user's document and in a
     * global 'stores' collection.
     */
    private fun saveStoreDetails() {
        val storeName = binding.storeNameEt.text.toString().trim()
        val address = binding.stLocationEt.text.toString().trim()
        val pincode = binding.pincodeEt.text.toString().trim()
        val contact = binding.contact.text.toString().trim()

        if (storeName.isEmpty() || address.isEmpty() || pincode.isEmpty() || contact.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = auth.currentUser?.uid
        if (userId != null) {
            val storeData = hashMapOf(
                "storeName" to storeName,
                "address" to address,
                "pincode" to pincode,
                "contact" to contact,
                "imageUrl" to "https://www.google.com/imgres?q=women%20store%20small%20business%20decoration%20items%20in%20dharamshala&imgurl=https%3A%2F%2Flookaside.instagram.com%2Fseo%2Fgoogle_widget%2Fcrawler%2F%3Fmedia_id%3D2938841593366418602&imgrefurl=https%3A%2F%2Fwww.instagram.com%2Fthecashmirstag%2Fp%2FCjI3N0oPNxc%2F&docid=E--UimdKO3zaUM&tbnid=S5HHDeFS4WyNoM&vet=12ahUKEwjU0tzFgs-MAxUUe2wGHdBdBggQM3oECFAQAA..i&w=1440&h=1800&hcb=2&ved=2ahUKEwjU0tzFgs-MAxUUe2wGHdBdBggQM3oECFAQAA",
                "userId" to userId
            )

            val storeMap = hashMapOf(
                "store" to storeData
            )

            // Save in user's document
            firestore.collection("users").document(userId)
                .set(storeMap, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener {
                    // Save in global stores collection
                    firestore.collection("stores").document(userId)
                        .set(storeData)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Store details saved successfully!", Toast.LENGTH_SHORT).show()

                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Failed to save store globally: ${it.message}", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error saving user store: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
        }
    }
}
