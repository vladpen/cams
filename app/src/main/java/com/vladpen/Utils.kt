package com.vladpen

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.util.Log
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

data class UrlDataModel(
    val scheme: String,
    val user: String,
    val password: String,
    val host: String,
    val port: Int,
    val path: String
)

object Utils {
    private const val transformation = "AES/CBC/PKCS7Padding"
    private lateinit var packageInfo: PackageInfo

    fun parseUrl(url: String?, defaultPort: Int = 554): UrlDataModel? {
        if (url == null)
            return null
        try {
            val rex = "((.+?)://)?((.+?)(:(.+))?@)?(.+?)(:(\\d+))?(/.*)?".toRegex()
            val res = rex.matchEntire(url) ?: return null
            val r = res.groupValues
            return UrlDataModel(
                r[2],
                r[4],
                r[6],
                r[7],
                r[9].toIntOrNull() ?: defaultPort,
                if (r[10] != "" && r[10].last().toString() == "/") r[10] else r[10] + "/"
            )
        } catch (e: Exception) {
            Log.e("Utils", "Can't parse URL $url (${e.localizedMessage})")
        }
        return null
    }

    fun replacePassword(url: String, replacement: String): String {
        return ":.+@".toRegex().replace(url, ":$replacement@")
    }

    fun decodeUrl(context: Context, url: String): String {
        val parts = parseUrl(url) ?: return url
        if (parts.password == "")
            return url
        val password = decodeString(context, parts.password)
        return replacePassword(url, password)
    }

    fun encodeString(context: Context, str: String): String {
        if (str == "") return str
        try {
            val cipher = Cipher.getInstance(transformation)
            cipher.init(Cipher.ENCRYPT_MODE, getKey(context), getIv(context))
            return cipher.doFinal(str.toByteArray()).toHexString()

        } catch (e: java.lang.Exception) {
            Log.e("Utils", "Can't encrypt password (${e.localizedMessage})")
        }
        return str
    }

    fun decodeString(context: Context, str: String): String {
        if (str == "") return str
        try {
            val encoded = str.decodeHex()

            val cipher = Cipher.getInstance(transformation)
            cipher.init(Cipher.DECRYPT_MODE, getKey(context), getIv(context))
            return String(cipher.doFinal(encoded))

        } catch (e: java.lang.Exception) {
            Log.e("Utils", "Can't decrypt password (${e.localizedMessage})")
        }
        return str
    }

    private fun ByteArray.toHexString() = joinToString("") { "%02x".format(it) }

    private fun String.decodeHex(): ByteArray {
        check(length % 2 == 0) { "Encoded string must have an even length" }
        return chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()
    }

    private fun getKey(context: Context): SecretKey {
        val len = 16
        var key = "...secret.key..." // $len bytes
        try {
            val info = getPackageInfo(context)
            key = (info.applicationInfo.uid.toString() + info.firstInstallTime)
                .padStart(len, '.')
                .takeLast(len)
        } catch (e: java.lang.Exception) {
            Log.e("Utils", "Key: can't get package info (${e.localizedMessage})")
        }
        return SecretKeySpec(key.toByteArray(), 0, len, "AES")
    }

    private fun getIv(context: Context): IvParameterSpec {
        val len = 16
        var iv = ".initial.vector." // $len bytes
        try {
            val info = getPackageInfo(context)
            iv = (info.applicationInfo.uid.toString() + info.firstInstallTime)
                .padEnd(len, '.')
                .slice(0 until len)
        } catch (e: java.lang.Exception) {
            Log.e("Utils", "IV: can't get package info (${e.localizedMessage})")
        }
        return IvParameterSpec(iv.toByteArray())
    }

    private fun getPackageInfo(context: Context): PackageInfo {
        if (::packageInfo.isInitialized)
            return packageInfo
        return context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_META_DATA
        )
    }
}