package com.veiltech.demo.ui.receiver

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.veiltech.demo.R
import com.veiltech.demo.data.api.NetworkModule
import com.veiltech.demo.data.repository.VeilRepository
import com.veiltech.demo.security.HashUtils
import com.veiltech.demo.security.SecureStorage
import com.veiltech.demo.util.BiometricHelper
import kotlinx.coroutines.launch

class ReceiverActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_receiver)
        val secure = SecureStorage(this)
        val repo = VeilRepository(NetworkModule.api("http://YOUR_EC2_PUBLIC_IP:8080/", secure))

        findViewById<Button>(R.id.verifyPinBtn).setOnClickListener {
            BiometricHelper.authenticate(this,
                onSuccess = {
                    val sessionId = findViewById<EditText>(R.id.sessionId).text.toString().toLong()
                    val pinHash = HashUtils.sha256(findViewById<EditText>(R.id.pin).text.toString())
                    lifecycleScope.launch {
                        runCatching { repo.verifyPin(sessionId, pinHash) }
                            .onSuccess { Toast.makeText(this@ReceiverActivity, "${it.message}, attempts left=${it.attemptsLeft}", Toast.LENGTH_LONG).show() }
                            .onFailure { Toast.makeText(this@ReceiverActivity, it.message, Toast.LENGTH_LONG).show() }
                    }
                },
                onFailed = { finishAffinity() }
            )
        }
    }
}
