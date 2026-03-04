package com.veiltech.demo.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.veiltech.demo.R
import com.veiltech.demo.data.api.NetworkModule
import com.veiltech.demo.data.repository.VeilRepository
import com.veiltech.demo.security.SecureStorage
import com.veiltech.demo.ui.transfer.TransferActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class DashboardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)
        val secure = SecureStorage(this)
        val repo = VeilRepository(NetworkModule.api("http://YOUR_EC2_PUBLIC_IP:8080/", secure))
        val list = findViewById<ListView>(R.id.userList)
        val poll = findViewById<Button>(R.id.pollRequests)

        lifecycleScope.launch {
            runCatching { repo.users() }
                .onSuccess { users ->
                    list.adapter = ArrayAdapter(this@DashboardActivity, android.R.layout.simple_list_item_1, users.map { "${it.name} (${it.phone})" })
                    list.setOnItemClickListener { _, _, pos, _ ->
                        val receiverId = users[pos].id
                        lifecycleScope.launch {
                            runCatching { repo.connect(secure.userId(), receiverId) }
                                .onSuccess {
                                    Toast.makeText(this@DashboardActivity, "Request sent: ${it.status}", Toast.LENGTH_SHORT).show()
                                    startActivity(Intent(this@DashboardActivity, TransferActivity::class.java).putExtra("requestId", it.id))
                                }
                                .onFailure { Toast.makeText(this@DashboardActivity, it.message, Toast.LENGTH_SHORT).show() }
                        }
                    }
                }
        }

        poll.setOnClickListener {
            lifecycleScope.launch {
                repeat(12) {
                    runCatching { repo.requests(secure.userId()) }
                        .onSuccess { reqs -> Toast.makeText(this@DashboardActivity, "Requests: ${reqs.size}", Toast.LENGTH_SHORT).show() }
                    delay(5000)
                }
            }
        }
    }
}
