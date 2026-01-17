package com.vladpen.onvif

import kotlinx.coroutines.*
import java.io.StringWriter
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.ByteArrayInputStream

class ONVIFSoapClient(
    private val serviceUrl: String,
    private val credentials: ONVIFCredentials?
) {
    companion object {
        private const val TIMEOUT = 10000
        private const val ONVIF_NAMESPACE = "http://www.onvif.org/ver10/device/wsdl"
    }

    fun sendRequest(method: String, namespace: String = ONVIF_NAMESPACE, parameters: Map<String, Any> = emptyMap()): ONVIFResponse? {
        return try {
            // Validate inputs
            val safeMethod = ONVIFSecurity.validateInput(method)
            val safeNamespace = ONVIFSecurity.validateInput(namespace)
            
            if (!ONVIFSecurity.isValidUrl(serviceUrl)) {
                return null
            }

            val soapEnvelope = createSoapEnvelope(safeMethod, safeNamespace, parameters)
            val response = sendHttpRequest(soapEnvelope)
            
            response?.let { parseResponse(it) }
        } catch (e: Exception) {
            null
        }
    }

    private fun createSoapEnvelope(method: String, namespace: String, parameters: Map<String, Any>): String {
        val securityHeader = credentials?.let { createWSSecurityHeader(it) } ?: ""
        
        val parameterXml = parameters.entries.joinToString("") { (key, value) ->
            "<$key>$value</$key>"
        }

        return """<?xml version="1.0" encoding="UTF-8"?>
<soap:Envelope xmlns:soap="http://www.w3.org/2003/05/soap-envelope" xmlns:tds="$namespace">
    <soap:Header>
        $securityHeader
    </soap:Header>
    <soap:Body>
        <tds:$method>
            $parameterXml
        </tds:$method>
    </soap:Body>
</soap:Envelope>"""
    }

    private fun createWSSecurityHeader(creds: ONVIFCredentials): String {
        val created = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date())
        val nonce = UUID.randomUUID().toString().replace("-", "").substring(0, 16)
        val digest = createPasswordDigest(nonce, created, creds.password)

        return """
        <wsse:Security xmlns:wsse="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd">
            <wsse:UsernameToken>
                <wsse:Username>${creds.username}</wsse:Username>
                <wsse:Password Type="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordDigest">$digest</wsse:Password>
                <wsse:Nonce EncodingType="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary">$nonce</wsse:Nonce>
                <wsu:Created xmlns:wsu="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd">$created</wsu:Created>
            </wsse:UsernameToken>
        </wsse:Security>
        """.trimIndent()
    }

    private fun createPasswordDigest(nonce: String, created: String, password: String): String {
        val digest = MessageDigest.getInstance("SHA-1")
        val input = (nonce + created + password).toByteArray()
        return Base64.getEncoder().encodeToString(digest.digest(input))
    }

    private fun sendHttpRequest(soapEnvelope: String): String? {
        return try {
            val url = URL(serviceUrl)
            val connection = url.openConnection() as HttpURLConnection
            
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/soap+xml; charset=utf-8")
            connection.setRequestProperty("SOAPAction", "")
            connection.connectTimeout = TIMEOUT
            connection.readTimeout = TIMEOUT
            connection.doOutput = true

            connection.outputStream.use { output ->
                output.write(soapEnvelope.toByteArray())
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.use { input ->
                    input.bufferedReader().readText()
                }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun parseResponse(xmlResponse: String): ONVIFResponse? {
        return try {
            val sanitized = ONVIFSecurity.sanitizeDeviceResponse(xmlResponse)
            val factory = DocumentBuilderFactory.newInstance()
            val builder = factory.newDocumentBuilder()
            val document = builder.parse(ByteArrayInputStream(sanitized.toByteArray()))
            
            ONVIFResponse(document)
        } catch (e: Exception) {
            null
        }
    }
}

class ONVIFResponse(private val document: Document) {
    fun getProperty(name: String): String? {
        return try {
            val elements = document.getElementsByTagName(name)
            if (elements.length > 0) {
                elements.item(0).textContent
            } else null
        } catch (e: Exception) {
            null
        }
    }

    fun getPropertyAsString(name: String): String? = getProperty(name)

    override fun toString(): String {
        return try {
            val transformer = javax.xml.transform.TransformerFactory.newInstance().newTransformer()
            val source = javax.xml.transform.dom.DOMSource(document)
            val writer = StringWriter()
            val result = javax.xml.transform.stream.StreamResult(writer)
            transformer.transform(source, result)
            writer.toString()
        } catch (e: Exception) {
            ""
        }
    }
}
