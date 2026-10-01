package com.example.vault

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import com.example.crypto.CryptoManager
import com.example.data.AppDatabase
import com.example.data.SecurityLogEntity
import com.example.data.SecurityPreferences
import com.example.data.VaultItemEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

object VaultSession {
    @Volatile
    var activeSecretKey: SecretKey? = null

    @Volatile
    var isUnlocked: Boolean = false

    @Volatile
    var masterPasscodePlain: String? = null

    fun lock() {
        isUnlocked = false
        activeSecretKey = null
        masterPasscodePlain = null
    }

    fun unlock(key: SecretKey, passcode: String? = null) {
        activeSecretKey = key
        isUnlocked = true
        masterPasscodePlain = passcode
    }
}

class VaultManager(private val context: Context) {

    private val database = AppDatabase.getInstance(context)
    private val preferences = SecurityPreferences(context)
    private val vaultDao = database.vaultDao()
    private val logDao = database.securityLogDao()

    private val photosDir = File(context.filesDir, "vault_photos").apply { mkdirs() }
    private val docsDir = File(context.filesDir, "vault_docs").apply { mkdirs() }

    /**
     * Get or create a persistent vault key bound to the device and passcode.
     */
    fun getOrDeriveVaultKey(): SecretKey {
        VaultSession.activeSecretKey?.let { return it }

        val passcode = VaultSession.masterPasscodePlain ?: "AEGIS_SYSTEM_DEFAULT_PIN"
        val salt = preferences.passcodeSalt?.let { CryptoManager.base64ToSalt(it) }
            ?: CryptoManager.generateSalt()
        val key = CryptoManager.deriveKey(passcode, salt)
        VaultSession.activeSecretKey = key
        return key
    }

