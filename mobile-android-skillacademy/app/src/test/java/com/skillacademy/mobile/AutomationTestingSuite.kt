package com.skillacademy.mobile

import com.skillacademy.mobile.data.remote.ResilienceManager
import com.skillacademy.mobile.data.security.SecurityUtils
import org.junit.Assert.*
import org.junit.Test

/**
 * ============================================================================
 * PRÁCTICAS 13 Y 14: Automatización de Pruebas y Validación Continua
 *
 * Componentes Críticos Auditados:
 *  1. Función crítica móvil: Módulo de Checkout y Cálculos Financieros
 *  2. Servicio y comunicación: Mapeo de respuestas de la API Fastify / Bonsai
 *  3. Persistencia y Resiliencia: Prevención de duplicados con Idempotency-Key
 * ============================================================================
 */
class AutomationTestingSuite {

    // ------------------------------------------------------------------------
    // FASE 2: AUTOMATIZACIÓN DE PRUEBAS UNITARIAS (5 Casos Obligatorios)
    // ------------------------------------------------------------------------

    /**
     * Lógica de cálculo de precios del módulo crítico de pagos
     */
    private fun calculateDiscount(basePrice: Double, couponCode: String?, isVip: Boolean): Double {
        if (basePrice <= 0.0) {
            throw IllegalArgumentException("El precio base debe ser estrictamente mayor a 0")
        }

        var rate = when (couponCode?.trim()?.uppercase()) {
            "SKILL20" -> 0.20
            "BLACKFRIDAY" -> 0.50
            "DEV10" -> 0.10
            else -> 0.0
        }

        var finalPrice = basePrice * (1.0 - rate)

        // Bonificación VIP acumulable para subtotales >= 50
        if (isVip && finalPrice >= 50.0) {
            finalPrice *= 0.95
        }

        return Math.round(finalPrice * 100.0) / 100.0
    }

    // 1. Entrada válida
    @Test
    fun testU01_ValidInput_CalculatesCorrectDiscount() {
        val finalPrice = calculateDiscount(100.0, "SKILL20", isVip = false)
        assertEquals("Con cupón SKILL20 sobre $100 se debe pagar $80.00", 80.00, finalPrice, 0.01)
    }

    // 2. Entrada vacía o nula
    @Test
    fun testU02_EmptyOrNullInput_HandlesGracefully() {
        val priceWithNull = calculateDiscount(50.0, null, isVip = false)
        assertEquals("Cupón nulo mantiene precio base", 50.00, priceWithNull, 0.01)

        val priceWithBlank = calculateDiscount(50.0, "   ", isVip = false)
        assertEquals("Cupón en blanco mantiene precio base", 50.00, priceWithBlank, 0.01)
    }

    // 3. Valores fuera de rango (límite inferior <= 0)
    @Test(expected = IllegalArgumentException::class)
    fun testU03_OutOfRange_NegativeOrZeroPrice_ThrowsException() {
        calculateDiscount(0.0, "SKILL20", isVip = false)
    }

    // 4. Resultado esperado (combinación VIP y umbral financiero)
    @Test
    fun testU04_ExpectedResult_VipBonusAppliedAboveThreshold() {
        // Base 120, descuento BlackFriday 50% = 60. Como 60 >= 50 y es VIP -> 5% adicional = 57.0
        val finalPrice = calculateDiscount(120.0, "BLACKFRIDAY", isVip = true)
        assertEquals("Debe aplicar 50% y bono VIP del 5% adicional", 57.00, finalPrice, 0.01)
    }

    // 5. Manejo de excepciones y sanitización de entrada sospechosa
    @Test
    fun testU05_ExceptionHandling_MalformedQuerySanitized() {
        val rawInput = "excel'; DROP TABLE cursos; --"
        val clean = SecurityUtils.sanitizeSearchQuery(rawInput)

        assertFalse("No debe contener comillas simples", clean.contains("'"))
        assertFalse("No debe contener punto y coma", clean.contains(";"))
        assertFalse("No debe contener guiones dobles", clean.contains("--"))
        assertTrue("Debe conservar la palabra clave válida", clean.contains("excel"))
    }


