package com.nobadhabbits.cornytask.features.login

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.lifecycleScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.nobadhabbits.cornytask.R
import com.nobadhabbits.cornytask.features.main.MainMenuActivity
import com.nobadhabbits.cornytask.features.widget.ACTION_DATA_UPDATED
import com.nobadhabbits.cornytask.features.widget.TodoWidgetReceiver
import com.nobadhabbits.cornytask.ui.theme.Cornytaskv2Theme
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LoginActivity : ComponentActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var credentialManager: CredentialManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        firebaseAuth = FirebaseAuth.getInstance()
        credentialManager = CredentialManager.create(this)

        setContent {
            Cornytaskv2Theme {
                LoginScreen(onSignInClick = { signIn(silent = false) })
            }
        }

        // Firebase session lost but the Google account was used before: sign back in
        // without making the user tap anything (or with a single tap after sign-out).
        if (savedInstanceState == null && firebaseAuth.currentUser == null) {
            signIn(silent = true)
        }
    }

    override fun onStart() {
        super.onStart()
        if (firebaseAuth.currentUser != null) {
            navigateToMain()
        }
    }

    private fun signIn(silent: Boolean) {
        val webClientId = getString(R.string.default_web_client_id)
        val option = if (silent) {
            GetGoogleIdOption.Builder()
                .setServerClientId(webClientId)
                .setFilterByAuthorizedAccounts(true)
                .setAutoSelectEnabled(true)
                .build()
        } else {
            GetSignInWithGoogleOption.Builder(webClientId).build()
        }
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()

        lifecycleScope.launch {
            try {
                val result = credentialManager.getCredential(this@LoginActivity, request)
                firebaseSignIn(result.credential)
            } catch (e: GetCredentialCancellationException) {
                // User dismissed the sheet; they can use the button.
            } catch (e: NoCredentialException) {
                // No previously used account: expected for the silent attempt.
                if (!silent) showError("No Google account available on this device.")
            } catch (e: GetCredentialException) {
                Log.w(TAG, "getCredential failed", e)
                if (!silent) showError("Sign-in failed: ${e.type}")
            }
        }
    }

    private suspend fun firebaseSignIn(credential: Credential) {
        if (credential !is CustomCredential ||
            credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            showError("Unexpected credential type.")
            return
        }
        try {
            val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
            val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
            firebaseAuth.signInWithCredential(firebaseCredential).await()
            navigateToMain()
        } catch (e: GoogleIdTokenParsingException) {
            Log.w(TAG, "Invalid Google ID token", e)
            showError("Sign-in failed.")
        } catch (e: Exception) {
            Log.w(TAG, "Firebase sign-in failed", e)
            showError("Firebase authentication failed.")
        }
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun navigateToMain() {
        val intent = Intent(this, TodoWidgetReceiver::class.java).apply {
            action = ACTION_DATA_UPDATED
        }
        sendBroadcast(intent)
        startActivity(Intent(this, MainMenuActivity::class.java))
        finish()
    }

    companion object {
        private const val TAG = "LoginActivity"
    }
}

@Composable
fun LoginScreen(onSignInClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.unicorn_logo),
            contentDescription = "App Logo",
            modifier = Modifier.size(150.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Welcome to Corny Task",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Cause all the hard work deserves a reward",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onSignInClick) {
            Text("Sign in with Google")
        }
    }
}
