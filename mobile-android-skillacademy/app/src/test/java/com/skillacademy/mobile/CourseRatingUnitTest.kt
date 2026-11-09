package com.skillacademy.mobile

import org.junit.Assert.*
import org.junit.Test

/**
 * =====================================================================
 * FASE 2 — Pruebas Unitarias (Android / JUnit)
 * FASE 3 — Pruebas de Integración simuladas (Android / JUnit)
 * Funcionalidad crítica: Calificación de Cursos
 *
 * Cubre:
 *  ✔ TC-U01: Caso válido   — rating 4 dentro del rango 1-5
 *  ✔ TC-U02: Caso límite   — extremos 1 y 5, flotantes
 *  ✔ TC-U03: Caso inválido — fuera de rango es acotado
 *  ✔ TC-I01: Integración   — upsert exitoso en curso_reviews (simulado con mapa)
 *  ✔ TC-I02: Integración   — error cuando userId está vacío
 *  ✔ TC-I03: Integración   — verificar que el rating persiste tras recarga
 * =====================================================================
 */
class CourseRatingUnitTest {

    // ─────────────────────────────────────────────────────────────────────────
    // Réplica de la lógica de acotamiento de rating (equivalente a la web)
    // Origen: InscripcionesService.calificarCurso() L118 (inscripciones.service.ts)
    // ─────────────────────────────────────────────────────────────────────────
    private fun boundRating(rating: Double): Int {
        return maxOf(1, minOf(5, Math.round(rating).toInt()))
    }

    private fun boundRating(rating: Int): Int = boundRating(rating.toDouble())

    // ─────────────────────────────────────────────────────────────────────────
    // Simulación de repositorio in-memory de curso_reviews
    // Equivale a la tabla `curso_reviews` de Supabase
    // ─────────────────────────────────────────────────────────────────────────
    data class ReviewRecord(val rating: Int, val comment: String?)

    private val reviewsDb = mutableMapOf<Pair<String, Long>, ReviewRecord>()

    /** Simula el upsert de InscripcionesService.calificarCurso() en Supabase */
    private fun upsertReview(
        userId: String,
        cursoId: Long,
        rating: Int,
        comment: String?,
        accessToken: String
    ): Result<Boolean> {
        if (userId.isBlank()) return Result.failure(Exception("userId vacío — acceso no autorizado"))
        if (accessToken.isBlank()) return Result.failure(Exception("Token inválido — no autenticado"))
        val bounded = boundRating(rating)
        reviewsDb[Pair(userId, cursoId)] = ReviewRecord(bounded, comment)
        return Result.success(true)
    }

    /** Simula la lectura del review guardado */
    private fun obtenerReview(userId: String, cursoId: Long): ReviewRecord? {
        return reviewsDb[Pair(userId, cursoId)]
    }

    // =========================================================================
    // FASE 2 — Pruebas Unitarias
    // =========================================================================

    // ── TC-U01: Caso Válido ──────────────────────────────────────────────────

    @Test
    fun testU01_BoundRating_ValorValido_Retorna4() {
        val resultado = boundRating(4)
        assertEquals("Rating 4 es válido y debe mantenerse en 4", 4, resultado)
    }

    @Test
    fun testU01_BoundRating_ValorValido_Retorna3() {
        val resultado = boundRating(3)
        assertEquals("Rating 3 es válido y debe mantenerse en 3", 3, resultado)
    }

    // ── TC-U02: Caso Límite ──────────────────────────────────────────────────

    @Test
    fun testU02_BoundRating_LimiteInferior_Retorna1() {
        assertEquals("El límite inferior 1 debe mantenerse en 1", 1, boundRating(1))
    }

    @Test
    fun testU02_BoundRating_LimiteSuperior_Retorna5() {
        assertEquals("El límite superior 5 debe mantenerse en 5", 5, boundRating(5))
    }

    @Test
    fun testU02_BoundRating_FlotanteAlto_Redondea5() {
        assertEquals("4.6 debe redondearse a 5", 5, boundRating(4.6))
    }

    @Test
    fun testU02_BoundRating_FlotanteBajo_Redondea1() {
        assertEquals("1.4 debe redondearse a 1", 1, boundRating(1.4))
    }

    // ── TC-U03: Caso Inválido ────────────────────────────────────────────────

