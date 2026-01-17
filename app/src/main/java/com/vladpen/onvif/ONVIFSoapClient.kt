package com.vladpen.onvif

import org.ksoap2.SoapEnvelope
import org.ksoap2.serialization.SoapObject
import org.ksoap2.serialization.SoapSerializationEnvelope
import org.ksoap2.transport.HttpTransportSE
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*

class ONVIFSoapClient(
    private val serviceUrl: String,
    private val credentials: ONVIFCredentials?
) {
    companion object {
        private const val TIMEOUT = 10000
        private const val ONVIF_NAMESPACE = "http://www.onvif.org/ver10/device/wsdl"
    }

    fun sendRequest(method: String, namespace: String = ONVIF_NAMESPACE, parameters: Map<String, Any> = emptyMap()): SoapObject? {
        return try {
            val request = SoapObject(namespace, method)
            
            // Add parameters
            parameters.forEach { (key, value) ->
                request.addProperty(key, value)
            }

            val envelope = SoapSerializationEnvelope(SoapEnvelope.VER11)
            envelope.setOutputSoapObject(request)

            // Add WS-Security header if credentials provided
            credentials?.let { addWSSecurityHeader(envelope, it) }

            val transport = HttpTransportSE(serviceUrl, TIMEOUT)
            transport.call("$namespace#$method", envelope)

            envelope.response as? SoapObject
        } catch (e: Exception) {
            null
        }
    }

    private fun addWSSecurityHeader(envelope: SoapSerializationEnvelope, creds: ONVIFCredentials) {
        val created = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date())
        val nonce = UUID.randomUUID().toString().replace("-", "").substring(0, 16)
        val digest = createPasswordDigest(nonce, created, creds.password)

        val security = SoapObject("http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd", "Security")
        val usernameToken = SoapObject("http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd", "UsernameToken")
        
        usernameToken.addProperty("Username", creds.username)
        usernameToken.addProperty("Password", digest)
        usernameToken.addProperty("Nonce", nonce)
        usernameToken.addProperty("Created", created)
        
        security.addSoapObject(usernameToken)
        envelope.headerOut = arrayOf(security)
    }

    private fun createPasswordDigest(nonce: String, created: String, password: String): String {
        val digest = MessageDigest.getInstance("SHA-1")
        val input = (nonce + created + password).toByteArray()
        return Base64.getEncoder().encodeToString(digest.digest(input))
    }
}
