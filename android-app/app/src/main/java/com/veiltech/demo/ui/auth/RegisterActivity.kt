package com.veiltech.demo.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.veiltech.demo.R
import com.veiltech.demo.data.api.NetworkModule
import com.veiltech.demo.data.repository.VeilRepository
import com.veiltech.demo.security.HashUtils
import com.veiltech.demo.security.SecureStorage
import com.veiltech.demo.ui.dashboard.DashboardActivity

class RegisterActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        val secure = SecureStorage(this)
        val vm = AuthViewModel(VeilRepository(NetworkModule.api("http://YOUR_EC2_PUBLIC_IP:8080/", secure)))
        vm.status.observe(this) { Toast.makeText(this, it, Toast.LENGTH_SHORT).show() }

        findViewById<Button>(R.id.registerBtn).setOnClickListener {
            val name = findViewById<EditText>(R.id.name).text.toString()
            val phone = findViewById<EditText>(R.id.phone).text.toString()
            val password = HashUtils.sha256(findViewById<EditText>(R.id.password).text.toString())
            vm.register(name, phone, password) { token, userId ->
                secure.saveToken(token); secure.saveUserId(userId)
                startActivity(Intent(this, DashboardActivity::class.java)); finish()
            }
        }
    }
}
