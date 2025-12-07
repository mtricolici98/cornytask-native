package com.example.cornytask_v2

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.example.cornytask_v2.features.login.LoginActivity
import com.example.cornytask_v2.features.main.MainMenuActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val account = GoogleSignIn.getLastSignedInAccount(this)
        if (account != null) {
            startActivity(Intent(this, MainMenuActivity::class.java))
        } else {
            startActivity(Intent(this, LoginActivity::class.java))
        }
        finish()
    }
}