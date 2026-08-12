package com.theveloper.pixelplay.data.archivetune

import moe.rukamori.archivetune.moriextractor.BearerTokenRepository
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataStoreBearerTokenRepository @Inject constructor() : BearerTokenRepository {
    private val tokenRef = AtomicReference<String?>(null)

    override fun getToken(): String? = tokenRef.get()

    override fun updateToken(token: String) {
        tokenRef.set(token.trim().takeIf { it.isNotEmpty() })
    }

    fun setToken(token: String?) {
        if (token == null) {
            clearToken()
        } else {
            updateToken(token)
        }
    }

    override fun clearToken() {
        tokenRef.set(null)
    }
}

class InMemoryBearerTokenRepository : BearerTokenRepository {
    private var token: String? = null

    override fun getToken(): String? = token

    override fun updateToken(token: String) {
        this.token = token.trim().takeIf { it.isNotEmpty() }
    }

    fun setToken(newToken: String?) {
        if (newToken == null) {
            clearToken()
        } else {
            updateToken(newToken)
        }
    }

    override fun clearToken() {
        token = null
    }
}
