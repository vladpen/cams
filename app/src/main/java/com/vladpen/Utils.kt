package com.vladpen

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.util.DisplayMetrics
import android.util.Log
import com.vladpen.cams.MainApp.Companion.context
import java.lang.Integer.max
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

const val KEY_LEN = 16

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

    fun isUrlLocal(url: String): Boolean {
        val parsedUrl = parseUrl(url)
        if (parsedUrl == null || parsedUrl.host == "")
            return false
        if (parsedUrl.host.startsWith("192.") || parsedUrl.host.startsWith("10."))
            return true
        return false
    }

    fun replacePassword(url: String, replacement: String): String {
        val parsedUrl = parseUrl(url) ?: return url
        var prefix = ""
        if (parsedUrl.scheme != "")
            prefix = "${parsedUrl.scheme}://"
        if (parsedUrl.user != "")
            prefix += parsedUrl.user

        return ".+@".toRegex().replace(url, "$prefix:$replacement@")
    }

    fun decodeUrl(url: String): String {
        val parts = parseUrl(url) ?: return url
        if (parts.password == "")
            return url
        val password = decodeString(parts.password)
        return replacePassword(url, password)
    }

    fun encodeString(str: String, key: String? = null): String {
        if (str == "") return str
        try {
            val cipher = Cipher.getInstance(transformation)
            cipher.init(Cipher.ENCRYPT_MODE, getKey(key), getIv(key))
            return cipher.doFinal(str.toByteArray()).toHexString()

        } catch (e: java.lang.Exception) {
            Log.e("Utils", "Can't encrypt password (${e.localizedMessage})")
        }
        return str
    }

    fun decodeString(str: String, key: String? = null): String {
        if (str == "") return str
        try {
            val encoded = str.decodeHex()

            val cipher = Cipher.getInstance(transformation)
            cipher.init(Cipher.DECRYPT_MODE, getKey(key), getIv(key))
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

    private fun getKey(key: String? = null): SecretKey {
        if (key != null)
            return getKeySpec(key)

        var installationKey = "...secret.key..."
        try {
            val info = getPackageInfo()
            installationKey = (info.applicationInfo.uid.toString() + info.firstInstallTime)
        } catch (e: java.lang.Exception) {
            Log.e("Utils", "Key: can't get package info (${e.localizedMessage})")
        }
        return getKeySpec(installationKey)
    }

    private fun getKeySpec(key: String): SecretKeySpec {
        val out = key.padStart(KEY_LEN, '.').takeLast(KEY_LEN)
        return SecretKeySpec(out.toByteArray(), 0, KEY_LEN, "AES")
    }

    private fun getIv(iv: String? = null): IvParameterSpec {
        if (iv != null)
            return getIvSpec(iv)

        var installationIv = ".initial.vector."
        try {
            val info = getPackageInfo()
            installationIv = (info.applicationInfo.uid.toString() + info.firstInstallTime)
        } catch (e: java.lang.Exception) {
            Log.e("Utils", "IV: can't get package info (${e.localizedMessage})")
        }
        return getIvSpec(installationIv)
    }

    private fun getIvSpec(iv: String): IvParameterSpec {
        val out = iv.padEnd(KEY_LEN, '.').slice(0 until KEY_LEN)
        return IvParameterSpec(out.toByteArray())
    }

    fun getPackageInfo(): PackageInfo {
        if (::packageInfo.isInitialized)
            return packageInfo
        return context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_META_DATA
        )
    }

    fun getColumnCount(metrics: DisplayMetrics, columnSymbolCount: Int = 30): Int {
        val textSize = 20 // sp
        val screenSymbolsCount = metrics.widthPixels / metrics.scaledDensity / textSize * 2
        return max(1, screenSymbolsCount.toInt() / columnSymbolCount)
    }
}