    @Test
    fun testU03_BoundRating_Cero_AcotadoA1() {
        assertEquals("Rating 0 debe acotarse al mínimo 1", 1, boundRating(0))
    }

    @Test
    fun testU03_BoundRating_Negativo_AcotadoA1() {
        assertEquals("Rating -5 debe acotarse al mínimo 1", 1, boundRating(-5))
    }

    @Test
    fun testU03_BoundRating_Excedido_AcotadoA5() {
        assertEquals("Rating 6 debe acotarse al máximo 5", 5, boundRating(6))
    }

    @Test
    fun testU03_BoundRating_MuyAlto_AcotadoA5() {
        assertEquals("Rating 100 debe acotarse al máximo 5", 5, boundRating(100))
    }

    // =========================================================================
    // FASE 3 — Pruebas de Integración (simuladas con repositorio in-memory)
    // =========================================================================

    // ── TC-I01: Flujo correcto ───────────────────────────────────────────────

    @Test
    fun testI01_CalificarCurso_FlujoValido_GuardaCorrectamente() {
        val userId = "uuid-user-test-001"
        val cursoId = 1L
        val resultado = upsertReview(userId, cursoId, 4, "Excelente curso de prueba", "valid-token")

        assertTrue("El upsert debe ser exitoso", resultado.isSuccess)
        val review = obtenerReview(userId, cursoId)
        assertNotNull("La review debe existir en la BD simulada", review)
        assertEquals("El rating debe ser 4", 4, review?.rating)
        assertEquals("El comentario debe persistir", "Excelente curso de prueba", review?.comment)
    }

    @Test
    fun testI01_CalificarCurso_RatingAcotado_PersisteBounded() {
        // Si el usuario envía rating = 7 (inválido), debe guardarse como 5
        val userId = "uuid-user-test-002"
        val cursoId = 2L
        upsertReview(userId, cursoId, 7, "Test acotamiento", "valid-token")

        val review = obtenerReview(userId, cursoId)
        assertNotNull(review)
        assertEquals("Rating 7 debe guardarse como 5 (acotado)", 5, review?.rating)
    }

    @Test
    fun testI01_CalificarCurso_Upsert_SobreescribeAnterior() {
        val userId = "uuid-user-test-003"
        val cursoId = 3L
        // Primera calificación
        upsertReview(userId, cursoId, 3, "Primera calificación", "valid-token")
        // Segunda calificación (debe sobreescribir la primera)
        upsertReview(userId, cursoId, 5, "Actualicé mi opinión", "valid-token")

        val review = obtenerReview(userId, cursoId)
        assertEquals("El upsert debe sobreescribir con el nuevo rating", 5, review?.rating)
        assertEquals("El comentario también debe actualizarse", "Actualicé mi opinión", review?.comment)
    }

    // ── TC-I02: Manejo de error ──────────────────────────────────────────────

    @Test
    fun testI02_CalificarCurso_UserIdVacio_RetornaError() {
        val resultado = upsertReview("", 1L, 5, "Test sin userId", "valid-token")
        assertTrue("Debe fallar cuando userId está vacío", resultado.isFailure)
        assertTrue(resultado.exceptionOrNull()?.message?.contains("vacío") == true)
    }

    @Test
    fun testI02_CalificarCurso_TokenVacio_RetornaError() {
        val resultado = upsertReview("uuid-user", 1L, 5, "Test sin token", "")
        assertTrue("Debe fallar cuando el token está vacío", resultado.isFailure)
        assertTrue(resultado.exceptionOrNull()?.message?.contains("autenticado") == true)
    }

    // ── TC-I03: Persistencia tras recarga ────────────────────────────────────

    @Test
    fun testI03_Persistencia_ReviewDisponibleTrasLectura() {
        val userId = "uuid-user-test-persistencia"
        val cursoId = 10L
        upsertReview(userId, cursoId, 4, "Persistencia verificada", "valid-token")

        // Simula "recarga" leyendo de nuevo el mapa (equivale a SELECT en Supabase)
        val reviewRecargada = obtenerReview(userId, cursoId)
        assertNotNull("El review debe persistir tras una recarga simulada", reviewRecargada)
        assertEquals(4, reviewRecargada?.rating)
    }
}
