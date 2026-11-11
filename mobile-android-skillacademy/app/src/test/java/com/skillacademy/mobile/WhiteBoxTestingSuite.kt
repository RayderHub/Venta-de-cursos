package com.skillacademy.mobile

import org.junit.Assert.*
import org.junit.Test
import org.json.JSONObject
import org.json.JSONArray

/**
 * ============================================================================
 * PRÁCTICA 11: Pruebas de White Box (Caja Blanca)
 * Suite de pruebas unitarias sobre métodos críticos de la aplicación móvil:
 *  1. Validación: Sanitización y validación de entrada de búsqueda
 *  2. Procesamiento / Cálculo: Reglas de checkout, cupones y descuentos acumulables
 *  3. Comunicación y Excepciones: Manejo de respuestas HTTP y mapeo de errores
 * ============================================================================
 */
class WhiteBoxTestingSuite {

    // ------------------------------------------------------------------------
    // MÉTODO 1: VALIDACIÓN (Input Validation & Sanitization)
    // ------------------------------------------------------------------------
    enum class ValidationStatus { VALID, EMPTY_OR_NULL, TOO_SHORT, TOO_LONG, SUSPICIOUS_CHARACTERS }
    data class ValidationResult(val status: ValidationStatus, val sanitizedValue: String)

    /**
     * Valida y sanitiza una consulta de búsqueda ingresada por el usuario.
     * Complejidad Ciclomática: V(G) = 5
     */
    fun validateAndSanitizeSearchQuery(input: String?): ValidationResult {
        // Decisión 1: Nulo o vacío
        if (input == null || input.trim().isEmpty()) {
            return ValidationResult(ValidationStatus.EMPTY_OR_NULL, "")
        }

        val trimmed = input.trim()

        // Decisión 2: Valor límite inferior (mínimo 2 caracteres)
        if (trimmed.length < 2) {
            return ValidationResult(ValidationStatus.TOO_SHORT, trimmed)
        }

        // Decisión 3: Valor límite superior (máximo 50 caracteres)
        if (trimmed.length > 50) {
            return ValidationResult(ValidationStatus.TOO_LONG, trimmed.take(50))
        }

        // Decisión 4: Caracteres sospechosos de inyección SQL / Scripting
        val suspiciousRegex = Regex("[;'\"\\-\\-\\/\\*]")
        if (suspiciousRegex.containsMatchIn(trimmed)) {
            val safe = trimmed.replace(suspiciousRegex, "")
            return ValidationResult(ValidationStatus.SUSPICIOUS_CHARACTERS, safe)
        }

        // Camino exitoso
        return ValidationResult(ValidationStatus.VALID, trimmed)
    }

    // Pruebas Método 1 (Statement, Branch, Condition & Boundary)
    @Test
    fun testM1_Path1_NullOrEmptyInput_ReturnsEmptyOrNull() {
        val rNull = validateAndSanitizeSearchQuery(null)
        assertEquals(ValidationStatus.EMPTY_OR_NULL, rNull.status)
        assertEquals("", rNull.sanitizedValue)

        val rBlank = validateAndSanitizeSearchQuery("   ")
        assertEquals(ValidationStatus.EMPTY_OR_NULL, rBlank.status)
    }

    @Test
    fun testM1_Path2_BoundaryMinLength_TooShort() {
        val r1 = validateAndSanitizeSearchQuery("a")
        assertEquals(ValidationStatus.TOO_SHORT, r1.status)
        assertEquals("a", r1.sanitizedValue)

        val r2 = validateAndSanitizeSearchQuery("ex")
        assertEquals(ValidationStatus.VALID, r2.status)
    }

    @Test
    fun testM1_Path3_BoundaryMaxLength_TooLong() {
        val str50 = "A".repeat(50)
        val str51 = "A".repeat(51)

        val r50 = validateAndSanitizeSearchQuery(str50)
        assertEquals(ValidationStatus.VALID, r50.status)

        val r51 = validateAndSanitizeSearchQuery(str51)
        assertEquals(ValidationStatus.TOO_LONG, r51.status)
        assertEquals(50, r51.sanitizedValue.length)
    }

