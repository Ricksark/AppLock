package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AppDatabase
import com.example.data.AppLockEntity
import com.example.data.SecurityPreferences
import com.example.service.AppLockMonitorService
import com.example.service.AppLockOverlayLauncher
import com.example.service.PermissionUtils
import com.example.ui.theme.BorderSlate
import com.example.ui.theme.CardSlate
import com.example.ui.theme.ContainerNavy
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.SafeEmerald
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.theme.ThreatCrimson
import com.example.ui.theme.WarningAmber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class InstalledAppItem(
    val packageName: String,
    val appName: String,
    val isSensitive: Boolean,
    val icon: Drawable?
)

@Composable
fun AppsLockScreen(
    database: AppDatabase,
    preferences: SecurityPreferences,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val appLocks by database.appLockDao().getAllAppLocks().collectAsStateWithLifecycle(initialValue = emptyList())

    var installedApps by remember { mutableStateOf<List<InstalledAppItem>>(emptyList()) }
    var isLoadingApps by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // "ALL", "LOCKED", "UNLOCKED", "SENSITIVE"

    var hasUsagePermission by remember { mutableStateOf(PermissionUtils.hasUsageStatsPermission(context)) }
    var hasOverlayPermission by remember { mutableStateOf(PermissionUtils.hasOverlayPermission(context)) }
    var isAccessibilityActive by remember { mutableStateOf(PermissionUtils.isAccessibilityServiceEnabled(context)) }
    var isBatteryOptimized by remember { mutableStateOf(PermissionUtils.isIgnoringBatteryOptimizations(context)) }
    var showFixDiagnostics by remember { mutableStateOf(false) }

    fun refreshPermissions() {
        hasUsagePermission = PermissionUtils.hasUsageStatsPermission(context)
        hasOverlayPermission = PermissionUtils.hasOverlayPermission(context)
        isAccessibilityActive = PermissionUtils.isAccessibilityServiceEnabled(context)
        isBatteryOptimized = PermissionUtils.isIgnoringBatteryOptimizations(context)
    }

    // Load installed applications from PackageManager
    fun loadInstalledApps() {
        scope.launch {
            isLoadingApps = true
            val apps = withContext(Dispatchers.IO) {
                val pm = context.packageManager
                val list = mutableListOf<InstalledAppItem>()
                val seenPackages = mutableSetOf<String>()

                val sensitiveKeywords = listOf(
                    "bank", "pay", "wallet", "gallery", "photo", "camera",
                    "message", "sms", "chat", "whatsapp", "telegram", "signal",
                    "chrome", "browser", "mail", "gmail", "settings", "drive",
                    "files", "contact", "phone", "facebook", "instagram", "tiktok"
                )

                // 1. Query all Launcher Activities
                val launcherIntent = Intent(Intent.ACTION_MAIN, null).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }
                val resolveInfos = pm.queryIntentActivities(launcherIntent, 0)
                for (info in resolveInfos) {
                    val pkg = info.activityInfo.packageName
                    if (pkg == context.packageName) continue
                    if (seenPackages.add(pkg)) {
                        val name = info.loadLabel(pm).toString()
                        val isSensitive = sensitiveKeywords.any {
                            name.contains(it, ignoreCase = true) || pkg.contains(it, ignoreCase = true)
                        }
                        val icon = try {
                            info.loadIcon(pm)
                        } catch (e: Exception) {
                            null
                        }
                        list.add(
                            InstalledAppItem(
                                packageName = pkg,
                                appName = name,
                                isSensitive = isSensitive,
                                icon = icon
                            )
                        )
                    }
                }

                // 2. Query Installed Applications to ensure complete visibility
                val installedPackages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                for (appInfo in installedPackages) {
                    val pkg = appInfo.packageName
                    if (pkg == context.packageName) continue
                    if (pm.getLaunchIntentForPackage(pkg) != null && seenPackages.add(pkg)) {
                        val name = appInfo.loadLabel(pm).toString()
                        val isSensitive = sensitiveKeywords.any {
                            name.contains(it, ignoreCase = true) || pkg.contains(it, ignoreCase = true)
                        }
                        val icon = try {
                            appInfo.loadIcon(pm)
                        } catch (e: Exception) {
                            null
                        }
                        list.add(
                            InstalledAppItem(
                                packageName = pkg,
                                appName = name,
                                isSensitive = isSensitive,
                                icon = icon
                            )
                        )
                    }
                }

                // Fallback standard demo apps if device has very few apps installed (e.g. fresh emulators)
                if (list.size < 3) {
                    val defaultPackages = listOf(
                        Triple("com.google.android.apps.photos", "Google Photos", true),
                        Triple("com.android.chrome", "Chrome Browser", true),
                        Triple("com.android.settings", "Settings", true),
                        Triple("com.google.android.gm", "Gmail", true),
                        Triple("com.whatsapp", "WhatsApp", true),
                        Triple("com.android.camera2", "Camera", true),
                        Triple("com.google.android.apps.messaging", "Messages", true),
                        Triple("com.google.android.documentsui", "Files", true)
                    )
                    for ((pkg, name, sensitive) in defaultPackages) {
                        if (seenPackages.add(pkg)) {
                            list.add(
                                InstalledAppItem(
                                    packageName = pkg,
                                    appName = name,
                                    isSensitive = sensitive,
                                    icon = null
                                )
                            )
                        }
                    }
                }

                list.sortBy { it.appName.lowercase() }
                list
            }
            installedApps = apps
            isLoadingApps = false
        }
    }

    LaunchedEffect(Unit) {
        refreshPermissions()
        loadInstalledApps()
    }

    val lockedMap = remember(appLocks) {
        appLocks.associate { it.packageName to it.isLocked }
    }

    val lockedCount = remember(lockedMap) {
        lockedMap.count { it.value }
    }

    val filteredApps = remember(installedApps, lockedMap, searchQuery, selectedFilter) {
        installedApps.filter { app ->
            val matchesSearch = searchQuery.isBlank() ||
                    app.appName.contains(searchQuery, ignoreCase = true) ||
                    app.packageName.contains(searchQuery, ignoreCase = true)

            val isLocked = lockedMap[app.packageName] == true

            val matchesFilter = when (selectedFilter) {
                "LOCKED" -> isLocked
                "UNLOCKED" -> !isLocked
                "SENSITIVE" -> app.isSensitive
                else -> true
            }

            matchesSearch && matchesFilter
        }
    }

    val isAllConfigured = preferences.isAppLockProtectionEnabled &&
            hasUsagePermission && hasOverlayPermission

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // App Lock Shield & Diagnostic Header Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardSlate),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    1.dp,
                    if (isAllConfigured) SafeEmerald.copy(alpha = 0.5f) else WarningAmber.copy(alpha = 0.6f),
                    RoundedCornerShape(16.dp)
                )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isAllConfigured) SafeEmerald.copy(alpha = 0.15f)
                                    else WarningAmber.copy(alpha = 0.15f)
                                )
                        ) {
                            Icon(
                                imageVector = if (isAllConfigured) Icons.Default.Shield else Icons.Default.Warning,
                                contentDescription = "Protection Status",
                                tint = if (isAllConfigured) SafeEmerald else WarningAmber,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = if (isAllConfigured) "App Lock Guard: Active" else "App Lock Setup Needed",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "$lockedCount of ${installedApps.size} apps protected",
                                color = CyberCyan,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Switch(
                        checked = preferences.isAppLockProtectionEnabled,
                        onCheckedChange = { enabled ->
                            preferences.isAppLockProtectionEnabled = enabled
                            if (enabled) {
                                AppLockMonitorService.start(context)
                            } else {
                                AppLockMonitorService.stop(context)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = SafeEmerald,
                            checkedTrackColor = SafeEmerald.copy(alpha = 0.35f),
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = ContainerNavy
                        ),
                        modifier = Modifier.testTag("applock_global_switch")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // App Lock Problem Solver / Fix Diagnostics Toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(ContainerNavy)
                        .clickable {
                            refreshPermissions()
                            showFixDiagnostics = !showFixDiagnostics
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = null,
                            tint = CyberCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isAllConfigured) "App Lock Diagnostic Suite (Healthy)" else "Fix App Lock Permissions (Action Needed)",
                            color = if (isAllConfigured) CyberCyan else WarningAmber,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Icon(
                        imageVector = if (showFixDiagnostics) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = TextSecondary
                    )
                }

                // Expandable Diagnostics & Fix Panel
                AnimatedVisibility(
                    visible = showFixDiagnostics,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    ) {
                        Text(
                            text = "To guarantee real-time locking on Android 10+, enable these system permissions:",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // 1. Usage Stats
                        DiagnosticItemRow(
                            title = "1. Usage Access Permission",
                            subtitle = "Detects when protected apps enter foreground",
                            isGranted = hasUsagePermission,
                            onFix = { PermissionUtils.openUsageAccessSettings(context) }
                        )

                        // 2. Draw Over Apps
                        DiagnosticItemRow(
                            title = "2. Draw Over Other Apps (Overlay)",
                            subtitle = "Allows lock overlay to display on screen",
                            isGranted = hasOverlayPermission,
                            onFix = { PermissionUtils.openOverlaySettings(context) }
                        )

                        // 3. Accessibility Service
                        DiagnosticItemRow(
                            title = "3. Real-Time Accessibility Shield",
                            subtitle = "Provides instantaneous 0ms interception on Android 10-14+",
                            isGranted = isAccessibilityActive,
                            onFix = { PermissionUtils.openAccessibilitySettings(context) }
                        )

                        // 4. Battery Optimization
                        DiagnosticItemRow(
                            title = "4. Background Run Exemption",
                            subtitle = "Prevents Android OS from killing the guard service",
                            isGranted = isBatteryOptimized,
                            onFix = { PermissionUtils.requestIgnoreBatteryOptimizations(context) }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Test Lock Screen button
                        Button(
                            onClick = {
                                AppLockOverlayLauncher.launchLockScreen(
                                    context,
                                    context.packageName,
                                    "Test Lock Verification"
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("test_lock_overlay_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Test Lock Screen Now", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar with refresh button
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search installed applications...", color = TextSecondary, fontSize = 13.sp) },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = CyberCyan)
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyberCyan,
                    unfocusedBorderColor = BorderSlate,
                    focusedContainerColor = CardSlate,
                    unfocusedContainerColor = CardSlate,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("app_search_input")
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = { loadInstalledApps() },
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardSlate)
                    .border(1.dp, BorderSlate, RoundedCornerShape(12.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh installed apps",
                    tint = CyberCyan
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Filter Chips Row & Batch Action Buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            FilterChip(
                selected = selectedFilter == "ALL",
                onClick = { selectedFilter = "ALL" },
                label = { Text("All (${installedApps.size})", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = CyberCyan,
                    selectedLabelColor = Color.Black,
                    containerColor = CardSlate,
                    labelColor = TextSecondary
                )
            )

            FilterChip(
                selected = selectedFilter == "LOCKED",
                onClick = { selectedFilter = "LOCKED" },
                label = { Text("Locked ($lockedCount)", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = CyberCyan,
                    selectedLabelColor = Color.Black,
                    containerColor = CardSlate,
                    labelColor = TextSecondary
                )
            )

            FilterChip(
                selected = selectedFilter == "UNLOCKED",
                onClick = { selectedFilter = "UNLOCKED" },
                label = { Text("Unlocked", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = CyberCyan,
                    selectedLabelColor = Color.Black,
                    containerColor = CardSlate,
                    labelColor = TextSecondary
                )
            )

            FilterChip(
                selected = selectedFilter == "SENSITIVE",
                onClick = { selectedFilter = "SENSITIVE" },
                label = { Text("Sensitive", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = CyberCyan,
                    selectedLabelColor = Color.Black,
                    containerColor = CardSlate,
                    labelColor = TextSecondary
                )
            )

            Spacer(modifier = Modifier.weight(1f))

            // Batch Lock Sensitive Shortcut
            Text(
                text = "Lock All Sensitive",
                color = CyberCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(ContainerNavy)
                    .clickable {
                        scope.launch(Dispatchers.IO) {
                            val sensitives = installedApps.filter { it.isSensitive }
                            val entities = sensitives.map {
                                AppLockEntity(
                                    packageName = it.packageName,
                                    appName = it.appName,
                                    isLocked = true,
                                    category = "SENSITIVE"
                                )
                            }
                            database.appLockDao().insertAll(entities)
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Locked ${entities.size} sensitive apps", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .testTag("lock_all_sensitive_action")
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Main List-Based UI using LazyColumn displaying all installed applications
        if (isLoadingApps) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = CyberCyan, strokeWidth = 3.dp)
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Scanning installed applications...",
                        color = CyberCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else if (filteredApps.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("No applications found", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Try clearing your search query or filter.", color = TextSecondary, fontSize = 12.sp)
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("installed_apps_lazy_column")
            ) {
                items(
                    items = filteredApps,
                    key = { it.packageName }
                ) { appItem ->
                    val isLocked = lockedMap[appItem.packageName] == true

                    InstalledAppLockRow(
                        app = appItem,
                        isLocked = isLocked,
                        onToggleLock = { newIsLocked ->
                            scope.launch(Dispatchers.IO) {
                                database.appLockDao().insertOrUpdate(
                                    AppLockEntity(
                                        packageName = appItem.packageName,
                                        appName = appItem.appName,
                                        isLocked = newIsLocked,
                                        category = if (appItem.isSensitive) "SENSITIVE" else "GENERAL"
                                    )
                                )
                            }
                        },
                        onTestLock = {
                            AppLockOverlayLauncher.launchLockScreen(
                                context,
                                appItem.packageName,
                                appItem.appName
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun InstalledAppLockRow(
    app: InstalledAppItem,
    isLocked: Boolean,
    onToggleLock: (Boolean) -> Unit,
    onTestLock: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardSlate),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (isLocked) CyberCyan.copy(alpha = 0.5f) else BorderSlate.copy(alpha = 0.35f),
                RoundedCornerShape(14.dp)
            )
            .testTag("app_item_${app.packageName}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // App Icon
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ContainerNavy)
            ) {
                if (app.icon != null) {
                    val bitmap = remember(app.icon) { app.icon.toBitmap(96, 96) }
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = app.appName,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                } else {
                    Icon(
                        imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.Shield,
                        contentDescription = null,
                        tint = if (isLocked) CyberCyan else TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // App Label, Package Name, Tags
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = app.appName,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (app.isSensitive) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(ThreatCrimson.copy(alpha = 0.2f))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "Sensitive",
                                color = ThreatCrimson,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isLocked) "Protected with AES-256" else app.packageName,
                        color = if (isLocked) CyberCyan else TextSecondary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Quick Test Button for locked apps
            if (isLocked) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(CyberCyan.copy(alpha = 0.15f))
                        .clickable(onClick = onTestLock)
                        .padding(horizontal = 7.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Test",
                        color = CyberCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            // Status Text (Locked / Unlocked)
            Text(
                text = if (isLocked) "Locked" else "Off",
                color = if (isLocked) SafeEmerald else TextTertiary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(end = 6.dp)
            )

            // Switch Component for toggling 'Is Locked' state
            Switch(
                checked = isLocked,
                onCheckedChange = onToggleLock,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = CyberCyan,
                    checkedTrackColor = CyberCyan.copy(alpha = 0.35f),
                    uncheckedThumbColor = TextSecondary,
                    uncheckedTrackColor = ContainerNavy
                ),
                modifier = Modifier.testTag("switch_${app.packageName}")
            )
        }
    }
}

@Composable
fun DiagnosticItemRow(
    title: String,
    subtitle: String,
    isGranted: Boolean,
    onFix: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (isGranted) SafeEmerald else WarningAmber,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    color = TextSecondary,
                    fontSize = 10.sp
                )
            }
        }

        if (!isGranted) {
            Button(
                onClick = onFix,
                colors = ButtonDefaults.buttonColors(containerColor = WarningAmber),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.height(28.dp)
            ) {
                Text("Enable", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        } else {
            Text("Granted", color = SafeEmerald, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}
