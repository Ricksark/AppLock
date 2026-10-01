package com.example.crypto

import android.content.Context
import android.util.Base64
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object CryptoManager {
    private const val ALGORITHM = "AES"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128
    private const val PBKDF2_ITERATIONS = 20000
    private const val KEY_LENGTH = 256

    private val secureRandom = SecureRandom()

    /**
     * Generate a cryptographic random salt of given byte length
     */
    fun generateSalt(length: Int = 16): ByteArray {
        val salt = ByteArray(length)
        secureRandom.nextBytes(salt)
        return salt
    }

    fun saltToBase64(salt: ByteArray): String = Base64.encodeToString(salt, Base64.NO_WRAP)
    fun base64ToSalt(base64: String): ByteArray = Base64.decode(base64, Base64.NO_WRAP)

    /**
     * Hash passcode using PBKDF2WithHmacSHA256
     */
    fun hashPasscode(passcode: String, salt: ByteArray): String {
        val spec = PBEKeySpec(passcode.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hash = factory.generateSecret(spec).encoded
        return Base64.encodeToString(hash, Base64.NO_WRAP)
    }

    fun verifyPasscode(passcode: String, storedHash: String, salt: ByteArray): Boolean {
        val computedHash = hashPasscode(passcode, salt)
        return MessageDigest.isEqual(
            Base64.decode(computedHash, Base64.NO_WRAP),
            Base64.decode(storedHash, Base64.NO_WRAP)
        )
    }

    /**
     * Derive an AES-256 SecretKey from master passcode and salt
     */
    fun deriveKey(passcode: String, salt: ByteArray): SecretKey {
        val spec = PBEKeySpec(passcode.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, ALGORITHM)
    }

    /**
     * Encrypt an input stream to a destination file using AES-256-GCM.
     * The file format starts with the 12-byte IV followed by the encrypted ciphertext.
     */
    fun encryptToFile(inputStream: InputStream, destinationFile: File, secretKey: SecretKey): Long {
        val iv = ByteArray(GCM_IV_LENGTH)
        secureRandom.nextBytes(iv)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val parameterSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec)

        var totalBytes = 0L
        FileOutputStream(destinationFile).use { fos ->
            // Write IV header first
            fos.write(iv)
            CipherOutputStream(fos, cipher).use { cos ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    cos.write(buffer, 0, bytesRead)
                    totalBytes += bytesRead
                }
                cos.flush()
            }
        }
        return totalBytes
    }

    /**
     * Decrypt a vaulted file to ByteArray in memory (never written unencrypted to disk).
     */
    fun decryptFileToBytes(sourceFile: File, secretKey: SecretKey): ByteArray {
        FileInputStream(sourceFile).use { fis ->
            val iv = ByteArray(GCM_IV_LENGTH)
            val ivBytesRead = fis.read(iv)
            if (ivBytesRead != GCM_IV_LENGTH) {
                throw IllegalStateException("Invalid encrypted file header")
            }

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val parameterSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec)

            CipherInputStream(fis, cipher).use { cis ->
                return cis.readBytes()
            }
        }
    }

    /**
     * Encrypt a text string (e.g., confidential notes or database values)
     */
    fun encryptString(plainText: String, secretKey: SecretKey): String {
        val iv = ByteArray(GCM_IV_LENGTH)
        secureRandom.nextBytes(iv)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val parameterSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec)

        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val combined = ByteArray(iv.size + cipherText.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(cipherText, 0, combined, iv.size, cipherText.size)

        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    /**
     * Decrypt a text string
     */
    fun decryptString(encryptedBase64: String, secretKey: SecretKey): String {
        val combined = Base64.decode(encryptedBase64, Base64.NO_WRAP)
        if (combined.size < GCM_IV_LENGTH) {
            throw IllegalArgumentException("Invalid encrypted payload")
        }

        val iv = ByteArray(GCM_IV_LENGTH)
        val cipherText = ByteArray(combined.size - GCM_IV_LENGTH)
        System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH)
        System.arraycopy(combined, GCM_IV_LENGTH, cipherText, 0, cipherText.size)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val parameterSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec)

        val plainBytes = cipher.doFinal(cipherText)
        return String(plainBytes, Charsets.UTF_8)
    }

    /**
     * DoD 5220.22-M File Shredder:
     * Overwrites file contents multiple times with zeroes and random data before deletion.
     */
    fun secureShredFile(file: File): Boolean {
        if (!file.exists()) return true
        return try {
            val length = file.length()
            if (length > 0) {
                FileOutputStream(file).use { fos ->
                    // Pass 1: Zeroes
                    val zeroes = ByteArray(8192)
                    var written = 0L
                    while (written < length) {
                        val toWrite = minOf(zeroes.size.toLong(), length - written).toInt()
                        fos.write(zeroes, 0, toWrite)
                        written += toWrite
                    }
                    fos.flush()
                }
                FileOutputStream(file).use { fos ->
                    // Pass 2: Random bytes
                    val randomData = ByteArray(8192)
                    var written = 0L
                    while (written < length) {
                        secureRandom.nextBytes(randomData)
                        val toWrite = minOf(randomData.size.toLong(), length - written).toInt()
                        fos.write(randomData, 0, toWrite)
                        written += toWrite
                    }
                    fos.flush()
                }
            }
            file.delete()
        } catch (e: Exception) {
            file.delete()
        }
    }
}
