package com.vladpen.onvif

import java.net.URI

object ONVIFUrlParser {
    data class ParsedOnvifUrl(
        val serviceUrl: String,
        val credentials: ONVIFCredentials?
    )
    
    fun parseOnvifUrl(url: String): ParsedOnvifUrl? {
        return try {
            val uri = URI(url)
            val userInfo = uri.userInfo
            val credentials = if (userInfo != null && userInfo.contains(":")) {
                val parts = userInfo.split(":", limit = 2)
                ONVIFCredentials(parts[0], parts[1])
            } else null
            
            val serviceUrl = "http://${uri.host}:${uri.port}${uri.path}"
            ParsedOnvifUrl(serviceUrl, credentials)
        } catch (e: Exception) {
            null
        }
    }
}
