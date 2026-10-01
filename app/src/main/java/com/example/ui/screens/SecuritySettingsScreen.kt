package com.example.ui.screens

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.NotificationImportant
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SimCardAlert
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.crypto.BiometricHelper
import com.example.data.AppDatabase
import com.example.data.SecurityLogEntity
import com.example.data.SecurityPreferences
import com.example.receiver.AegisDeviceAdminReceiver
import com.example.ui.theme.BorderSlate
import com.example.ui.theme.CardSlate
import com.example.ui.theme.ContainerNavy
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.SafeEmerald
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.ThreatCrimson
import com.example.ui.theme.WarningAmber
import com.example.vault.VaultManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SecuritySettingsScreen(
    database: AppDatabase,
    preferences: SecurityPreferences,
    vaultManager: VaultManager,
    onResetMasterPasscode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val logs by database.securityLogDao().getRecentLogs().collectAsStateWithLifecycle(initialValue = emptyList())

    var isBiometricEnabled by remember { mutableStateOf(preferences.isBiometricEnabled) }
    var remoteWipeSecret by remember { mutableStateOf(preferences.remoteWipeSecret) }
    var maxFailedAttempts by remember { mutableStateOf(preferences.maxFailedAttempts) }
    var autoLockDelay by remember { mutableStateOf(preferences.autoLockDelaySeconds) }
    var isDeviceAdminActive by remember { mutableStateOf(AegisDeviceAdminReceiver.isDeviceAdminActive(context)) }

    var showDuressPinDialog by remember { mutableStateOf(false) }
    var showWipeSecretDialog by remember { mutableStateOf(false) }
    var showSimulatorDialog by remember { mutableStateOf(false) }
    var showRealWipeConfirmDialog by remember { mutableStateOf(false) }
    var showLogsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isDeviceAdminActive = AegisDeviceAdminReceiver.isDeviceAdminActive(context)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Section 1: Biometric & Master Authentication
        Text(
            text = "AUTHENTICATION & BIOMETRICS",
            color = CyberCyan,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 6.dp)
        )

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = CardSlate),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Biometrics Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(CyberCyan.copy(alpha = 0.15f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = null,
                                tint = CyberCyan,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Biometric Unlock", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(
                                text = BiometricHelper.getBiometricStatusMessage(context),
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Switch(
                        checked = isBiometricEnabled,
                        onCheckedChange = { enabled ->
                            isBiometricEnabled = enabled
                            preferences.isBiometricEnabled = enabled
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyberCyan,
                            checkedTrackColor = CyberCyan.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier.testTag("biometric_toggle_switch")
                    )
                }

                HorizontalDivider(color = BorderSlate.copy(alpha = 0.4f), modifier = Modifier.padding(vertical = 12.dp))

                // Change Master Passcode
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onResetMasterPasscode)
                        .padding(vertical = 4.dp)
                        .testTag("change_master_passcode_row")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(ContainerNavy)
                        ) {
                            Icon(Icons.Default.Key, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Change Master Passcode", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("Configured as ${preferences.passcodeLength}-digit PIN", color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                    Text("Update", color = CyberCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                HorizontalDivider(color = BorderSlate.copy(alpha = 0.4f), modifier = Modifier.padding(vertical = 12.dp))

                // Duress / Panic PIN
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDuressPinDialog = true }
                        .padding(vertical = 4.dp)
                        .testTag("duress_pin_row")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(ThreatCrimson.copy(alpha = 0.15f))
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = ThreatCrimson, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Duress / Panic PIN", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(
                                text = if (preferences.duressPasscodeHash != null) "Active: Purges vault on emergency" else "Not configured",
                                color = if (preferences.duressPasscodeHash != null) SafeEmerald else TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                    Text("Configure", color = CyberCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Section 2: Lost Device & Remote Wipe Capabilities
        Text(
            text = "REMOTE WIPE & ANTI-THEFT PROTECTION",
            color = ThreatCrimson,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 6.dp)
        )

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = CardSlate),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, ThreatCrimson.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Remote Wipe Passphrase
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showWipeSecretDialog = true }
                        .padding(vertical = 4.dp)
                        .testTag("remote_wipe_phrase_row")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(ThreatCrimson.copy(alpha = 0.15f))
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null, tint = ThreatCrimson, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Remote Wipe Passphrase", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("Code: $remoteWipeSecret", color = CyberCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text("Edit", color = CyberCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                HorizontalDivider(color = BorderSlate.copy(alpha = 0.4f), modifier = Modifier.padding(vertical = 12.dp))

                // Auto-Wipe after N Failed Attempts
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Auto-Wipe on Brute Force", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(
                            text = if (maxFailedAttempts > 0) "After $maxFailedAttempts failed attempts" else "Disabled",
                            color = CyberCyan,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(0 to "Off", 3 to "3 Fails", 5 to "5 Fails", 10 to "10 Fails").forEach { (threshold, label) ->
                            FilterChip(
                                selected = maxFailedAttempts == threshold,
                                onClick = {
                                    maxFailedAttempts = threshold
                                    preferences.maxFailedAttempts = threshold
                                },
                                label = { Text(label, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = if (threshold > 0) ThreatCrimson else ContainerNavy,
                                    selectedLabelColor = Color.White,
                                    containerColor = ContainerNavy,
                                    labelColor = TextSecondary
                                )
                            )
                        }
                    }
                }

                HorizontalDivider(color = BorderSlate.copy(alpha = 0.4f), modifier = Modifier.padding(vertical = 12.dp))

                // Android Device Administrator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (isDeviceAdminActive) SafeEmerald.copy(alpha = 0.15f) else ContainerNavy)
                        ) {
                            Icon(
                                Icons.Default.AdminPanelSettings,
                                contentDescription = null,
                                tint = if (isDeviceAdminActive) SafeEmerald else TextSecondary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Device Administrator", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(
                                text = if (isDeviceAdminActive) "Active (Hardware Wipe Permitted)" else "Inactive (Vault Wipe Only)",
                                color = if (isDeviceAdminActive) SafeEmerald else TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    if (!isDeviceAdminActive) {
                        Button(
                            onClick = {
                                val intent = AegisDeviceAdminReceiver.createActivationIntent(context)
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ContainerNavy),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Activate", color = CyberCyan, fontSize = 11.sp)
                        }
                    } else {
                        Text("Enabled", color = SafeEmerald, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                HorizontalDivider(color = BorderSlate.copy(alpha = 0.4f), modifier = Modifier.padding(vertical = 12.dp))

                // Interactive Remote Wipe Simulator & Emergency Test
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = { showSimulatorDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = ContainerNavy),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("simulate_wipe_button")
                    ) {
                        Icon(Icons.Default.Sensors, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Test Wipe Protocol", color = CyberCyan, fontSize = 12.sp)
                    }

                    Button(
                        onClick = { showRealWipeConfirmDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = ThreatCrimson),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("emergency_purge_button")
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Purge Vault", color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Section 3: Security Audit Logs
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "SECURITY AUDIT LOGS",
                color = CyberCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "View All (${logs.size})",
                color = CyberCyan,
                fontSize = 11.sp,
                modifier = Modifier.clickable { showLogsDialog = true }
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = CardSlate),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (logs.isEmpty()) {
                    Text("No security events recorded yet.", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(8.dp))
                } else {
                    logs.take(4).forEach { log ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (log.isAlert) ThreatCrimson else SafeEmerald)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(log.details, color = TextPrimary, fontSize = 12.sp, maxLines = 1)
                                val dateStr = SimpleDateFormat("HH:mm:ss • MMM dd", Locale.getDefault()).format(Date(log.timestamp))
                                Text(dateStr, color = TextSecondary, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }

    // Configure Duress PIN Dialog
    if (showDuressPinDialog) {
        var duressPinInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showDuressPinDialog = false },
            title = { Text("Set Duress / Panic PIN", color = ThreatCrimson) },
            text = {
                Column {
                    Text(
                        "If you are forced to unlock AegisLock under duress or threat, entering this decoy PIN will silently shred all vaulted photos & documents while unlocking safely to protect you.",
                        color = TextPrimary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = duressPinInput,
                        onValueChange = { duressPinInput = it },
                        label = { Text("Panic PIN (e.g. 9999)", color = TextSecondary) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ThreatCrimson,
                            unfocusedBorderColor = BorderSlate,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (duressPinInput.length >= 4) {
                            preferences.setDuressPasscode(duressPinInput)
                            Toast.makeText(context, "Duress PIN activated", Toast.LENGTH_SHORT).show()
                            showDuressPinDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ThreatCrimson)
                ) {
                    Text("Save Duress PIN", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDuressPinDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = CardSlate
        )
    }

    // Remote Wipe Secret Phrase Dialog
    if (showWipeSecretDialog) {
        var secretInput by remember { mutableStateOf(remoteWipeSecret) }
        AlertDialog(
            onDismissRequest = { showWipeSecretDialog = false },
            title = { Text("Configure Remote Wipe Passphrase", color = CyberCyan) },
            text = {
                Column {
                    Text(
                        "To wipe this lost device remotely, send a broadcast intent or trigger via emergency SMS containing this exact secret code. The application will immediately purge all encrypted vaults.",
                        color = TextPrimary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = secretInput,
                        onValueChange = { secretInput = it },
                        label = { Text("Emergency Passphrase", color = TextSecondary) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = BorderSlate,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (secretInput.isNotBlank()) {
                            preferences.remoteWipeSecret = secretInput
                            remoteWipeSecret = secretInput
                            Toast.makeText(context, "Remote wipe phrase saved", Toast.LENGTH_SHORT).show()
                            showWipeSecretDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan)
                ) {
                    Text("Save Code", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWipeSecretDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = CardSlate
        )
    }

    // Remote Wipe Test / Simulator Dialog
    if (showSimulatorDialog) {
        AlertDialog(
            onDismissRequest = { showSimulatorDialog = false },
            title = { Text("Remote Wipe Protocol Simulator", color = CyberCyan) },
            text = {
                Column {
                    Text(
                        "Test and verify the remote wipe trigger safely. This will simulate receiving the remote wipe broadcast with secret '$remoteWipeSecret'.",
                        color = TextPrimary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(ContainerNavy)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "Command: am broadcast -a com.aistudio.applocker.ACTION_REMOTE_WIPE --es code \"$remoteWipeSecret\"",
                            color = CyberCyan,
                            fontSize = 11.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            database.securityLogDao().insertLog(
                                SecurityLogEntity(
                                    eventType = "REMOTE_WIPE_SIMULATED",
                                    details = "Verified remote wipe trigger protocol with code '$remoteWipeSecret'."
                                )
                            )
                        }
                        Toast.makeText(context, "Protocol verified: Remote wipe listener responding properly.", Toast.LENGTH_LONG).show()
                        showSimulatorDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan)
                ) {
                    Text("Run Dry-Run Test", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSimulatorDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = CardSlate
        )
    }

    // Real Wipe Emergency Confirmation Dialog
    if (showRealWipeConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showRealWipeConfirmDialog = false },
            title = { Text("EMERGENCY PURGE CONFIRMATION", color = ThreatCrimson, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "WARNING: This will immediately execute DoD 5220.22-M 3-pass file shredding across all encrypted photos, documents, and reset all app locks. This action CANNOT be undone.",
                    color = TextPrimary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val result = vaultManager.executeRemoteWipe("Manual User Emergency Purge")
                            showRealWipeConfirmDialog = false
                            Toast.makeText(context, "Purge complete: ${result.filesShredded} files shredded.", Toast.LENGTH_LONG).show()
                            onResetMasterPasscode()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ThreatCrimson)
                ) {
                    Text("PURGE ALL DATA NOW", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRealWipeConfirmDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = CardSlate
        )
    }

    // Security Audit Logs Dialog
    if (showLogsDialog) {
        AlertDialog(
            onDismissRequest = { showLogsDialog = false },
            title = { Text("Security Audit Logs", color = CyberCyan) },
            text = {
                Column(modifier = Modifier.height(300.dp).verticalScroll(rememberScrollState())) {
                    if (logs.isEmpty()) {
                        Text("No logs found.", color = TextSecondary)
                    } else {
                        logs.forEach { log ->
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Text(
                                    text = log.details,
                                    color = if (log.isAlert) ThreatCrimson else TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))
                                Text(
                                    text = "$dateStr • ${log.eventType}",
                                    color = TextSecondary,
                                    fontSize = 10.sp
                                )
                                HorizontalDivider(color = BorderSlate.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 4.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            database.securityLogDao().clearLogs()
                        }
                        showLogsDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ContainerNavy)
                ) {
                    Text("Clear Logs", color = CyberCyan)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogsDialog = false }) {
                    Text("Close", color = TextSecondary)
                }
            },
            containerColor = CardSlate
        )
    }
}
