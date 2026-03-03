package com.veiltech.demo.security

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

object AesFileCrypto {
    private const val ANDROID_KEY_STORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "veiltech_aes"

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        val existing = ks.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing
        val keyGenerator = KeyGenerator.getInstance("AES", ANDROID_KEY_STORE)
        keyGenerator.init(
            android.security.keystore.KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or android.security.keystore.KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return keyGenerator.generateKey()
    }

    fun encrypt(context: Context, input: File): File {
        val out = File(context.cacheDir, input.name + ".enc")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        FileOutputStream(out).use { fos ->
            fos.write(cipher.iv)
            java.io.FileInputStream(input).use { fis -> javax.crypto.CipherOutputStream(fos, cipher).use { cos -> fis.copyTo(cos) } }
        }
        return out
    }
}
