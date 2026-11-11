package com.skillacademy.mobile

import com.skillacademy.mobile.data.remote.ResilienceManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * ============================================================================
 * PRÁCTICA 12: Pruebas Unitarias de Resiliencia, Idempotencia y Exponential Backoff
 *
 * Cubre:
 *  ✔ Generación y unicidad de Idempotency-Key (UUID v4)
 *  ✔ Clasificación de errores no reintentables (400, 401, 403, 404)
 *  ✔ Clasificación de errores reintentables (502, 503, 504, Timeout)
 *  ✔ Reintento controlado con recuperación exitosa en intento 2 o 3
 *  ✔ Límite máximo de reintentos ante caída persistente
 * ============================================================================
 */
class ResilienceUnitTest {

    @Test
    fun testResilience_IdempotencyKey_IsUniqueAndHasPrefix() {
        val key1 = ResilienceManager.generateIdempotencyKey()
        val key2 = ResilienceManager.generateIdempotencyKey()

        assertTrue(key1.startsWith("IDEMP-"))
        assertTrue(key2.startsWith("IDEMP-"))
        assertNotEquals("Cada clave de idempotencia debe ser única", key1, key2)
    }

    @Test
    fun testResilience_ErrorClassification_NonRetryableCodes() {
        // Códigos 4xx de cliente no deben reintentarse
        assertFalse(ResilienceManager.isRetryableHttpCode(400))
        assertFalse(ResilienceManager.isRetryableHttpCode(401))
        assertFalse(ResilienceManager.isRetryableHttpCode(403))
        assertFalse(ResilienceManager.isRetryableHttpCode(404))
    }

    @Test
    fun testResilience_ErrorClassification_RetryableCodes() {
        // Códigos 5xx y 429 de servidor sí deben reintentarse
        assertTrue(ResilienceManager.isRetryableHttpCode(500))
        assertTrue(ResilienceManager.isRetryableHttpCode(502))
        assertTrue(ResilienceManager.isRetryableHttpCode(503))
        assertTrue(ResilienceManager.isRetryableHttpCode(504))
        assertTrue(ResilienceManager.isRetryableHttpCode(429))
    }

    @Test
    fun testResilience_ExponentialBackoff_RecoversOnSecondAttempt() = runBlocking {
        var attemptsCount = 0
        val policy = ResilienceManager.RetryPolicy(maxAttempts = 3, initialDelayMs = 50L)

        val result = ResilienceManager.executeWithBackoff(policy) { attempt, _ ->
            attemptsCount++
            if (attempt == 1) {
                throw SocketTimeoutException("Simulación de timeout transitorio")
            }
            "Recuperación Exitosa"
        }

        assertTrue(result.isSuccess)
        assertEquals(2, result.attemptsMade)
        assertEquals("Recuperación Exitosa", result.data)
        assertTrue(result.idempotencyKey.isNotBlank())
    }

    @Test
    fun testResilience_ExponentialBackoff_FailsGracefullyAfterMaxAttempts() = runBlocking {
        var attemptsCount = 0
        val policy = ResilienceManager.RetryPolicy(maxAttempts = 3, initialDelayMs = 30L)

        val result = ResilienceManager.executeWithBackoff<String>(policy) { _, _ ->
            attemptsCount++
            throw IOException("Caída total de interfaz de red")
        }

        assertFalse(result.isSuccess)
        assertEquals(3, result.attemptsMade)
        assertNull(result.data)
        assertNotNull(result.errorMessage)
    }
}
