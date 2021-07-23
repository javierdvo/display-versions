package com.javierdvo.link

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.autofill.AutofillManager
import androidx.appcompat.app.AppCompatActivity
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth


class FirebaseUIActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_firebase_ui)

        themeAndLogo()

    }


    // [START auth_fui_result]
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val response = IdpResponse.fromResultIntent(data)

            if (resultCode == Activity.RESULT_OK) {
                // Successfully signed in
                val user = FirebaseAuth.getInstance().currentUser
                Log.d(TAG,"Signup Success")
                if (user != null) {
                    Log.d(TAG,user.uid)
                }
                setResult(RESULT_OK, data);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    val autofillManager = this.applicationContext.getSystemService(AutofillManager::class.java)
                    autofillManager.commit()
                } else {
                    TODO("VERSION.SDK_INT < O")
                }
                finish()

            } else {
                if (response != null){
                    Log.d(TAG,"Signup Error")
                    Log.d(TAG, response.error?.errorCode.toString())
                }
                else {
                    Log.d(TAG,"Flow Cancelled")
                }
                // Sign in failed. If response is null the user canceled the
                // sign-in flow using the back button. Otherwise check
                // response.getError().getErrorCode() and handle the error.
                // ...
            }
        }
    }
    // [END auth_fui_result]


    private fun delete() {
        // [START auth_fui_delete]
        AuthUI.getInstance()
            .delete(this)
            .addOnCompleteListener {
                // ...
            }
        // [END auth_fui_delete]
    }

    private fun themeAndLogo() {
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build()
        )
        startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .setLogo(R.drawable.vio_icon_small) // Set logo drawable
                .setTheme(R.style.AppTheme) // Set theme
                .setTosAndPrivacyPolicyUrls(
                    "https://example.com/terms.html",
                    "https://example.com/privacy.html")
                .build(),
            RC_SIGN_IN)
    }


    companion object {
        private const val TAG = "FB_LOGIN"
        private const val RC_SIGN_IN = 123
    }
}