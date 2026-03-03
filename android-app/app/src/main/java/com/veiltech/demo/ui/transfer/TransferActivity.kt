package com.veiltech.demo.ui.transfer

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.veiltech.demo.R
import com.veiltech.demo.data.api.NetworkModule
import com.veiltech.demo.data.repository.VeilRepository
import com.veiltech.demo.security.AesFileCrypto
import com.veiltech.demo.security.HashUtils
import com.veiltech.demo.security.SecureStorage
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime

class TransferActivity : ComponentActivity() {
    private var maskedUri: Uri? = null
    private var actualUri: Uri? = null

    private val picker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data ?: return@registerForActivityResult
            val target = result.data?.getStringExtra("target")
            if (target == "masked") maskedUri = uri else actualUri = uri
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transfer)
        val requestId = intent.getLongExtra("requestId", -1)
        val secure = SecureStorage(this)
        val api = NetworkModule.api("http://YOUR_EC2_PUBLIC_IP:8080/", secure)

        findViewById<Button>(R.id.pickMasked).setOnClickListener { pick("masked") }
        findViewById<Button>(R.id.pickActual).setOnClickListener {
            Toast.makeText(this, "Actual file must be ≤ masked file size", Toast.LENGTH_SHORT).show()
            pick("actual")
        }

        findViewById<Button>(R.id.uploadBtn).setOnClickListener {
            val pinHash = HashUtils.sha256(findViewById<EditText>(R.id.pin).text.toString())
            val expiryMin = findViewById<EditText>(R.id.expiryMinutes).text.toString().toLongOrNull() ?: 5
            lifecycleScope.launch {
                runCatching {
                    validateFiles()
                    val maskedFile = copyToCache(maskedUri!!, "masked")
                    val actualFile = copyToCache(actualUri!!, "actual")
                    val encrypted = AesFileCrypto.encrypt(this@TransferActivity, actualFile)
                    val maskedPart = MultipartBody.Part.createFormData("masked", maskedFile.name, maskedFile.asRequestBody("application/octet-stream".toMediaTypeOrNull()))
                    val encPart = MultipartBody.Part.createFormData("encryptedActual", encrypted.name, encrypted.asRequestBody("application/octet-stream".toMediaTypeOrNull()))
                    api.upload(
                        requestId.toString().toRequestBody(),
                        pinHash.toRequestBody(),
                        LocalDateTime.now().plusMinutes(expiryMin).toString().toRequestBody(),
                        maskedPart,
                        encPart
                    )
                }.onSuccess {
                    findViewById<TextView>(R.id.status).text = "Uploaded successfully"
                }.onFailure {
                    Toast.makeText(this@TransferActivity, it.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun pick(target: String) {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            putExtra("target", target)
        }
        picker.launch(intent)
    }

    private fun copyToCache(uri: Uri, name: String): File {
        val out = File(cacheDir, "$name-${System.currentTimeMillis()}")
        contentResolver.openInputStream(uri)!!.use { input ->
            FileOutputStream(out).use { output -> input.copyTo(output) }
        }
        return out
    }

    private fun validateFiles() {
        val m = maskedUri ?: error("Select masked file")
        val a = actualUri ?: error("Select actual file")
        val mSize = sizeOf(m)
        val aSize = sizeOf(a)
        if (aSize > mSize) error("Actual file must be <= masked file size")
    }

    private fun sizeOf(uri: Uri): Long {
        contentResolver.query(uri, null, null, null, null)?.use {
            val idx = it.getColumnIndex(OpenableColumns.SIZE)
            if (it.moveToFirst() && idx != -1) return it.getLong(idx)
        }
        return 0L
    }
}
