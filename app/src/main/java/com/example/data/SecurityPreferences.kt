package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.crypto.CryptoManager

class SecurityPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("aegis_security_prefs", Context.MODE_PRIVATE)

    var isSetupCompleted: Boolean
        get() = prefs.getBoolean(KEY_SETUP_COMPLETED, false)
        set(value) = prefs.edit().putBoolean(KEY_SETUP_COMPLETED, value).apply()

    var passcodeHash: String?
        get() = prefs.getString(KEY_PASSCODE_HASH, null)
        set(value) = prefs.edit().putString(KEY_PASSCODE_HASH, value).apply()

    var passcodeSalt: String?
        get() = prefs.getString(KEY_PASSCODE_SALT, null)
        set(value) = prefs.edit().putString(KEY_PASSCODE_SALT, value).apply()

    var passcodeLength: Int
        get() = prefs.getInt(KEY_PASSCODE_LENGTH, 4)
        set(value) = prefs.edit().putInt(KEY_PASSCODE_LENGTH, value).apply()

    var isBiometricEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, value).apply()

    var duressPasscodeHash: String?
        get() = prefs.getString(KEY_DURESS_HASH, null)
        set(value) = prefs.edit().putString(KEY_DURESS_HASH, value).apply()

    var duressPasscodeSalt: String?
        get() = prefs.getString(KEY_DURESS_SALT, null)
        set(value) = prefs.edit().putString(KEY_DURESS_SALT, value).apply()

    var remoteWipeSecret: String
        get() = prefs.getString(KEY_REMOTE_WIPE_SECRET, "AEGIS#WIPE#9911") ?: "AEGIS#WIPE#9911"
        set(value) = prefs.edit().putString(KEY_REMOTE_WIPE_SECRET, value).apply()

    var remoteWipeEnabled: Boolean
        get() = prefs.getBoolean(KEY_REMOTE_WIPE_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_REMOTE_WIPE_ENABLED, value).apply()

    var maxFailedAttempts: Int
        get() = prefs.getInt(KEY_MAX_FAILED_ATTEMPTS, 5)
        set(value) = prefs.edit().putInt(KEY_MAX_FAILED_ATTEMPTS, value).apply()

    var currentFailedAttempts: Int
        get() = prefs.getInt(KEY_CURRENT_FAILED_ATTEMPTS, 0)
        set(value) = prefs.edit().putInt(KEY_CURRENT_FAILED_ATTEMPTS, value).apply()

    var isAppLockProtectionEnabled: Boolean
        get() = prefs.getBoolean(KEY_APPLOCK_PROTECTION_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_APPLOCK_PROTECTION_ENABLED, value).apply()

    var autoLockDelaySeconds: Int
        get() = prefs.getInt(KEY_AUTO_LOCK_DELAY, 0)
        set(value) = prefs.edit().putInt(KEY_AUTO_LOCK_DELAY, value).apply()

    var securityHint: String?
        get() = prefs.getString(KEY_SECURITY_HINT, null)
        set(value) = prefs.edit().putString(KEY_SECURITY_HINT, value).apply()

    var lastUnlockTimestamp: Long
        get() = prefs.getLong(KEY_LAST_UNLOCK_TIMESTAMP, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_UNLOCK_TIMESTAMP, value).apply()

    /**
     * Configure or update Master Passcode
     */
    fun setMasterPasscode(passcode: String, length: Int = passcode.length) {
        val salt = CryptoManager.generateSalt()
        val hash = CryptoManager.hashPasscode(passcode, salt)
        prefs.edit()
            .putString(KEY_PASSCODE_HASH, hash)
            .putString(KEY_PASSCODE_SALT, CryptoManager.saltToBase64(salt))
            .putInt(KEY_PASSCODE_LENGTH, length)
            .putBoolean(KEY_SETUP_COMPLETED, true)
            .putInt(KEY_CURRENT_FAILED_ATTEMPTS, 0)
            .apply()
    }

    /**
     * Check if entered passcode matches Master Passcode
     */
    fun verifyMasterPasscode(enteredPasscode: String): Boolean {
        val hash = passcodeHash ?: return false
        val saltBase64 = passcodeSalt ?: return false
        val salt = CryptoManager.base64ToSalt(saltBase64)
        val valid = CryptoManager.verifyPasscode(enteredPasscode, hash, salt)
        if (valid) {
            currentFailedAttempts = 0
            lastUnlockTimestamp = System.currentTimeMillis()
        } else {
            currentFailedAttempts += 1
        }
        return valid
    }

    /**
     * Configure a Duress / Panic Passcode.
     * When entered, this signals coercion and triggers silent wipe/decoy.
     */
    fun setDuressPasscode(passcode: String) {
        val salt = CryptoManager.generateSalt()
        val hash = CryptoManager.hashPasscode(passcode, salt)
        prefs.edit()
            .putString(KEY_DURESS_HASH, hash)
            .putString(KEY_DURESS_SALT, CryptoManager.saltToBase64(salt))
            .apply()
    }

    fun isDuressPasscode(enteredPasscode: String): Boolean {
        val hash = duressPasscodeHash ?: return false
        val saltBase64 = duressPasscodeSalt ?: return false
        val salt = CryptoManager.base64ToSalt(saltBase64)
        return CryptoManager.verifyPasscode(enteredPasscode, hash, salt)
    }

    fun resetAll() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_SETUP_COMPLETED = "setup_completed"
        private const val KEY_PASSCODE_HASH = "passcode_hash"
        private const val KEY_PASSCODE_SALT = "passcode_salt"
        private const val KEY_PASSCODE_LENGTH = "passcode_length"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_DURESS_HASH = "duress_hash"
        private const val KEY_DURESS_SALT = "duress_salt"
        private const val KEY_REMOTE_WIPE_SECRET = "remote_wipe_secret"
        private const val KEY_REMOTE_WIPE_ENABLED = "remote_wipe_enabled"
        private const val KEY_MAX_FAILED_ATTEMPTS = "max_failed_attempts"
        private const val KEY_CURRENT_FAILED_ATTEMPTS = "current_failed_attempts"
        private const val KEY_APPLOCK_PROTECTION_ENABLED = "applock_protection_enabled"
        private const val KEY_AUTO_LOCK_DELAY = "auto_lock_delay"
        private const val KEY_SECURITY_HINT = "security_hint"
        private const val KEY_LAST_UNLOCK_TIMESTAMP = "last_unlock_timestamp"
    }
}
