package com.vladpen.onvif

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

object ONVIFSecurity {
    private const val PREFS_NAME = "onvif_secure_prefs"
    private const val KEY_ALIAS = "onvif_encryption_key"
    private const val TRANSFORMATION = "AES/ECB/PKCS1Padding"

    fun encryptCredentials(context: Context, credentials: ONVIFCredentials): String {
        return try {
            val key = getOrCreateKey(context)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key)
            
            val credentialsJson = "${credentials.username}:${credentials.password}"
            val encryptedBytes = cipher.doFinal(credentialsJson.toByteArray())
            Base64.encodeToString(encryptedBytes, Base64.DEFAULT)
        } catch (e: Exception) {
            // Fallback to base64 encoding if encryption fails
            Base64.encodeToString("${credentials.username}:${credentials.password}".toByteArray(), Base64.DEFAULT)
        }
    }

    fun decryptCredentials(context: Context, encryptedData: String): ONVIFCredentials? {
        return try {
            val key = getOrCreateKey(context)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key)
            
            val encryptedBytes = Base64.decode(encryptedData, Base64.DEFAULT)
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            val credentialsString = String(decryptedBytes)
            
            val parts = credentialsString.split(":")
            if (parts.size == 2) {
                ONVIFCredentials(parts[0], parts[1])
            } else null
        } catch (e: Exception) {
            // Fallback to base64 decoding
            try {
                val decoded = String(Base64.decode(encryptedData, Base64.DEFAULT))
                val parts = decoded.split(":")
                if (parts.size == 2) {
                    ONVIFCredentials(parts[0], parts[1])
                } else null
            } catch (e2: Exception) {
                null
            }
        }
    }

    private fun getOrCreateKey(context: Context): SecretKey {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val keyString = prefs.getString(KEY_ALIAS, null)
        
        return if (keyString != null) {
            val keyBytes = Base64.decode(keyString, Base64.DEFAULT)
            SecretKeySpec(keyBytes, "AES")
        } else {
            // Generate new key
            val keyGenerator = KeyGenerator.getInstance("AES")
            keyGenerator.init(256)
            val key = keyGenerator.generateKey()
            
            // Store key
            val keyBytes = key.encoded
            val keyString64 = Base64.encodeToString(keyBytes, Base64.DEFAULT)
            prefs.edit().putString(KEY_ALIAS, keyString64).apply()
            
            key
        }
    }

    fun validateInput(input: String): String {
        // Basic input sanitization for ONVIF operations
        return input.replace(Regex("[<>&\"']"), "")
            .trim()
            .take(1000) // Limit length
    }

    fun isValidUrl(url: String): Boolean {
        return try {
            val uri = java.net.URI(url)
            uri.scheme in listOf("http", "https") && uri.host != null
        } catch (e: Exception) {
            false
        }
    }

    fun sanitizeDeviceResponse(response: String): String {
        // Remove potentially dangerous XML content
        return response.replace(Regex("<script[^>]*>.*?</script>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("<[^>]*javascript:[^>]*>", RegexOption.IGNORE_CASE), "")
    }
}
