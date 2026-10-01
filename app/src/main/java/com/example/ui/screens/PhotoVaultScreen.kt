package com.example.ui.screens

import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AppDatabase
import com.example.data.VaultItemEntity
import com.example.ui.theme.BorderSlate
import com.example.ui.theme.CardSlate
import com.example.ui.theme.ContainerNavy
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ShieldNavy
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.ThreatCrimson
import com.example.vault.VaultManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PhotoVaultScreen(
    database: AppDatabase,
    vaultManager: VaultManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val photos by database.vaultDao().getItemsByType("PHOTO").collectAsStateWithLifecycle(initialValue = emptyList())

    var isImporting by remember { mutableStateOf(false) }
    var selectedPhotoItem by remember { mutableStateOf<VaultItemEntity?>(null) }
    var photoToDelete by remember { mutableStateOf<VaultItemEntity?>(null) }

    // Modern zero-permission Android Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 15)
    ) { uris ->
        if (uris.isNotEmpty()) {
            isImporting = true
            scope.launch {
                var importedCount = 0
                for (uri in uris) {
                    val result = vaultManager.importPhoto(uri)
                    if (result.isSuccess) importedCount++
                }
                isImporting = false
                Toast.makeText(context, "Encrypted and vaulted $importedCount photo(s)", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val totalEncryptedBytes = remember(photos) {
        photos.sumOf { it.fileSizeBytes }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Vault Header Summary Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardSlate),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberCyan.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(CyberCyan.copy(alpha = 0.15f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = "Photo Vault",
                            tint = CyberCyan,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "AES-256 Encrypted Photo Vault",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "${photos.size} photo(s) securely hidden • ${formatBytes(totalEncryptedBytes)}",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(ContainerNavy)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "GCM Auth",
                            color = CyberCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (isImporting) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    CircularProgressIndicator(
                        color = CyberCyan,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Encrypting and moving photos to private vault...",
                        color = CyberCyan,
                        fontSize = 12.sp
                    )
                }
            }

            if (photos.isEmpty()) {
                // Empty State
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(ContainerNavy)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Empty Vault",
                                tint = CyberCyan,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "No Photos in Secure Vault",
                            color = TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Photos added here are encrypted with military-grade AES-256 and removed from device gallery visibility.",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Button(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("empty_vault_add_photos_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = ShieldNavy
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Vault Photos Now", color = ShieldNavy, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // Photos Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("photos_grid")
                ) {
                    items(photos, key = { it.id }) { photoItem ->
                        VaultPhotoGridItem(
                            item = photoItem,
                            vaultManager = vaultManager,
                            onClick = { selectedPhotoItem = photoItem }
                        )
                    }
                }
            }
        }

        // Floating Action Button to Add Photos
        FloatingActionButton(
            onClick = {
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            containerColor = CyberCyan,
            contentColor = ShieldNavy,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("fab_add_photos")
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Photos")
        }

        // Fullscreen Secure Photo Viewer Dialog
        selectedPhotoItem?.let { item ->
            SecurePhotoViewerDialog(
                item = item,
                vaultManager = vaultManager,
                onDismiss = { selectedPhotoItem = null },
                onExport = {
                    scope.launch {
                        exportPhotoToPublic(context, vaultManager, item)
                    }
                },
                onShred = {
                    photoToDelete = item
                }
            )
        }

        // Shred Confirmation Dialog
        photoToDelete?.let { item ->
            AlertDialog(
                onDismissRequest = { photoToDelete = null },
                title = { Text("DoD 5220.22-M Secure Shred", color = ThreatCrimson) },
                text = {
                    Text(
                        text = "Are you sure you want to permanently shred '${item.title}'? The file will be overwritten with zero and random byte passes to prevent forensic recovery.",
                        color = TextPrimary
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            scope.launch {
                                vaultManager.shredAndDeleteItem(item)
                                photoToDelete = null
                                selectedPhotoItem = null
                                Toast.makeText(context, "File securely shredded and purged.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ThreatCrimson)
                    ) {
                        Text("Shred Permanently", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { photoToDelete = null }) {
                        Text("Cancel", color = TextSecondary)
                    }
                },
                containerColor = CardSlate
            )
        }
    }
}

@Composable
fun VaultPhotoGridItem(
    item: VaultItemEntity,
    vaultManager: VaultManager,
    onClick: () -> Unit
) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(item.id) {
        val decrypted = vaultManager.decryptPhotoToBitmap(item)
        bitmap = decrypted
        isLoading = false
    }

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = CardSlate),
        modifier = Modifier
            .aspectRatio(1f)
            .border(1.dp, BorderSlate.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .testTag("photo_item_${item.id}")
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else if (isLoading) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    CircularProgressIndicator(
                        color = CyberCyan,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(24.dp)
                    )
                }
            } else {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Encrypted",
                        tint = TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Lock Badge Overlay
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(ShieldNavy.copy(alpha = 0.75f))
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = CyberCyan,
                    modifier = Modifier.size(12.dp)
                )
            }

            // Title Overlay at bottom
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(ShieldNavy.copy(alpha = 0.7f))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = item.title,
                    color = TextPrimary,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun SecurePhotoViewerDialog(
    item: VaultItemEntity,
    vaultManager: VaultManager,
    onDismiss: () -> Unit,
    onExport: () -> Unit,
    onShred: () -> Unit
) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(item.id) {
        bitmap = vaultManager.decryptPhotoToBitmap(item)
        isLoading = false
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ShieldNavy)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top App Bar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextPrimary
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = item.title,
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "AES-256 Decrypted in RAM",
                            color = CyberCyan,
                            fontSize = 11.sp
                        )
                    }

                    IconButton(onClick = onShred) {
                        Icon(
                            imageVector = Icons.Default.DeleteForever,
                            contentDescription = "Shred",
                            tint = ThreatCrimson
                        )
                    }
                }

                // Main Image Container
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap!!.asImageBitmap(),
                            contentDescription = item.title,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (isLoading) {
                        CircularProgressIndicator(color = CyberCyan)
                    } else {
                        Text("Failed to decrypt photo", color = ThreatCrimson)
                    }
                }

                // Bottom Metadata & Export Actions
                Card(
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSlate),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Size: ${formatBytes(item.fileSizeBytes)}",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                            val dateStr = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                                .format(Date(item.createdAt))
                            Text(
                                text = dateStr,
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = onExport,
                                colors = ButtonDefaults.buttonColors(containerColor = ContainerNavy),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FileDownload,
                                    contentDescription = null,
                                    tint = CyberCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Export to Storage", color = CyberCyan, fontSize = 13.sp)
                            }

                            Button(
                                onClick = onShred,
                                colors = ButtonDefaults.buttonColors(containerColor = ThreatCrimson),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteForever,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Shred File", color = Color.White, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

private suspend fun exportPhotoToPublic(context: android.content.Context, vaultManager: VaultManager, item: VaultItemEntity) {
    withContext(Dispatchers.IO) {
        try {
            val bytes = vaultManager.decryptDocumentBytes(item) ?: return@withContext
            val exportDir = File(context.getExternalFilesDir(null), "Aegis_Restored_Photos").apply { mkdirs() }
            val destFile = File(exportDir, item.originalFileName)
            FileOutputStream(destFile).use { it.write(bytes) }
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Exported safely to: ${destFile.name}", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format(Locale.US, "%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}
