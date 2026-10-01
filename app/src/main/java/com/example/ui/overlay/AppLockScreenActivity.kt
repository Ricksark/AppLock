package com.example.ui.overlay

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.example.crypto.BiometricHelper
import com.example.data.AppDatabase
import com.example.data.SecurityLogEntity
import com.example.data.SecurityPreferences
import com.example.service.AppLockOverlayLauncher
import com.example.service.LockStateManager
import com.example.ui.components.PasscodeKeypad
import com.example.ui.theme.CardSlate
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ShieldNavy
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.ThreatCrimson
import com.example.vault.VaultManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppLockScreenActivity : FragmentActivity() {

    private lateinit var preferences: SecurityPreferences
    private lateinit var database: AppDatabase
    private var targetPackageName: String = ""
    private var targetAppName: String = "Locked Application"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferences = SecurityPreferences(this)
        database = AppDatabase.getInstance(this)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        window.addFlags(
            android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
        )

        handleIntent(intent)

        // Prevent bypassing via Back button: redirect to Launcher Home screen
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(homeIntent)
                finish()
            }
        })
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        targetPackageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: ""
        targetAppName = intent.getStringExtra(EXTRA_APP_NAME) ?: "Locked Application"

        val appIconDrawable = try {
            if (targetPackageName.isNotEmpty()) {
                packageManager.getApplicationIcon(targetPackageName)
            } else null
        } catch (e: Exception) {
            null
        }

        setContent {
            MyApplicationTheme {
                AppLockScreenContent(
                    appName = targetAppName,
                    appIcon = appIconDrawable,
                    pinLength = preferences.passcodeLength,
                    isBiometricEnabled = preferences.isBiometricEnabled && BiometricHelper.isBiometricAvailable(this),
                    onUnlockSuccess = { handleUnlockSuccess() },
                    onDuressUnlock = { handleDuressUnlock() },
                    onFailedAttempt = { handleFailedAttempt() },
                    onRequestBiometric = { promptBiometrics() }
                )
            }
        }

        if (preferences.isBiometricEnabled && BiometricHelper.isBiometricAvailable(this)) {
            promptBiometrics()
        }
    }

    override fun onDestroy() {
        AppLockOverlayLauncher.dismissLockNotification(this)
        super.onDestroy()
    }

    private fun promptBiometrics() {
        BiometricHelper.showBiometricPrompt(
            activity = this,
            title = "Unlock $targetAppName",
            subtitle = "Verify biometric identity to access application",
            negativeButtonText = "Use Passcode",
            onSuccess = {
                handleUnlockSuccess()
            },
            onError = { _, _ ->
                // Fallback to passcode keypad
            },
            onFailed = {
                handleFailedAttempt()
            }
        )
    }

    private fun handleUnlockSuccess() {
        if (targetPackageName.isNotEmpty()) {
            LockStateManager.markAppUnlocked(targetPackageName)
            lifecycleScope.launch(Dispatchers.IO) {
                database.appLockDao().incrementUnlockCount(targetPackageName)
                database.securityLogDao().insertLog(
                    SecurityLogEntity(
                        eventType = "UNLOCK_SUCCESS",
                        details = "Successfully unlocked $targetAppName"
                    )
                )
            }
        }
        finish()
    }

    private fun handleDuressUnlock() {
        lifecycleScope.launch(Dispatchers.IO) {
            val vaultManager = VaultManager(applicationContext)
            vaultManager.executeRemoteWipe("DURESS_CODE_ENTERED_AT_OVERLAY")
            withContext(Dispatchers.Main) {
                Toast.makeText(applicationContext, "Safe mode unlocked", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun handleFailedAttempt() {
        val maxAttempts = preferences.maxFailedAttempts
        val currentFailed = preferences.currentFailedAttempts

        lifecycleScope.launch(Dispatchers.IO) {
            database.securityLogDao().insertLog(
                SecurityLogEntity(
                    eventType = "UNLOCK_FAILED",
                    details = "Failed passcode attempt for $targetAppName (Attempt $currentFailed/$maxAttempts)",
                    isAlert = true
                )
            )

            if (maxAttempts in 1..currentFailed) {
                // Trigger auto-wipe on brute force detection!
                val vaultManager = VaultManager(applicationContext)
                vaultManager.executeRemoteWipe("AUTO_WIPE_MAX_ATTEMPTS_EXCEEDED")
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        applicationContext,
                        "Anti-theft protocol triggered: Vault data cleared.",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                }
            }
        }
    }

    companion object {
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val EXTRA_APP_NAME = "extra_app_name"
    }
}

@Composable
fun AppLockScreenContent(
    appName: String,
    appIcon: Drawable?,
    pinLength: Int,
    isBiometricEnabled: Boolean,
    onUnlockSuccess: () -> Unit,
    onDuressUnlock: () -> Unit,
    onFailedAttempt: () -> Unit,
    onRequestBiometric: () -> Unit
) {
    val preferences = remember { SecurityPreferences(com.example.AegisApplication.instance) }
    var currentInput by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun checkPasscode(pin: String) {
        if (preferences.isDuressPasscode(pin)) {
            onDuressUnlock()
            return
        }
        if (preferences.verifyMasterPasscode(pin)) {
            onUnlockSuccess()
        } else {
            isError = true
            val remaining = preferences.maxFailedAttempts - preferences.currentFailedAttempts
            errorMessage = if (preferences.maxFailedAttempts > 0 && remaining in 1..3) {
                "Wrong passcode! Warning: $remaining attempts remaining before wipe"
            } else {
                "Incorrect passcode. Try again."
            }
            onFailedAttempt()
            currentInput = ""
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = ShieldNavy
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp)
        ) {
            // Header: Target App Information
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(CardSlate)
                        .border(1.5.dp, CyberCyan.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                ) {
                    if (appIcon != null) {
                        val bitmap = remember(appIcon) { appIcon.toBitmap(120, 120) }
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = appName,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked",
                            tint = CyberCyan,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = appName,
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "AegisLock Encrypted Protection",
                    color = CyberCyan,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Keypad Component
            PasscodeKeypad(
                pinLength = pinLength,
                currentInput = currentInput,
                onDigitClick = { digit ->
                    if (currentInput.length < pinLength) {
                        isError = false
                        errorMessage = null
                        val updated = currentInput + digit
                        currentInput = updated
                        if (updated.length == pinLength) {
                            checkPasscode(updated)
                        }
                    }
                },
                onBackspaceClick = {
                    if (currentInput.isNotEmpty()) {
                        currentInput = currentInput.dropLast(1)
                        isError = false
                        errorMessage = null
                    }
                },
                onBiometricClick = if (isBiometricEnabled) onRequestBiometric else null,
                isBiometricAvailable = isBiometricEnabled,
                isError = isError,
                errorMessage = errorMessage
            )

            // Security footer
            Text(
                text = "Protected with AES-256 & Biometric Authentication",
                color = TextSecondary,
                fontSize = 11.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
    }
}
