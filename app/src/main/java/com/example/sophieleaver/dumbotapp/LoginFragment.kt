package com.example.sophieleaver.dumbotapp

import android.annotation.TargetApi
import android.app.AlertDialog
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Build
import android.os.Bundle
import android.support.v4.app.Fragment
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.fragment_login.view.*
import kotlinx.android.synthetic.main.view_register_account.view.*
import org.jetbrains.anko.find
import org.jetbrains.anko.toast

class LoginFragment : Fragment(), View.OnClickListener  {

    lateinit var auth: FirebaseAuth
    val ref = FirebaseDatabase.getInstance().reference

    private var emailForm: EditText? = null
    private var passwordForm: EditText? = null
    private var registerEmailForm : EditText? = null
    private var registerPasswordForm : EditText? = null
    private var confirmPasswordForm : EditText? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_login, container, false)

        view.button_login.setOnClickListener(this)
        view.button_register.setOnClickListener(this)

        return view
    }

    override fun onClick(v: View?) {
        when (v?.id){

            R.id.button_login -> {
                emailForm = view!!.findViewById(R.id.editText_email)
                passwordForm = view!!.findViewById(R.id.editText_password)

                //sign user in with provided credentials
                signIn(emailForm!!.text.toString(), passwordForm!!.text.toString())
            }

            R.id.button_register -> {
                //inflate the view to register a new account
                val builder = AlertDialog.Builder(context)
                val registerView = layoutInflater.inflate(R.layout.view_register_account, null)
                builder.setView(registerView)

                val dialog = builder.create()
                dialog.show()

                registerEmailForm = registerView!!.findViewById(R.id.text_register_email)
                registerPasswordForm = registerView!!.findViewById(R.id.text_register_password)
                confirmPasswordForm = registerView!!.findViewById(R.id.text_confirm_password)

                //cancel making a new account
                registerView.button_register_cancelled.setOnClickListener {dialog.cancel()}

                //register
                registerView.button_create_new_account.setOnClickListener {
                    createAccount(
//                        "email@test.com", "password"
                        registerEmailForm!!.text.toString(),
                        registerPasswordForm!!.text.toString(),
                        dialog
                    )

                }
            }
        }
    }

    private fun validateFormLogin(): Boolean {
        var valid = true

        //email cannot be empty
        val email = emailForm!!.text.toString()
        if (TextUtils.isEmpty(email)) {
            emailForm!!.error = "Required."
            valid = false
        } else {
            emailForm!!.error = null
        }

        //password cannot be empty
        val password = passwordForm!!.text.toString()
        if (TextUtils.isEmpty(password)) {
            passwordForm!!.error = "Required."
            valid = false
        }
        else {
            passwordForm!!.error = null
        }

        return valid
    }

    private fun signIn(email: String, password: String) {
        Log.d(tag, "signIn:$email")
        if (!validateFormLogin()) {
            return
        }

        // sign user in with email and password provided
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {

                    // Sign in success, update UI with the signed-in user's information
                    Log.d(tag, "signInWithEmail:success")
                    val user = auth.currentUser
                    requireActivity().toast("Log In is Successful")

                    emailForm!!.text.clear()
                    passwordForm!!.text.clear()

                    updateUI("MANAGER")

                } else {

                    // If sign in fails, display a message to the user.
                    Log.w(tag, "signInWithEmail:failure", task.exception)
                    Toast.makeText(context, "Email or password incorrect, please re-enter",
                        Toast.LENGTH_SHORT).show()

                    updateUI(null)
                }
            }
    }

    private fun validateFormRegister(): Boolean {
        var valid = true
        val email = registerEmailForm!!.text.toString()
        if (TextUtils.isEmpty(email)) {
            registerEmailForm!!.error = "Required."
            valid = false
        } else {
            registerEmailForm!!.error = null
        }

        val password = registerPasswordForm!!.text.toString()
        val confirmPassword = confirmPasswordForm!!.text.toString()

        if (TextUtils.isEmpty(password)) { //check password form not empty
            registerPasswordForm!!.error = "Required."
            valid = false
        }
        else if (TextUtils.isEmpty(confirmPassword)){ //check second password form not empty
            confirmPasswordForm!!.error = "Required."
        }
        else if(password != confirmPassword){ //check the two passwords match
            confirmPasswordForm!!.error = "Passwords Do Not Match."
        }
        else{
            registerPasswordForm!!.error = null
        }


        return valid
    }

    private fun createAccount(email: String, password: String, dialog : AlertDialog) {
        Log.d(tag, "createAccount:$email")

        if (!validateFormRegister()) { //checks email, authentication number, and password have been entered and are of correct form
            return
        }

        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(requireActivity()) { task ->
            if (task.isSuccessful) {

                //create new database entry for user
                updateUI("MANAGER")
                dialog.cancel()
            } else {
                // If sign in fails, display a message to the user.
                Log.w(tag, "createUserWithEmail:failure", task.exception)
                Toast.makeText(context, "Invalid email or password", Toast.LENGTH_SHORT).show()
                //updateUI(null)

            }

        }
    }



    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private fun updateUI(mode : String?) {
        if (mode != null) {
            (activity as MainActivity).changeMode(mode)
            (activity as MainActivity).showOrderFragment()
        }

    }

    companion object {
        @JvmStatic
        fun newInstance() = LoginFragment()
    }

}
