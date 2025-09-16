package com.example.udyam.Seller

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.udyam.databinding.ActivityAddThreadBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

/**
 * An activity for sellers to create and post new threads.
 *
 * This activity allows a seller to write a title and description, attach an image,
 * and post it as a new thread. The image is uploaded to ImgBB, and the thread
 * data, including the image URL and user information, is saved to Firestore.
 */
class AddThreadActivity : AppCompatActivity() {
    private lateinit var progressDialog: ProgressDialog
    private lateinit var binding: ActivityAddThreadBinding
    private val PICK_IMAGE_REQUEST = 1
    private var selectedImageUri: Uri? = null

    private val apiKey = "a55a29c04fe50e932764f6e4a1ad0361" // Replace with your ImgBB API key
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

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
        binding = ActivityAddThreadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.imgCard.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            startActivityForResult(Intent.createChooser(intent, "Select Thread Image"), PICK_IMAGE_REQUEST)
        }

        binding.btnSubmit.setOnClickListener {
            if (selectedImageUri != null) {
                uploadImageToImgBB(selectedImageUri!!)
            } else {
                Toast.makeText(this, "Please select an image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Handles the result from the image picker activity.
     *
     * @param requestCode The integer request code originally supplied to
     *                    startActivityForResult(), allowing you to identify who this
     *                    result came from.
     * @param resultCode The integer result code returned by the child activity
     *                   through its setResult().
     * @param data An Intent, which can return result data to the caller
     *               (various data can be attached to Intent "extras").
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data?.data != null) {
            selectedImageUri = data.data
            Glide.with(this).load(selectedImageUri).into(binding.threadImg)
        }
    }

    /**
     * Uploads the selected image to ImgBB.
     *
     * @param imageUri The URI of the image to upload.
     */
    private fun uploadImageToImgBB(imageUri: Uri) {
        val inputStream = contentResolver.openInputStream(imageUri)
        val imageBytes = inputStream?.readBytes()

        if (imageBytes == null) {
            Toast.makeText(this, "Unable to read image", Toast.LENGTH_SHORT).show()
            return
        }

        val base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
        val client = OkHttpClient()

        val requestBody = FormBody.Builder()
            .add("key", apiKey)
            .add("image", base64Image)
            .build()

        val request = Request.Builder()
            .url("https://api.imgbb.com/1/upload")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@AddThreadActivity, "Image upload failed: ${e.message}", Toast.LENGTH_LONG).show()
                    Log.e("ImgBB Upload", "Error: ", e)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                val json = JSONObject(responseBody ?: "{}")

                if (json.has("data")) {
                    val link = json.getJSONObject("data").getString("url")
                    runOnUiThread {
                        Toast.makeText(this@AddThreadActivity, "Image uploaded!", Toast.LENGTH_SHORT).show()
                        saveThreadToFirestore(link) // ✅ Call this inside UI thread
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@AddThreadActivity, "Upload failed: Unexpected response", Toast.LENGTH_LONG).show()
                        Log.e("ImgBB Upload", "Response: $json")
                    }
                }
            }
        })
    }

    /**
     * Saves the thread data to Firebase Firestore.
     *
     * @param imageUrl The URL of the uploaded image.
     */
    private fun saveThreadToFirestore(imageUrl: String) {
        val title = binding.etTitle.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()

        if (title.isBlank() || description.isBlank()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        progressDialog = ProgressDialog(this).apply {
            setMessage("Posting thread...")
            setCancelable(false)
            show()
        }

        // 🔍 Get username from Firestore
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                val username = document.getString("name") ?: "Unknown"

                val thread = hashMapOf(
                    "title" to title,
                    "description" to description,
                    "imageUrl" to imageUrl,
                    "timestamp" to System.currentTimeMillis(),
                    "createdAt" to com.google.firebase.Timestamp.now(), // 👈 Optional: Firestore native Timestamp
                    "likeCount" to 0,
                    "shareCount" to 0,
                    "userId" to userId,
                    "username" to username
                )


                db.collection("threads")
                    .add(thread)
                    .addOnSuccessListener { docRef ->
                        db.collection("users").document(userId)
                            .collection("myThreads")
                            .document(docRef.id)
                            .set(thread)
                            .addOnSuccessListener {
                                progressDialog.dismiss()
                                Toast.makeText(this, "Thread added successfully!", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this, SellerHomeActivity::class.java))
                                finish()
                            }
                            .addOnFailureListener { e ->
                                progressDialog.dismiss()
                                Toast.makeText(this, "Saved to threads but failed in myThreads", Toast.LENGTH_SHORT).show()
                                Log.e("Firestore", "myThreads error: ", e)
                            }
                    }
                    .addOnFailureListener { e ->
                        progressDialog.dismiss()
                        Toast.makeText(this, "Failed to add thread: ${e.message}", Toast.LENGTH_LONG).show()
                        Log.e("Firestore", "Error adding thread", e)
                    }
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                Toast.makeText(this, "Failed to fetch username: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e("Firestore", "Error fetching username", e)
            }
    }


}
