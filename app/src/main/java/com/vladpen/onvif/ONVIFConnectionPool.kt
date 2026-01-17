package com.vladpen.onvif

import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

class ONVIFConnectionPool private constructor() {
    companion object {
        @Volatile
        private var INSTANCE: ONVIFConnectionPool? = null
        
        fun getInstance(): ONVIFConnectionPool {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ONVIFConnectionPool().also { INSTANCE = it }
            }
        }
    }

    private val connections = ConcurrentHashMap<String, PooledConnection>()
    private val executor = Executors.newFixedThreadPool(4)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private data class PooledConnection(
        val client: ONVIFSoapClient,
        var lastUsed: Long,
        var isInUse: Boolean = false
    )

    fun getConnection(serviceUrl: String, credentials: ONVIFCredentials?): ONVIFSoapClient {
        val key = "$serviceUrl:${credentials?.username ?: "anonymous"}"
        
        return synchronized(connections) {
            val existing = connections[key]
            
            if (existing != null && !existing.isInUse) {
                existing.isInUse = true
                existing.lastUsed = System.currentTimeMillis()
                existing.client
            } else {
                // Create new connection
                val client = ONVIFSoapClient(serviceUrl, credentials)
                val pooled = PooledConnection(client, System.currentTimeMillis(), true)
                connections[key] = pooled
                client
            }
        }
    }

    fun releaseConnection(serviceUrl: String, credentials: ONVIFCredentials?) {
        val key = "$serviceUrl:${credentials?.username ?: "anonymous"}"
        
        synchronized(connections) {
            connections[key]?.let { connection ->
                connection.isInUse = false
                connection.lastUsed = System.currentTimeMillis()
            }
        }
    }

    fun cleanup() {
        scope.launch {
            val currentTime = System.currentTimeMillis()
            val timeout = 5 * 60 * 1000L // 5 minutes
            
            synchronized(connections) {
                val iterator = connections.iterator()
                while (iterator.hasNext()) {
                    val entry = iterator.next()
                    val connection = entry.value
                    
                    if (!connection.isInUse && (currentTime - connection.lastUsed) > timeout) {
                        iterator.remove()
                    }
                }
            }
        }
    }

    fun shutdown() {
        scope.cancel()
        executor.shutdown()
        connections.clear()
    }

    // Start periodic cleanup
    init {
        scope.launch {
            while (isActive) {
                delay(60000) // Cleanup every minute
                cleanup()
            }
        }
    }
}
