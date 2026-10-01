package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.crypto.BiometricHelper
import com.example.crypto.CryptoManager
import com.example.data.AppDatabase
import com.example.data.SecurityLogEntity
import com.example.data.SecurityPreferences
import com.example.service.AppLockMonitorService
import com.example.ui.components.PasscodeKeypad
import com.example.ui.screens.AppsLockScreen
import com.example.ui.screens.DocsVaultScreen
import com.example.ui.screens.PhotoVaultScreen
import com.example.ui.screens.SecuritySettingsScreen
import com.example.ui.theme.BorderSlate
import com.example.ui.theme.CardSlate
import com.example.ui.theme.ContainerNavy
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SafeEmerald
import com.example.ui.theme.ShieldNavy
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.theme.ThreatCrimson
import com.example.vault.VaultManager
import com.example.vault.VaultSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {

    private lateinit var preferences: SecurityPreferences
    private lateinit var database: AppDatabase
    private lateinit var vaultManager: VaultManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        preferences = SecurityPreferences(this)
        database = AppDatabase.getInstance(this)
        vaultManager = VaultManager(this)

        if (preferences.isAppLockProtectionEnabled) {
            AppLockMonitorService.start(this)
        }

        setContent {
            MyApplicationTheme {
                MainAppHost(
                    activity = this,
                    preferences = preferences,
                    database = database,
                    vaultManager = vaultManager
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // If auto-lock is configured for immediately and the session was unlocked, check if timeout elapsed
        if (preferences.isSetupCompleted && VaultSession.isUnlocked) {
            val delayMillis = preferences.autoLockDelaySeconds * 1000L
            if (delayMillis == 0L && preferences.lastUnlockTimestamp > 0 &&
                (System.currentTimeMillis() - preferences.lastUnlockTimestamp) > 30000L
            ) {
                VaultSession.lock()
            }
        }
    }
}

@Composable
fun MainAppHost(
    activity: FragmentActivity,
    preferences: SecurityPreferences,
    database: AppDatabase,
    vaultManager: VaultManager
) {
    var isSetupCompleted by remember { mutableStateOf(preferences.isSetupCompleted) }
    var isUnlocked by remember { mutableStateOf(VaultSession.isUnlocked) }

    if (!isSetupCompleted) {
        // Setup Wizard
        SetupWizardScreen(
            preferences = preferences,
            onSetupFinished = {
                isSetupCompleted = true
                isUnlocked = true
            }
        )
    } else if (!isUnlocked) {
        // Master Lock Screen
        MasterLockScreen(
            activity = activity,
            preferences = preferences,
            database = database,
            vaultManager = vaultManager,
            onUnlocked = { isUnlocked = true }
        )
    } else {
        // Main Vault Dashboard
        VaultDashboardScreen(
            database = database,
            preferences = preferences,
            vaultManager = vaultManager,
            onLockNow = {
                VaultSession.lock()
                isUnlocked = false
            },
            onResetMasterPasscode = {
                VaultSession.lock()
                preferences.isSetupCompleted = false
                isSetupCompleted = false
                isUnlocked = false
            }
        )
    }
}

@Composable
fun SetupWizardScreen(
    preferences: SecurityPreferences,
    onSetupFinished: () -> Unit
) {
    var step by remember { mutableIntStateOf(1) } // 1: Create PIN, 2: Confirm PIN, 3: Security & Remote Wipe
    var firstPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val pinLength = preferences.passcodeLength

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = ShieldNavy
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 40.dp)
        ) {
            // Header Info
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(CyberCyan.copy(alpha = 0.15f))
                        .border(1.5.dp, CyberCyan, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Shield",
                        tint = CyberCyan,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = when (step) {
                        1 -> "Create Master Passcode"
                        2 -> "Confirm Master Passcode"
                        else -> "Security & Remote Wipe"
                    },
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = when (step) {
                        1 -> "Choose a secure $pinLength-digit passcode to encrypt your apps and files."
                        2 -> "Re-enter the exact same $pinLength-digit passcode to verify."
                        else -> "Your vault is configured with AES-256 and emergency lost-device wipe."
                    },
                    color = TextSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            if (step == 1) {
                PasscodeKeypad(
                    pinLength = pinLength,
                    currentInput = firstPin,
                    onDigitClick = { digit ->
                        if (firstPin.length < pinLength) {
                            val updated = firstPin + digit
                            firstPin = updated
                            if (updated.length == pinLength) {
                                step = 2
                            }
                        }
                    },
                    onBackspaceClick = {
                        if (firstPin.isNotEmpty()) firstPin = firstPin.dropLast(1)
                    },
                    isError = false
                )
            } else if (step == 2) {
                PasscodeKeypad(
                    pinLength = pinLength,
                    currentInput = confirmPin,
                    onDigitClick = { digit ->
                        if (confirmPin.length < pinLength) {
                            isError = false
                            errorMessage = null
                            val updated = confirmPin + digit
                            confirmPin = updated
                            if (updated.length == pinLength) {
                                if (updated == firstPin) {
                                    preferences.setMasterPasscode(updated)
                                    val salt = preferences.passcodeSalt?.let { CryptoManager.base64ToSalt(it) }
                                        ?: CryptoManager.generateSalt()
                                    val key = CryptoManager.deriveKey(updated, salt)
                                    VaultSession.unlock(key, updated)
                                    step = 3
                                } else {
                                    isError = true
                                    errorMessage = "Passcodes do not match. Try again."
                                    confirmPin = ""
                                }
                            }
                        }
                    },
                    onBackspaceClick = {
                        if (confirmPin.isNotEmpty()) {
                            confirmPin = confirmPin.dropLast(1)
                            isError = false
                            errorMessage = null
                        }
                    },
                    isError = isError,
                    errorMessage = errorMessage
                )
            } else {
                // Step 3 Overview Card
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CardSlate),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, CyberCyan.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text("Vault Protection Enabled", color = SafeEmerald, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("• AES-256-GCM authenticated file encryption", color = TextPrimary, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("• Real-time AppLock guard for Android 10+", color = TextPrimary, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("• Default Remote Wipe Phrase: ${preferences.remoteWipeSecret}", color = CyberCyan, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("• Biometrics enabled for instant verification", color = TextPrimary, fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = onSetupFinished,
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("finish_setup_button")
                    ) {
                        Text("Enter AegisLock Vault", color = ShieldNavy, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }

            // Footer Step Indicator
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 1..3) {
                    Box(
                        modifier = Modifier
                            .size(if (i == step) 10.dp else 7.dp)
                            .clip(CircleShape)
                            .background(if (i == step) CyberCyan else TextTertiary)
                    )
                }
            }
        }
    }
}

@Composable
fun MasterLockScreen(
    activity: FragmentActivity,
    preferences: SecurityPreferences,
    database: AppDatabase,
    vaultManager: VaultManager,
    onUnlocked: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var currentInput by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val pinLength = preferences.passcodeLength
    val hasBiometrics = preferences.isBiometricEnabled && BiometricHelper.isBiometricAvailable(activity)

    fun unlockWithBiometrics() {
        BiometricHelper.showBiometricPrompt(
            activity = activity,
            title = "AegisLock Master Unlock",
            subtitle = "Verify fingerprint or face to open vault",
            negativeButtonText = "Use Passcode",
            onSuccess = {
                val key = vaultManager.getOrDeriveVaultKey()
                VaultSession.unlock(key)
                preferences.lastUnlockTimestamp = System.currentTimeMillis()
                onUnlocked()
            },
            onError = { _, _ -> },
            onFailed = {
                isError = true
                errorMessage = "Biometric verification failed"
            }
        )
    }

    LaunchedEffect(Unit) {
        if (hasBiometrics) {
            unlockWithBiometrics()
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
                .padding(horizontal = 24.dp, vertical = 36.dp)
        ) {
            // Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(CardSlate)
                        .border(1.5.dp, CyberCyan, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Lock",
                        tint = CyberCyan,
                        modifier = Modifier.size(38.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "AegisLock Vault",
                    color = TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Enter Master Passcode or use Biometrics",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }

            // Keypad
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
                            if (preferences.isDuressPasscode(updated)) {
                                // Duress trigger
                                scope.launch(Dispatchers.IO) {
                                    vaultManager.executeRemoteWipe("DURESS_PIN_ENTERED_AT_LAUNCH")
                                    onUnlocked()
                                }
                            } else if (preferences.verifyMasterPasscode(updated)) {
                                val salt = preferences.passcodeSalt?.let { CryptoManager.base64ToSalt(it) }
                                    ?: CryptoManager.generateSalt()
                                val key = CryptoManager.deriveKey(updated, salt)
                                VaultSession.unlock(key, updated)
                                onUnlocked()
                            } else {
                                isError = true
                                val rem = preferences.maxFailedAttempts - preferences.currentFailedAttempts
                                errorMessage = if (preferences.maxFailedAttempts > 0 && rem in 1..3) {
                                    "Wrong PIN! $rem attempts before remote vault purge"
                                } else {
                                    "Incorrect passcode. Try again."
                                }
                                currentInput = ""
                            }
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
                onBiometricClick = if (hasBiometrics) { { unlockWithBiometrics() } } else null,
                isBiometricAvailable = hasBiometrics,
                isError = isError,
                errorMessage = errorMessage
            )

            Text(
                text = "AES-256 Encrypted • Lost-Device Anti-Theft Protected",
                color = TextTertiary,
                fontSize = 11.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultDashboardScreen(
    database: AppDatabase,
    preferences: SecurityPreferences,
    vaultManager: VaultManager,
    onLockNow: () -> Unit,
    onResetMasterPasscode: () -> Unit
) {
    var currentTab by remember { mutableIntStateOf(0) } // 0: Apps Lock, 1: Photos, 2: Docs, 3: Security & Wipe

    // Prevent exiting immediately with back button; return to tab 0 or lock
    BackHandler {
        if (currentTab != 0) {
            currentTab = 0
        } else {
            onLockNow()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(CyberCyan.copy(alpha = 0.2f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = CyberCyan,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AegisLock",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onLockNow,
                        modifier = Modifier.testTag("lock_now_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Lock Vault",
                            tint = CyberCyan
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = ShieldNavy
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = CardSlate,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    icon = { Icon(Icons.Default.Shield, contentDescription = "App Lock") },
                    label = { Text("App Lock", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ShieldNavy,
                        selectedTextColor = CyberCyan,
                        indicatorColor = CyberCyan,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    ),
                    modifier = Modifier.testTag("nav_tab_app_lock")
                )

                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    icon = { Icon(Icons.Default.PhotoLibrary, contentDescription = "Photos") },
                    label = { Text("Photos", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ShieldNavy,
                        selectedTextColor = CyberCyan,
                        indicatorColor = CyberCyan,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    ),
                    modifier = Modifier.testTag("nav_tab_photos")
                )

                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { currentTab = 2 },
                    icon = { Icon(Icons.Default.Description, contentDescription = "Docs") },
                    label = { Text("Docs Vault", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ShieldNavy,
                        selectedTextColor = CyberCyan,
                        indicatorColor = CyberCyan,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    ),
                    modifier = Modifier.testTag("nav_tab_docs")
                )

                NavigationBarItem(
                    selected = currentTab == 3,
                    onClick = { currentTab = 3 },
                    icon = { Icon(Icons.Default.Security, contentDescription = "Security") },
                    label = { Text("Security", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ShieldNavy,
                        selectedTextColor = CyberCyan,
                        indicatorColor = CyberCyan,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    ),
                    modifier = Modifier.testTag("nav_tab_security")
                )
            }
        },
        containerColor = ShieldNavy
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "dashboard_tab_transition"
            ) { tab ->
                when (tab) {
                    0 -> AppsLockScreen(database = database, preferences = preferences)
                    1 -> PhotoVaultScreen(database = database, vaultManager = vaultManager)
                    2 -> DocsVaultScreen(database = database, vaultManager = vaultManager)
                    3 -> SecuritySettingsScreen(
                        database = database,
                        preferences = preferences,
                        vaultManager = vaultManager,
                        onResetMasterPasscode = onResetMasterPasscode
                    )
                }
            }
        }
    }
}
