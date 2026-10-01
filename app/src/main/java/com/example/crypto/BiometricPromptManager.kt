package com.example.crypto

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

sealed class BiometricAuthResult {
    object Success : BiometricAuthResult()
    object Failed : BiometricAuthResult()
    data class Error(val errorCode: Int, val errString: String) : BiometricAuthResult()
    data class HardwareUnavailable(val reason: String) : BiometricAuthResult()
}

enum class BiometricHardwareStatus {
    AVAILABLE,
    NOT_AVAILABLE,
    NONE_ENROLLED,
    SECURITY_UPDATE_REQUIRED,
    UNSUPPORTED
}

class BiometricPromptManager(private val context: Context) {

    private val biometricManager = BiometricManager.from(context)

    /**
     * Checks if biometric sensor hardware is available and has credentials enrolled.
     */
    fun checkHardwareStatus(): BiometricHardwareStatus {
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK

        return when (biometricManager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricHardwareStatus.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricHardwareStatus.NONE_ENROLLED
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricHardwareStatus.NOT_AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricHardwareStatus.NOT_AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> BiometricHardwareStatus.SECURITY_UPDATE_REQUIRED
            else -> BiometricHardwareStatus.UNSUPPORTED
        }
    }

    fun isBiometricReady(): Boolean {
        return checkHardwareStatus() == BiometricHardwareStatus.AVAILABLE
    }

    fun getStatusDescription(): String {
        return when (checkHardwareStatus()) {
            BiometricHardwareStatus.AVAILABLE -> "Hardware-backed biometric sensor active (Fingerprint/Face)"
            BiometricHardwareStatus.NONE_ENROLLED -> "No fingerprint or face registered in Android Settings"
            BiometricHardwareStatus.NOT_AVAILABLE -> "Biometric hardware unavailable or not present"
            BiometricHardwareStatus.SECURITY_UPDATE_REQUIRED -> "Security patch update required for biometric sensor"
            BiometricHardwareStatus.UNSUPPORTED -> "Biometric authentication not supported on this device"
        }
    }

    /**
     * Launch standard biometric prompt for app unlock flow.
     */
    fun showPrompt(
        activity: FragmentActivity,
        title: String = "Biometric Verification",
        subtitle: String = "Verify fingerprint or face to proceed",
        negativeButtonText: String = "Use Passcode",
        onResult: (BiometricAuthResult) -> Unit
    ) {
        if (!isBiometricReady()) {
            onResult(BiometricAuthResult.HardwareUnavailable(getStatusDescription()))
            return
        }

        val executor = ContextCompat.getMainExecutor(activity)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onResult(BiometricAuthResult.Success)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onResult(BiometricAuthResult.Error(errorCode, errString.toString()))
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onResult(BiometricAuthResult.Failed)
            }
        }

        val prompt = BiometricPrompt(activity, executor, callback)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeButtonText)
            .setConfirmationRequired(false)
            .build()

        prompt.authenticate(promptInfo)
    }

    /**
     * Hardware-backed cryptographic biometric authentication (Cipher verification).
     */
    fun showCryptoPrompt(
        activity: FragmentActivity,
        cryptoObject: BiometricPrompt.CryptoObject,
        title: String = "AegisLock Hardware Unlock",
        subtitle: String = "Hardware-backed biometric signature required",
        negativeButtonText: String = "Use Passcode",
        onResult: (BiometricAuthResult) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onResult(BiometricAuthResult.Success)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onResult(BiometricAuthResult.Error(errorCode, errString.toString()))
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onResult(BiometricAuthResult.Failed)
            }
        }

        val prompt = BiometricPrompt(activity, executor, callback)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeButtonText)
            .setConfirmationRequired(false)
            .build()

        prompt.authenticate(promptInfo, cryptoObject)
    }

    companion object {
        private var instance: BiometricPromptManager? = null

        fun getInstance(context: Context): BiometricPromptManager {
            return instance ?: synchronized(this) {
                instance ?: BiometricPromptManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