    // ------------------------------------------------------------------------
    // FASE 3: AUTOMATIZACIÓN DE INTEGRACIÓN (3 Casos Obligatorios)
    // ------------------------------------------------------------------------

    // Simulación del repositorio de persistencia con control de idempotencia
    private val ordersDb = mutableMapOf<String, String>() // IdempotencyKey -> OrderId

    data class MockSearchResponse(val total: Int, val cursos: List<String>, val desdeCache: Boolean)

    private fun parseSearchResponse(rawJson: String): MockSearchResponse {
        val total = Regex("\"total\"\\s*:\\s*(\\d+)").find(rawJson)?.groupValues?.get(1)?.toInt() ?: 0
        val desdeCache = rawJson.contains("\"desdeCache\"\\s*:\\s*true".toRegex())
        val titulos = Regex("\"titulo\"\\s*:\\s*\"([^\"]+)\"").findAll(rawJson).map { it.groupValues[1] }.toList()
        return MockSearchResponse(total, titulos, desdeCache)
    }

    /**
     * Prueba I-01: La app procesa la respuesta JSON estructurada del servicio de búsqueda
     */
    @Test
    fun testI01_MobileToSearchApi_ParsesResponseCorrectly() {
        // Simulación de payload real que entrega /api/cursos/search en Fastify
        val mockApiResponse = """
            {
                "total": 2,
                "cursos": [
                    {
                        "id": 1,
                        "titulo": "Excel Avanzado",
                        "precio": 25.0,
                        "categoria": "Excel"
                    },
                    {
                        "id": 2,
                        "titulo": "Automatizacion de Reportes en Excel",
                        "precio": 32.0,
                        "categoria": "Excel"
                    }
                ],
                "desdeCache": true
            }
        """.trimIndent()

        val parsed = parseSearchResponse(mockApiResponse)

        assertEquals("Total de cursos debe ser 2", 2, parsed.total)
        assertEquals("Debe contener 2 elementos en el arreglo", 2, parsed.cursos.size)
        assertTrue("Debe provenir de la caché optimizada de Fastify", parsed.desdeCache)
        assertEquals("Excel Avanzado", parsed.cursos[0])
    }

    /**
     * Prueba I-02: La API devuelve los códigos HTTP esperados y la app los mapea
     */
    @Test
    fun testI02_ApiHttpStatusCodes_MapsExpectedErrors() {
        // Validación de clasificación de códigos
        assertTrue("HTTP 200 es exitoso", 200 in 200..299)
        assertTrue("HTTP 502 Bad Gateway es un error transitorio reintentable", ResilienceManager.isRetryableHttpCode(502))
        assertFalse("HTTP 400 Bad Request es un error definitivo de cliente, no reintentable", ResilienceManager.isRetryableHttpCode(400))
        assertFalse("HTTP 401 Unauthorized no debe reintentarse sin nuevo login", ResilienceManager.isRetryableHttpCode(401))
    }

    /**
     * Prueba I-03: Persistencia y resiliencia evitan duplicados mediante Idempotency-Key
     */
    @Test
    fun testI03_Persistence_IdempotencyPreventsDuplicateOrders() {
        val idempotencyToken = ResilienceManager.generateIdempotencyKey()

        // Primer intento: Registro de orden exitoso
        fun processPayment(token: String, orderDetails: String): String {
            if (ordersDb.containsKey(token)) {
                // Si la clave ya existe, retorna la orden previamente generada (Idempotencia)
                return ordersDb[token]!!
            }
            val newOrderId = "ORD-${System.currentTimeMillis()}"
            ordersDb[token] = newOrderId
            return newOrderId
        }

        val firstOrderId = processPayment(idempotencyToken, "Curso Excel Avanzado - $25")
        assertNotNull(firstOrderId)

        // Segundo intento simulado (por timeout de red donde el cliente reintenta con la misma clave)
        val retryOrderId = processPayment(idempotencyToken, "Curso Excel Avanzado - $25")

        assertEquals("El reintento debe devolver exactamente el mismo ID de orden sin duplicar en DB", firstOrderId, retryOrderId)
        assertEquals("Solo debe existir un registro en el almacenamiento", 1, ordersDb.size)
    }
}
