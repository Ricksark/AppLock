package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_locks")
data class AppLockEntity(
    @PrimaryKey val packageName: String,
    val appName: String,
    val isLocked: Boolean = true,
    val category: String = "GENERAL",
    val lockedAt: Long = System.currentTimeMillis(),
    val unlockCount: Int = 0,
    val isBiometricAllowed: Boolean = true
)

@Entity(tableName = "vault_items")
data class VaultItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val type: String, // "PHOTO", "DOCUMENT", "NOTE"
    val originalFileName: String,
    val encryptedFilePath: String,
    val fileSizeBytes: Long,
    val mimeType: String,
    val category: String = "Confidential", // "Confidential", "Financial", "Personal", "Work", "IDs"
    val createdAt: Long = System.currentTimeMillis(),
    val noteContentEncrypted: String? = null
)

@Entity(tableName = "security_logs")
data class SecurityLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventType: String, // "UNLOCK_SUCCESS", "UNLOCK_FAILED", "PHOTO_VAULTED", "DOC_VAULTED", "APP_LOCKED", "REMOTE_WIPE_SIMULATED", "REMOTE_WIPE_EXECUTED", "SECURITY_ALERT"
    val details: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isAlert: Boolean = false
)
