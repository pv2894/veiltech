package com.veiltech.demo.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.veiltech.demo.ui.dashboard.DashboardActivity
import com.veiltech.demo.util.BiometricHelper

class LaunchActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BiometricHelper.authenticate(this,
            onSuccess = {
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            },
            onFailed = {
                Toast.makeText(this, "Biometric auth failed", Toast.LENGTH_LONG).show()
                finishAffinity()
            }
        )
    }
}
