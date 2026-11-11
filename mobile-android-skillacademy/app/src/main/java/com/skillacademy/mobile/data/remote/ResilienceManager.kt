package com.skillacademy.mobile.data.remote

import kotlinx.coroutines.delay
import java.io.IOException
import java.util.UUID

/**
 * ============================================================================
 * PRÁCTICA 12: Mecanismo de Resiliencia, Idempotencia y Exponential Backoff
 *
 * Implementa las reglas de resiliencia ante contingencias de red:
 *  ✔ Generación de Idempotency-Key (UUID v4) para evitar cobros o inscripciones duplicadas
 *  ✔ Reintento controlado con espera exponencial progresiva (Exponential Backoff + Jitter)
 *  ✔ Clasificación de errores no reintentables (400, 401, 403, 404) vs reintentables (502, 503, 504, Timeout)
 * ============================================================================
 */
object ResilienceManager {

    data class RetryPolicy(
        val maxAttempts: Int = 3,
        val initialDelayMs: Long = 400L,
        val maxDelayMs: Long = 3000L,
        val multiplier: Double = 2.0
    )

    data class ExecutionResult<T>(
        val data: T?,
        val attemptsMade: Int,
        val isSuccess: Boolean,
        val errorMessage: String? = null,
        val idempotencyKey: String
    )

    /**
     * Genera una clave de idempotencia única para cada intención de escritura.
     */
    fun generateIdempotencyKey(): String {
        return "IDEMP-${UUID.randomUUID()}"
    }

    /**
     * Determina si un código de error o excepción es transitoria y amerita reintento.
     */
    fun isRetryableException(e: Throwable): Boolean {
        return when (e) {
            is java.net.SocketTimeoutException,
            is java.net.ConnectException,
            is java.net.UnknownHostException,
            is IOException -> true
            else -> false
        }
    }

    fun isRetryableHttpCode(statusCode: Int): Boolean {
        // 500, 502, 503, 504 son reintentables; 400, 401, 403, 404 NO
        return statusCode in listOf(500, 502, 503, 504, 429)
    }

    /**
     * Ejecuta una acción de red asíncrona aplicando espera progresiva exponencial.
     */
    suspend fun <T> executeWithBackoff(
        policy: RetryPolicy = RetryPolicy(),
        idempotencyKey: String = generateIdempotencyKey(),
        block: suspend (attempt: Int, idempKey: String) -> T
    ): ExecutionResult<T> {
        var currentDelay = policy.initialDelayMs
        var lastError: Throwable? = null

        for (attempt in 1..policy.maxAttempts) {
            try {
                val result = block(attempt, idempotencyKey)
                return ExecutionResult(
                    data = result,
                    attemptsMade = attempt,
                    isSuccess = true,
                    idempotencyKey = idempotencyKey
                )
            } catch (e: Throwable) {
                lastError = e
                val canRetry = attempt < policy.maxAttempts && isRetryableException(e)
                if (!canRetry) {
                    break
                }
                delay(currentDelay)
                currentDelay = (currentDelay * policy.multiplier).toLong().coerceAtMost(policy.maxDelayMs)
            }
        }

        return ExecutionResult(
            data = null,
            attemptsMade = policy.maxAttempts,
            isSuccess = false,
            errorMessage = lastError?.localizedMessage ?: "Fallo desconocido tras reintentos",
            idempotencyKey = idempotencyKey
        )
    }
}
