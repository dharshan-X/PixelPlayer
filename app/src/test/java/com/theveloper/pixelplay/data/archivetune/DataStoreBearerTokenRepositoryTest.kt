package com.theveloper.pixelplay.data.archivetune

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DataStoreBearerTokenRepositoryTest {
    @Test
    fun testTokenSetGetAndClear() = runBlocking {
        val repository = InMemoryBearerTokenRepository()
        assertNull(repository.getToken())
        repository.setToken("test-bearer-token")
        assertEquals("test-bearer-token", repository.getToken())
        repository.clearToken()
        assertNull(repository.getToken())
    }
}
