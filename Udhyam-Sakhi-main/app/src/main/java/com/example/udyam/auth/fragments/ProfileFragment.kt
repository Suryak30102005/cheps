package com.example.udyam.auth.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.udyam.R
import com.example.udyam.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * A fragment where users complete their profile information during signup.
 *
 * This fragment collects additional user details like address, pincode, contact number, and role.
 * It then creates a new user in Firebase Authentication and saves the complete profile
 * to Firestore.
 */
class ProfileFragment : Fragment() {

    private lateinit var binding: FragmentProfileBinding
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

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
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_profile, container, false)
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

        val args = ProfileFragmentArgs.fromBundle(requireArguments())
        val emailFromSignup = args.userEmail
        val passwordFromSignup = args.userPassword
        val nameFromSignup = args.userName

        binding.nameEt.setText(nameFromSignup)

        binding.saveBtn.setOnClickListener {
            val name = binding.nameEt.text.toString().trim()
            val address = binding.address.text.toString().trim()
            val pincode = binding.pincode.text.toString().trim()
            val contact = binding.contact.text.toString().trim()
            val role = binding.roleSpinner.selectedItem.toString()

            when {
                name.isEmpty() -> toast("Name can't be empty")
                address.isEmpty() -> toast("Please fill the address")
                pincode.isEmpty() -> toast("Please fill the pincode")
                contact.isEmpty() -> toast("Please fill the contact number")
                !contact.matches(Regex("^[6-9][0-9]{9}$")) ->
                    toast("Enter a valid 10-digit contact number starting with 6-9")
                role == "You are" -> toast("Please select a valid role")
                else -> {
                    registerUser(
                        name,
                        address,
                        pincode,
                        contact,
                        role,
                        emailFromSignup,
                        passwordFromSignup
                    )
                }
            }
        }
    }

    /**
     * Registers a new user with Firebase Authentication and saves their profile to Firestore.
     *
     * @param name The user's name.
     * @param address The user's address.
     * @param pincode The user's pincode.
     * @param contact The user's contact number.
     * @param role The user's role (buyer or seller).
     * @param email The user's email address.
     * @param password The user's password.
     */
    private fun registerUser(
        name: String, address: String, pincode: String,
        contact: String, role: String,
        email: String, password: String
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val user = authResult.user
                val uid = user?.uid

                if (uid != null) {
                    user.sendEmailVerification()
                        .addOnSuccessListener {
                            toast("Verification email sent!")

                            val userMap = hashMapOf(
                                "uid" to uid,
                                "name" to name,
                                "email" to email,
                                "address" to address,
                                "pincode" to pincode,
                                "contact" to contact,
                                "role" to role
                            )

                            firestore.collection("users").document(uid)
                                .set(userMap)
                                .addOnSuccessListener {
                                    toast("User profile saved successfully!")
                                    findNavController().navigate(R.id.action_profileFragment_to_loginFragment)
                                }
                                .addOnFailureListener {
                                    toast("Firestore error: ${it.message}")
                                }

                        }
                        .addOnFailureListener {
                            toast("Failed to send verification email: ${it.message}")
                        }
                } else {
                    toast("User UID is null!")
                }

            }
            .addOnFailureListener {
                toast("Signup failed: ${it.message}")
            }
    }

    /**
     * Shows a toast message.
     *
     * @param msg The message to show.
     */
    private fun toast(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
    }
}
