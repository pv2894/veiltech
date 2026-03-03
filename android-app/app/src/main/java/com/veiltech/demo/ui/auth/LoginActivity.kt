package com.veiltech.demo.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.veiltech.demo.R
import com.veiltech.demo.data.api.NetworkModule
import com.veiltech.demo.data.repository.VeilRepository
import com.veiltech.demo.security.HashUtils
import com.veiltech.demo.security.SecureStorage
import com.veiltech.demo.ui.dashboard.DashboardActivity

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        val phone = findViewById<EditText>(R.id.phone)
        val pass = findViewById<EditText>(R.id.password)
        val register = findViewById<TextView>(R.id.gotoRegister)
        val btn = findViewById<Button>(R.id.loginBtn)

        val secure = SecureStorage(this)
        val vm = AuthViewModel(VeilRepository(NetworkModule.api("http://YOUR_EC2_PUBLIC_IP:8080/", secure)))
        vm.status.observe(this) { Toast.makeText(this, it, Toast.LENGTH_SHORT).show() }

        btn.setOnClickListener {
            vm.login(phone.text.toString(), HashUtils.sha256(pass.text.toString())) { token, userId ->
                secure.saveToken(token); secure.saveUserId(userId)
                startActivity(Intent(this, DashboardActivity::class.java)); finish()
            }
        }
        register.setOnClickListener { startActivity(Intent(this, RegisterActivity::class.java)) }
    }
}