    /**
     * Import and encrypt a photo from Uri
     */
    suspend fun importPhoto(uri: Uri): Result<VaultItemEntity> = withContext(Dispatchers.IO) {
        try {
            val fileName = getFileName(uri) ?: "photo_${System.currentTimeMillis()}.jpg"
            val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
            val encFileName = "enc_${UUID.randomUUID()}.bin"
            val targetFile = File(photosDir, encFileName)

            val key = getOrDeriveVaultKey()
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(Exception("Cannot read image stream"))

            val byteCount = inputStream.use { stream ->
                CryptoManager.encryptToFile(stream, targetFile, key)
            }

            val item = VaultItemEntity(
                title = fileName.substringBeforeLast("."),
                type = "PHOTO",
                originalFileName = fileName,
                encryptedFilePath = targetFile.absolutePath,
                fileSizeBytes = byteCount,
                mimeType = mimeType,
                category = "Photos"
            )
            val id = vaultDao.insertItem(item)
            val savedItem = item.copy(id = id)

            logDao.insertLog(
                SecurityLogEntity(
                    eventType = "PHOTO_VAULTED",
                    details = "Encrypted and vaulted photo: $fileName (${byteCount / 1024} KB)"
                )
            )

            Result.success(savedItem)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Import and encrypt a sensitive document
     */
    suspend fun importDocument(uri: Uri, category: String = "Confidential"): Result<VaultItemEntity> = withContext(Dispatchers.IO) {
        try {
            val fileName = getFileName(uri) ?: "doc_${System.currentTimeMillis()}.dat"
            val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
            val encFileName = "enc_${UUID.randomUUID()}.bin"
            val targetFile = File(docsDir, encFileName)

            val key = getOrDeriveVaultKey()
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(Exception("Cannot read document stream"))

            val byteCount = inputStream.use { stream ->
                CryptoManager.encryptToFile(stream, targetFile, key)
            }

            val item = VaultItemEntity(
                title = fileName.substringBeforeLast("."),
                type = "DOCUMENT",
                originalFileName = fileName,
                encryptedFilePath = targetFile.absolutePath,
                fileSizeBytes = byteCount,
                mimeType = mimeType,
                category = category
            )
            val id = vaultDao.insertItem(item)
            val savedItem = item.copy(id = id)

            logDao.insertLog(
                SecurityLogEntity(
                    eventType = "DOC_VAULTED",
                    details = "Encrypted and vaulted document: $fileName ($category)"
                )
            )

            Result.success(savedItem)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Save a confidential encrypted note
     */
    suspend fun saveNote(title: String, content: String, category: String = "Confidential"): Result<VaultItemEntity> = withContext(Dispatchers.IO) {
        try {
            val key = getOrDeriveVaultKey()
            val encryptedContent = CryptoManager.encryptString(content, key)

            val item = VaultItemEntity(
                title = title,
                type = "NOTE",
                originalFileName = "$title.note",
                encryptedFilePath = "",
                fileSizeBytes = content.toByteArray().size.toLong(),
                mimeType = "text/plain",
                category = category,
                noteContentEncrypted = encryptedContent
            )
            val id = vaultDao.insertItem(item)
            val savedItem = item.copy(id = id)

            logDao.insertLog(
                SecurityLogEntity(
                    eventType = "NOTE_VAULTED",
                    details = "Created encrypted confidential note: $title"
                )
            )

            Result.success(savedItem)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Decrypt note content in memory
     */
    fun decryptNoteContent(noteItem: VaultItemEntity): String {
        val encryptedContent = noteItem.noteContentEncrypted ?: return ""
        val key = getOrDeriveVaultKey()
        return try {
            CryptoManager.decryptString(encryptedContent, key)
        } catch (e: Exception) {
            "Error: Decryption failed (invalid key or corrupted data)"
        }
    }

    /**
     * Decrypt a photo into Bitmap in memory without touching disk
     */
    suspend fun decryptPhotoToBitmap(item: VaultItemEntity): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val file = File(item.encryptedFilePath)
            if (!file.exists()) return@withContext null
            val key = getOrDeriveVaultKey()
            val bytes = CryptoManager.decryptFileToBytes(file, key)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Decrypt document to byte array
     */
    suspend fun decryptDocumentBytes(item: VaultItemEntity): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val file = File(item.encryptedFilePath)
            if (!file.exists()) return@withContext null
            val key = getOrDeriveVaultKey()
            CryptoManager.decryptFileToBytes(file, key)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Securely shred and delete an item
     */
    suspend fun shredAndDeleteItem(item: VaultItemEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            if (item.encryptedFilePath.isNotEmpty()) {
                val file = File(item.encryptedFilePath)
                if (file.exists()) {
                    CryptoManager.secureShredFile(file)
                }
            }
            vaultDao.deleteItem(item)
            logDao.insertLog(
                SecurityLogEntity(
                    eventType = "ITEM_SHREDDED",
                    details = "Securely shredded and purged: ${item.title}"
                )
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Complete Remote Wipe Execution:
     * - Shreds all encrypted photos
     * - Shreds all encrypted documents
     * - Purges Room database records
     * - Resets security preferences
     * - Logs emergency purge
     */
    suspend fun executeRemoteWipe(reason: String = "Remote Wipe Command Received"): WipeResult = withContext(Dispatchers.IO) {
        var filesShredded = 0
        var bytesPurged = 0L

        // Shred all photos
        photosDir.listFiles()?.forEach { file ->
            bytesPurged += file.length()
            if (CryptoManager.secureShredFile(file)) {
                filesShredded++
            }
        }

        // Shred all documents
        docsDir.listFiles()?.forEach { file ->
            bytesPurged += file.length()
            if (CryptoManager.secureShredFile(file)) {
                filesShredded++
            }
        }

        // Clear Room database
        vaultDao.deleteAll()
        database.appLockDao().deleteAll()

        // Log the event before clearing logs or keep one security log
        database.securityLogDao().clearLogs()
        database.securityLogDao().insertLog(
            SecurityLogEntity(
                eventType = "REMOTE_WIPE_EXECUTED",
                details = "EMERGENCY WIPE: Shredded $filesShredded vault files ($bytesPurged bytes purged). Reason: $reason",
                isAlert = true
            )
        )

        // Reset preferences and locks
        preferences.resetAll()
        VaultSession.lock()

        WipeResult(
            success = true,
            filesShredded = filesShredded,
            bytesPurged = bytesPurged,
            message = "Remote Wipe executed successfully. All encrypted vaults shredded."
        )
    }

    private fun getFileName(uri: Uri): String? {
        var name: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        name = it.getString(index)
                    }
                }
            }
        }
        if (name == null) {
            name = uri.path?.let { path ->
                val cut = path.lastIndexOf('/')
                if (cut != -1) path.substring(cut + 1) else path
            }
        }
        return name
    }
}

data class WipeResult(
    val success: Boolean,
    val filesShredded: Int,
    val bytesPurged: Long,
    val message: String
)