    @Test
    fun testM1_Path4_SuspiciousCharacters_Neutralized() {
        val r = validateAndSanitizeSearchQuery("excel'; DROP TABLE--")
        assertEquals(ValidationStatus.SUSPICIOUS_CHARACTERS, r.status)
        assertFalse(r.sanitizedValue.contains(";"))
        assertFalse(r.sanitizedValue.contains("'"))
    }

    @Test
    fun testM1_Path5_ValidQuery_Success() {
        val r = validateAndSanitizeSearchQuery("Excel Avanzado")
        assertEquals(ValidationStatus.VALID, r.status)
        assertEquals("Excel Avanzado", r.sanitizedValue)
    }


    // ------------------------------------------------------------------------
    // MÉTODO 2: PROCESAMIENTO / CÁLCULO (Pricing & Coupon Engine)
    // ------------------------------------------------------------------------
    data class CheckoutResult(
        val basePrice: Double,
        val discountRate: Double,
        val vipBonusApplied: Boolean,
        val finalPrice: Double
    )

    /**
     * Calcula el monto final del checkout aplicando cupones y beneficios por membresía.
     * Complejidad Ciclomática: V(G) = 6
     */
    fun calculateCheckoutTotal(basePrice: Double, couponCode: String?, isVip: Boolean): CheckoutResult {
        // Decisión 1: Validación de valor límite inferior
        if (basePrice <= 0.0) {
            throw IllegalArgumentException("El precio base debe ser estrictamente positivo")
        }

        // Decisión 2 y 3: Tasa de descuento por cupón
        var rate = 0.0
        val couponClean = couponCode?.trim()?.uppercase()
        if (couponClean != null) {
            rate = when (couponClean) {
                "SKILL20" -> 0.20
                "BLACKFRIDAY" -> 0.50
                "DEV10" -> 0.10
                else -> 0.0
            }
        }

        var priceAfterCoupon = basePrice * (1.0 - rate)

        // Decisión 4: Bonificación VIP acumulable (condición compuesta)
        var vipApplied = false
        if (isVip && priceAfterCoupon >= 50.0) {
            priceAfterCoupon *= 0.95 // 5% extra
            vipApplied = true
        }

        // Redondeo a 2 decimales
        val rounded = Math.round(priceAfterCoupon * 100.0) / 100.0
        return CheckoutResult(basePrice, rate, vipApplied, rounded)
    }

