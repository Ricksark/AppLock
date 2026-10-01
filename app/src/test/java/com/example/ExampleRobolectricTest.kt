package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.crypto.CryptoManager
import com.example.data.SecurityPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun readStringFromContext() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("AegisLock", appName)
    }

    @Test
    fun testPasscodeHashAndVerification() {
        val salt = CryptoManager.generateSalt()
        val pin = "1234"
        val hash = CryptoManager.hashPasscode(pin, salt)

        assertTrue(CryptoManager.verifyPasscode("1234", hash, salt))
        assertFalse(CryptoManager.verifyPasscode("9999", hash, salt))
    }

    @Test
    fun testAesStringEncryptionDecryption() {
        val salt = CryptoManager.generateSalt()
        val key = CryptoManager.deriveKey("mySecretPin99", salt)
        val plainText = "Top-Secret Confidential Note Content"

        val encrypted = CryptoManager.encryptString(plainText, key)
        val decrypted = CryptoManager.decryptString(encrypted, key)

        assertEquals(plainText, decrypted)
    }

    @Test
    fun testSecurityPreferences() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = SecurityPreferences(context)
        prefs.setMasterPasscode("7890", 4)

        assertTrue(prefs.verifyMasterPasscode("7890"))
        assertFalse(prefs.verifyMasterPasscode("1111"))
    }
}
