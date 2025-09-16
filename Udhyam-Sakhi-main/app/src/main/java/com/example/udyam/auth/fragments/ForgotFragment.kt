package com.example.udyam.auth.fragments

import android.app.ProgressDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.findNavController
import com.example.udyam.R
import com.example.udyam.databinding.FragmentForgotBinding
import com.google.firebase.auth.FirebaseAuth

/**
 * A fragment that handles the password reset process.
 *
 * This fragment provides a form for users to enter their email address
 * and request a password reset email.
 */
class ForgotFragment : Fragment() {

    private lateinit var binding :FragmentForgotBinding

    private val forgotAuth = FirebaseAuth.getInstance()
    private lateinit var forgotPD : ProgressDialog


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        forgotPD = ProgressDialog(activity)

    }

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
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_forgot, container, false)
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
        binding.btnForgotPasswordBack.setOnClickListener {
            findNavController().navigate(R.id.action_forgotFragment_to_loginFragment)
        }

        binding.btnResetPassword.setOnClickListener{


            val email = binding.edtForgotPasswordEmail.text.toString()
            if(email.isNotEmpty()){
                sendTheUserResetEmail(email)
            }
            else{
                Toast.makeText(activity , "Please Enter a Verified Email" , Toast.LENGTH_SHORT).show()
            }

        }
    }

    /**
     * Sends a password reset email to the user.
     *
     * @param email The user's email address.
     */
    private fun sendTheUserResetEmail(email:String?) {
        forgotPD.show()
        forgotPD.setMessage("Please wait")
        forgotAuth.sendPasswordResetEmail(email!!).addOnSuccessListener {
            Toast.makeText(activity , " Please check your email" , Toast.LENGTH_SHORT).show()
            forgotPD.dismiss()

        }.addOnFailureListener{
            Toast.makeText(activity , "Something went wrong , Exception -> ${it.toString()}"
                , Toast.LENGTH_SHORT).show()
            forgotPD.dismiss()

        }
    }

    }