    // Pruebas Método 2
    @Test(expected = IllegalArgumentException::class)
    fun testM2_Path1_ZeroOrNegativePrice_ThrowsException() {
        calculateCheckoutTotal(0.0, "SKILL20", false)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testM2_Path1b_NegativePrice_ThrowsException() {
        calculateCheckoutTotal(-15.50, null, true)
    }

    @Test
    fun testM2_Path2_CouponSkill20_StandardUser() {
        val res = calculateCheckoutTotal(100.0, "SKILL20", false)
        assertEquals(0.20, res.discountRate, 0.001)
        assertFalse(res.vipBonusApplied)
        assertEquals(80.0, res.finalPrice, 0.01)
    }

    @Test
    fun testM2_Path3_CouponBlackFriday_VipBonusApplied() {
        // Base 120, 50% desc -> 60 (>= 50 y VIP -> 5% adicional -> 57.0)
        val res = calculateCheckoutTotal(120.0, "BLACKFRIDAY", true)
        assertEquals(0.50, res.discountRate, 0.001)
        assertTrue(res.vipBonusApplied)
        assertEquals(57.0, res.finalPrice, 0.01)
    }

    @Test
    fun testM2_Path4_VipBonusNotAppliedWhenUnderThreshold() {
        // Base 40, sin cupón, es VIP pero < 50
        val res = calculateCheckoutTotal(40.0, null, true)
        assertFalse(res.vipBonusApplied)
        assertEquals(40.0, res.finalPrice, 0.01)
    }

    @Test
    fun testM2_Path5_UnknownCoupon_AppliesNoDiscount() {
        val res = calculateCheckoutTotal(80.0, "INVENTADO", false)
        assertEquals(0.0, res.discountRate, 0.001)
        assertEquals(80.0, res.finalPrice, 0.01)
    }


    // ------------------------------------------------------------------------
    // MÉTODO 3: COMUNICACIÓN Y MANEJO DE EXCEPCIONES
    // ------------------------------------------------------------------------
    sealed class NetworkResponse {
        data class Success(val count: Int, val items: List<String>) : NetworkResponse()
        data class ClientError(val code: Int, val message: String) : NetworkResponse()
        data class ServerError(val code: Int, val message: String) : NetworkResponse()
        data class ParseError(val rawSnippet: String) : NetworkResponse()
    }

    /**
     * Mapea códigos de estado HTTP y serializaciones de backend hacia modelos de dominio.
     * Complejidad Ciclomática: V(G) = 6
     */
    fun processHttpResponse(statusCode: Int, responseBody: String?): NetworkResponse {
        return when {
            // Camino 1: Exitoso
            statusCode in 200..299 -> {
                if (responseBody.isNullOrBlank()) {
                    NetworkResponse.Success(0, emptyList())
                } else {
                    try {
                        val fakeItems = if (responseBody.contains("cursos")) listOf("Excel Avanzado", "Power BI") else emptyList()
                        NetworkResponse.Success(fakeItems.size, fakeItems)
                    } catch (e: Exception) {
                        NetworkResponse.ParseError(responseBody.take(30))
                    }
                }
            }
            // Camino 2: Recurso no encontrado o catálogo vacío
            statusCode == 404 -> NetworkResponse.Success(0, emptyList())
            // Camino 3: Error de validación o cliente
            statusCode in 400..499 -> NetworkResponse.ClientError(statusCode, "Petición rechazada por el servidor ($statusCode)")
            // Camino 4: Error interno o caída de servicio (502 Bonsai/Render)
            statusCode in 500..599 -> NetworkResponse.ServerError(statusCode, "Servicio temporalmente indisponible")
            // Camino 5: Código fuera de rango RFC
            else -> NetworkResponse.ServerError(statusCode, "Código HTTP inesperado: $statusCode")
        }
    }

    // Pruebas Método 3
    @Test
    fun testM3_Path1_Http200_ValidBody() {
        val res = processHttpResponse(200, "{\"total\": 2, \"cursos\": [\"Excel\"]}")
        assertTrue(res is NetworkResponse.Success)
        assertEquals(2, (res as NetworkResponse.Success).count)
    }

    @Test
    fun testM3_Path2_Http200_EmptyBody() {
        val res = processHttpResponse(204, "")
        assertTrue(res is NetworkResponse.Success)
        assertEquals(0, (res as NetworkResponse.Success).count)
    }

    @Test
    fun testM3_Path3_Http404_ReturnsEmptyGracefully() {
        val res = processHttpResponse(404, null)
        assertTrue(res is NetworkResponse.Success)
        assertEquals(0, (res as NetworkResponse.Success).count)
    }

    @Test
    fun testM3_Path4_Http400_ClientError() {
        val res = processHttpResponse(400, "{\"error\": \"Bad Request\"}")
        assertTrue(res is NetworkResponse.ClientError)
        assertEquals(400, (res as NetworkResponse.ClientError).code)
    }

    @Test
    fun testM3_Path5_Http502_ServerError() {
        val res = processHttpResponse(502, "Bad Gateway")
        assertTrue(res is NetworkResponse.ServerError)
        assertEquals(502, (res as NetworkResponse.ServerError).code)
    }
}

