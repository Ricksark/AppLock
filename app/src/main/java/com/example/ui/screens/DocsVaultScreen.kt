package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AppDatabase
import com.example.data.VaultItemEntity
import com.example.ui.theme.BorderSlate
import com.example.ui.theme.CardSlate
import com.example.ui.theme.ContainerNavy
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.SafeEmerald
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
fun DocsVaultScreen(
    database: AppDatabase,
    vaultManager: VaultManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val allItems by database.vaultDao().getAllVaultItems().collectAsStateWithLifecycle(initialValue = emptyList())

    // Only non-photo items (documents and confidential notes)
    val docItems = remember(allItems) {
        allItems.filter { it.type != "PHOTO" }
    }

    var selectedFilter by remember { mutableStateOf("ALL") } // "ALL", "NOTE", "DOCUMENT", "Financial", "IDs"
    var showNewNoteDialog by remember { mutableStateOf(false) }
    var selectedItemForView by remember { mutableStateOf<VaultItemEntity?>(null) }
    var itemToShred by remember { mutableStateOf<VaultItemEntity?>(null) }

    // System File picker for documents
    val docPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            scope.launch {
                var imported = 0
                for (uri in uris) {
                    val result = vaultManager.importDocument(uri, category = "Confidential")
                    if (result.isSuccess) imported++
                }
                Toast.makeText(context, "Encrypted & vaulted $imported document(s)", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val filteredItems = remember(docItems, selectedFilter) {
        when (selectedFilter) {
            "ALL" -> docItems
            "NOTE" -> docItems.filter { it.type == "NOTE" }
            "DOCUMENT" -> docItems.filter { it.type == "DOCUMENT" }
            else -> docItems.filter { it.category.equals(selectedFilter, ignoreCase = true) }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Vault Header Card
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
                        imageVector = Icons.Default.Description,
                        contentDescription = "Docs Vault",
                        tint = CyberCyan,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Encrypted Documents & Notes",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "${docItems.size} sensitive item(s) secured on disk",
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
                        text = "Zero-Knowledge",
                        color = CyberCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Action Buttons Row: Add Doc & New Note
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = {
                    docPickerLauncher.launch(
                        arrayOf(
                            "application/pdf",
                            "text/*",
                            "application/msword",
                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                            "application/vnd.ms-excel",
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                            "*/*"
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = ContainerNavy),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("import_doc_button")
            ) {
                Icon(
                    imageVector = Icons.Default.UploadFile,
                    contentDescription = null,
                    tint = CyberCyan,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Import Files", color = CyberCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            Button(
                onClick = { showNewNoteDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("create_note_button")
            ) {
                Icon(
                    imageVector = Icons.Default.EditNote,
                    contentDescription = null,
                    tint = ShieldNavy,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("New Note", color = ShieldNavy, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Filter chips
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            FilterChip(
                selected = selectedFilter == "ALL",
                onClick = { selectedFilter = "ALL" },
                label = { Text("All", fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = CyberCyan,
                    selectedLabelColor = Color.Black,
                    containerColor = CardSlate,
                    labelColor = TextSecondary
                )
            )

            FilterChip(
                selected = selectedFilter == "NOTE",
                onClick = { selectedFilter = "NOTE" },
                label = { Text("Notes", fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = CyberCyan,
                    selectedLabelColor = Color.Black,
                    containerColor = CardSlate,
                    labelColor = TextSecondary
                )
            )

            FilterChip(
                selected = selectedFilter == "DOCUMENT",
                onClick = { selectedFilter = "DOCUMENT" },
                label = { Text("Files & PDFs", fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = CyberCyan,
                    selectedLabelColor = Color.Black,
                    containerColor = CardSlate,
                    labelColor = TextSecondary
                )
            )

            FilterChip(
                selected = selectedFilter == "Financial",
                onClick = { selectedFilter = "Financial" },
                label = { Text("Financial", fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = CyberCyan,
                    selectedLabelColor = Color.Black,
                    containerColor = CardSlate,
                    labelColor = TextSecondary
                )
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Documents and Notes List
        if (filteredItems.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(ContainerNavy)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = CyberCyan,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "No Sensitive Documents Vaulted",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Tap 'Import Files' to encrypt contracts/tax records or 'New Note' to store passwords & confidential codes.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("docs_list")
            ) {
                items(filteredItems, key = { it.id }) { item ->
                    DocItemCard(
                        item = item,
                        onClick = { selectedItemForView = item },
                        onShred = { itemToShred = item }
                    )
                }
            }
        }
    }

    // Create New Confidential Note Dialog
    if (showNewNoteDialog) {
        CreateNoteDialog(
            onDismiss = { showNewNoteDialog = false },
            onSave = { title, content, category ->
                scope.launch {
                    val result = vaultManager.saveNote(title, content, category)
                    if (result.isSuccess) {
                        Toast.makeText(context, "Note encrypted & saved in vault", Toast.LENGTH_SHORT).show()
                    }
                    showNewNoteDialog = false
                }
            }
        )
    }

    // View Encrypted Document or Note Dialog
    selectedItemForView?.let { item ->
        ViewVaultItemDialog(
            item = item,
            vaultManager = vaultManager,
            onDismiss = { selectedItemForView = null },
            onShred = {
                itemToShred = item
            }
        )
    }

    // Shred Confirmation Dialog
    itemToShred?.let { item ->
        AlertDialog(
            onDismissRequest = { itemToShred = null },
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
                            itemToShred = null
                            selectedItemForView = null
                            Toast.makeText(context, "File securely shredded and purged.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ThreatCrimson)
                ) {
                    Text("Shred Permanently", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToShred = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = CardSlate
        )
    }
}

@Composable
fun DocItemCard(
    item: VaultItemEntity,
    onClick: () -> Unit,
    onShred: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardSlate),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderSlate.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("doc_item_${item.id}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(ContainerNavy)
            ) {
                Icon(
                    imageVector = when {
                        item.type == "NOTE" -> Icons.Default.EditNote
                        item.mimeType.contains("pdf", ignoreCase = true) -> Icons.Default.PictureAsPdf
                        else -> Icons.Default.Description
                    },
                    contentDescription = item.type,
                    tint = CyberCyan,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.title,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(ContainerNavy)
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = item.category,
                            color = CyberCyan,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                val dateStr = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(item.createdAt))
                Text(
                    text = "${item.type} • ${formatBytes(item.fileSizeBytes)} • $dateStr",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }

            IconButton(onClick = onShred) {
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = "Shred",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun CreateNoteDialog(
    onDismiss: () -> Unit,
    onSave: (title: String, content: String, category: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Confidential") }
    val categories = listOf("Confidential", "Financial", "Personal", "Work", "IDs")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardSlate),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Create Encrypted Note",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Encrypted in memory with AES-256 before disk write.",
                    color = CyberCyan,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Note Title", color = TextSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = BorderSlate,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("note_title_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Confidential Content", color = TextSecondary) },
                    minLines = 4,
                    maxLines = 8,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = BorderSlate,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("note_content_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text("Category:", color = TextSecondary, fontSize = 12.sp)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    categories.take(3).forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyberCyan,
                                selectedLabelColor = Color.Black,
                                containerColor = ContainerNavy,
                                labelColor = TextSecondary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = TextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isNotBlank() && content.isNotBlank()) {
                                onSave(title, content, selectedCategory)
                            }
                        },
                        enabled = title.isNotBlank() && content.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("save_note_button")
                    ) {
                        Text("Save Encrypted", color = ShieldNavy, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ViewVaultItemDialog(
    item: VaultItemEntity,
    vaultManager: VaultManager,
    onDismiss: () -> Unit,
    onShred: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var decryptedContent by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(item.id) {
        if (item.type == "NOTE") {
            decryptedContent = vaultManager.decryptNoteContent(item)
        } else {
            // For text documents, try to decode as text
            val bytes = vaultManager.decryptDocumentBytes(item)
            if (bytes != null && (item.mimeType.contains("text") || item.originalFileName.endsWith(".txt"))) {
                decryptedContent = String(bytes, Charsets.UTF_8)
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardSlate),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${item.type} • ${item.category}",
                            color = CyberCyan,
                            fontSize = 12.sp
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (decryptedContent != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(ContainerNavy)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = decryptedContent!!,
                            color = TextPrimary,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Vault Note", decryptedContent)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                            }
                            .padding(vertical = 4.dp)
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", tint = CyberCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy to clipboard", color = CyberCyan, fontSize = 12.sp)
                    }
                } else {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(ContainerNavy)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = item.originalFileName, color = TextPrimary, fontSize = 13.sp)
                            Text(text = "Encrypted binary file (${formatBytes(item.fileSizeBytes)})", color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (item.type != "NOTE") {
                        Button(
                            onClick = {
                                scope.launch {
                                    val bytes = vaultManager.decryptDocumentBytes(item) ?: return@launch
                                    val exportDir = File(context.getExternalFilesDir(null), "Aegis_Restored_Docs").apply { mkdirs() }
                                    val destFile = File(exportDir, item.originalFileName)
                                    FileOutputStream(destFile).use { it.write(bytes) }
                                    Toast.makeText(context, "Exported to: ${destFile.name}", Toast.LENGTH_LONG).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ContainerNavy),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Export", color = CyberCyan, fontSize = 12.sp)
                        }
                    }

                    Button(
                        onClick = onShred,
                        colors = ButtonDefaults.buttonColors(containerColor = ThreatCrimson),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Shred File", color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
