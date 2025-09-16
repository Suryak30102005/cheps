package com.example.udyam.auth.fragments

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.findNavController
import com.example.udyam.MainActivity
import com.example.udyam.R
import com.example.udyam.Seller.SellerHomeActivity
import com.example.udyam.buyerHome.BuyerHomeActivity

import com.example.udyam.databinding.FragmentLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.firestore.FirebaseFirestore

/**
 * A fragment that handles user login.
 *
 * This fragment provides a form for users to enter their email and password to log in.
 * It also handles navigation to the signup and password reset screens.
 * After successful login, it redirects the user to the appropriate home screen
 * based on their role (buyer or seller).
 */
class LoginFragment : Fragment() {

    private lateinit var binding: FragmentLoginBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private lateinit var progressDialogLogin: ProgressDialog

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
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_login, container, false)
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

        // Handle already logged-in user
        if (auth.currentUser != null) {
            val uid = auth.currentUser!!.uid
            db.collection("users").document(uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val role = document.getString("role")
                        when (role) {
                            "seller" -> {
                                startActivity(Intent(activity, SellerHomeActivity::class.java))
                                requireActivity().finish()
                            }
                            "buyer" -> {
                                startActivity(Intent(activity, BuyerHomeActivity::class.java))
                                requireActivity().finish()
                            }
                            else -> {
                                Toast.makeText(activity, "User role not recognized", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        Toast.makeText(activity, "User data not found", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(activity, "Failed to fetch user data: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        // MOVE THESE OUTSIDE THE IF BLOCK
        progressDialogLogin = ProgressDialog(activity)

        binding.signInTextToSignUp.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_signupFragment)
        }

        binding.forgotPass.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_forgotFragment)
        }

        binding.loginButton.setOnClickListener {
            val email = binding.loginetemail.text.toString()
            val password = binding.loginetpassword.text.toString()

            if (email.isEmpty()) {
                Toast.makeText(activity, "Email can't be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                Toast.makeText(activity, "Password can't be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            getTheUserSignedIn(email, password)
        }
    }

    /**
     * Signs in the user with the given email and password.
     *
     * @param email The user's email address.
     * @param password The user's password.
     */
    private fun getTheUserSignedIn(email: String, password: String) {
                progressDialogLogin.show()
                progressDialogLogin.setMessage("Signing in")

                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        progressDialogLogin.dismiss()

                        if (task.isSuccessful) {
                            val user = auth.currentUser

                            if (user != null && user.isEmailVerified) {
                                val uid = user.uid

                                db.collection("users").document(uid).get()
                                    .addOnSuccessListener { document ->
                                        if (document.exists()) {
                                            val role = document.getString("role")?.lowercase()

                                            when (role) {
                                                "buyer" -> {
                                                    startActivity(
                                                        Intent(
                                                            activity,
                                                            BuyerHomeActivity::class.java
                                                        )
                                                    )
                                                    activity?.finish()
                                                }

                                                "seller" -> {
                                                    startActivity(
                                                        Intent(
                                                            activity,
                                                            SellerHomeActivity::class.java
                                                        )
                                                    )
                                                    activity?.finish()
                                                }

                                                else -> {
                                                    startActivity(
                                                        Intent(
                                                            activity,
                                                            MainActivity::class.java
                                                        )
                                                    )
                                                    activity?.finish()
                                                }
                                            }
                                        } else {
                                            Toast.makeText(
                                                activity,
                                                "User data not found",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(
                                            activity,
                                            "Failed to fetch user role",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                            } else {
                                Toast.makeText(
                                    activity,
                                    "Please verify your email",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } else {
                            Toast.makeText(activity, "Invalid Credentials", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                    .addOnFailureListener { exception ->
                        progressDialogLogin.dismiss()
                        if (exception is FirebaseAuthInvalidCredentialsException) {
                            Toast.makeText(activity, "Invalid Credentials", Toast.LENGTH_SHORT)
                                .show()
                        } else {
                            Toast.makeText(activity, "Authorization Failed", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
            }


        }