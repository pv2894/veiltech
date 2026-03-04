package com.veiltech.demo.util

import androidx.activity.ComponentActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat

object BiometricHelper {
    fun canUse(activity: ComponentActivity): Boolean {
        val status = BiometricManager.from(activity).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        return status == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun authenticate(activity: ComponentActivity, onSuccess: () -> Unit, onFailed: () -> Unit) {
        if (!canUse(activity)) {
            onFailed(); return
        }
        val prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onSuccess()
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) = onFailed()
            override fun onAuthenticationFailed() = Unit
        })
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("VeilTech Security Check")
                .setSubtitle("Authenticate to continue")
                .setNegativeButtonText("Exit")
                .build()
        )
    }
}
