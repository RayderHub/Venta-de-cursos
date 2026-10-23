package com.skillacademy.mobile.data.remote

import com.skillacademy.mobile.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Cliente HTTP ligero para el backend Fastify + Elasticsearch.
 * URL base apunta al servicio desplegado en Render.
 */
object ElasticsearchHttp {
    // URL del backend Fastify que expone /api/cursos/search
    const val BACKEND_URL = "https://skillacademy-backend-at8o.onrender.com"

    /**
     * GET /api/cursos/search?q={query}
     * Devuelve el body JSON como String o null si falla.
     */
    suspend fun buscarCursos(query: String): String? = withContext(Dispatchers.IO) {
        try {
            val q = java.net.URLEncoder.encode(query.trim(), "UTF-8")
            val url = URL("$BACKEND_URL/api/cursos/search?q=$q")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 15_000
            conn.readTimeout    = 15_000
            conn.setRequestProperty("Accept", "application/json")

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = BufferedReader(InputStreamReader(stream, "UTF-8")).use { it.readText() }
            if (code in 200..299) text else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

/**
 * CoursesRepository — Entregable 2
 *
 * Carga datos reales desde Supabase usando SupabaseHttp (el cliente HTTP del proyecto):
 *  - Cursos publicados con galería, requisitos, queAprenderas
 *  - Lecciones por curso (tabla lecciones)
 *  - Reseñas con nombre del reviewer (view curso_reviews_public, con fallback a tabla directa)
 *  - Perfil del instructor (tabla profiles)
 *  - Banners y Categorías derivados dinámicamente de los cursos existentes
 */
object CoursesRepository {

    private const val BASE = "/rest/v1"

    // ─────────────────────────────────────────────────────────────────────────
    // CURSOS — catálogo completo
    // ─────────────────────────────────────────────────────────────────────────

    var isLastFetchSuccess: Boolean = true
    var lastErrorMessage: String? = null

    suspend fun getCourses(): List<CourseUiModel> = withContext(Dispatchers.IO) {
        val result = SupabaseHttp.getArray("$BASE/cursos?select=*&estado=eq.Publicado&order=id.asc")
        if (result.isSuccess && result.data != null) {
            isLastFetchSuccess = true
            lastErrorMessage = null
            val lista = parseArray(result.data) { parseCurso(it) }
            // Actualizar caché para el fallback local de Elasticsearch
            _cachedCourses.clear()
            _cachedCourses.addAll(lista)
            lista
        } else {
            isLastFetchSuccess = false
            lastErrorMessage = "No fue posible obtener la información."
            emptyList()
        }
    }

    /** Carga detalles completos de UN curso: con lecciones, reseñas e instructor */
    suspend fun getCourseDetail(cursoId: Long): CourseUiModel? = withContext(Dispatchers.IO) {
        try {
            coroutineScope {
                val cursoDeferred     = async { fetchCursoBase(cursoId) }
                val leccionesDeferred = async { fetchLecciones(cursoId) }
                val resenasDeferred   = async { fetchResenas(cursoId) }

                val cursoBase = cursoDeferred.await() ?: return@coroutineScope null
                val lecciones = leccionesDeferred.await()
                val resenas   = resenasDeferred.await()
                val instructor = fetchInstructor(cursoBase.instructorId)

                cursoBase.copy(
                    lecciones        = lecciones,
                    resenas          = resenas,
                    instructorPerfil = instructor
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private suspend fun fetchCursoBase(cursoId: Long): CourseUiModel? {
        val result = SupabaseHttp.getArray("$BASE/cursos?id=eq.$cursoId&limit=1")
        return if (result.isSuccess && result.data != null && result.data.length() > 0) {
            parseCurso(result.data.getJSONObject(0))
        } else null
    }

    private suspend fun fetchLecciones(cursoId: Long): List<LessonUiModel> {
        val result = SupabaseHttp.getArray("$BASE/lecciones?curso_id=eq.$cursoId&order=orden.asc")
        if (!result.isSuccess || result.data == null) return emptyList()
        return parseArray(result.data) { o ->
            LessonUiModel(
                id              = o.optLong("id", 0),
                orden           = o.optInt("orden", 1),
                titulo          = o.optString("titulo", "Lección"),
                descripcion     = o.optString("descripcion", ""),
                duracionMinutos = o.optInt("duracion_minutos", 0),
                esGratis        = o.optBoolean("es_gratis", false)
            )
        }
    }

    /**
     * Intenta cargar reseñas desde la VIEW curso_reviews_public.
     * Si la vista no existe aún (antes de correr la migración SQL),
     * cae en la tabla directa curso_reviews sin join de nombre.
     */
    private suspend fun fetchResenas(cursoId: Long): List<ReviewUiModel> {
        // Intento 1: vista con join de nombre del alumno
        val viewResult = SupabaseHttp.getArray(
            "$BASE/curso_reviews_public?curso_id=eq.$cursoId&order=created_at.desc"
        )
        if (viewResult.isSuccess && viewResult.data != null) {
            return parseArray(viewResult.data) { o ->
                ReviewUiModel(
                    id             = o.optLong("id", 0),
                    rating         = o.optInt("rating", 5),
                    comment        = o.optString("comment").takeIf { it.isNotBlank() },
                    reviewerName   = o.optString("reviewer_name").ifBlank { "Estudiante" },
                    reviewerAvatar = o.optString("reviewer_avatar").takeIf { it.isNotBlank() },
                    reviewerRole   = o.optString("reviewer_role").ifBlank { "student" },
                    createdAt      = o.optString("created_at", "")
                )
            }
        }

        // Intento 2: tabla directa sin nombre del reviewer
        val tableResult = SupabaseHttp.getArray(
            "$BASE/curso_reviews?curso_id=eq.$cursoId&order=created_at.desc"
        )
        if (!tableResult.isSuccess || tableResult.data == null) return emptyList()
        return parseArray(tableResult.data) { o ->
            ReviewUiModel(
                id             = o.optLong("id", 0),
                rating         = o.optInt("rating", 5),
                comment        = o.optString("comment").takeIf { it.isNotBlank() },
                reviewerName   = "Estudiante",
                reviewerAvatar = null,
                reviewerRole   = "student",
                createdAt      = o.optString("created_at", "")
            )
        }
    }

    private suspend fun fetchInstructor(instructorId: String): InstructorUiModel? {
        if (instructorId.isBlank()) return null
        val result = SupabaseHttp.getArray(
            "$BASE/profiles?id=eq.$instructorId&select=id,full_name,avatar_url,email&limit=1"
        )
        if (!result.isSuccess || result.data == null || result.data.length() == 0) return null
        val o = result.data.getJSONObject(0)
        return InstructorUiModel(
            id        = o.optString("id", instructorId),
            fullName  = o.optString("full_name", "Instructor"),
            avatarUrl = o.optString("avatar_url").takeIf { it.isNotBlank() },
            email     = o.optString("email", "")
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BÚSQUEDA PREDICTIVA — usa Elasticsearch a través del backend Fastify
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Busca cursos usando el motor Elasticsearch del backend Fastify.
     * Endpoint: GET /api/cursos/search?q={query}
     *
     * La búsqueda es full-text con boost por campo (título > instructor > categoría > descripción)
     * más una prefix query para predicción en tiempo real.
     *
     * Fallback: si el backend no está disponible, filtra localmente el catálogo ya cargado.
     */
    suspend fun searchCourses(query: String): List<CourseUiModel> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext getCourses()

        // ── Intento principal: Elasticsearch vía Fastify ──────────────────────
        val body = ElasticsearchHttp.buscarCursos(query)
        if (body != null) {
            try {
                val json = JSONObject(body)
                val cursosArr = json.optJSONArray("cursos") ?: JSONArray()
                val resultado = parseArray(cursosArr) { parseCursoES(it) }
                if (resultado.isNotEmpty()) return@withContext resultado
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // ── Fallback: filtro local si Elasticsearch no responde ───────────────
        val cached = if (_cachedCourses.isNotEmpty()) _cachedCourses else getCourses()
        cached.filter {
            it.titulo.contains(query, ignoreCase = true) ||
            it.instructor.contains(query, ignoreCase = true) ||
            it.categoria.contains(query, ignoreCase = true)
        }
    }

    // Caché en memoria para el fallback de búsqueda
    private val _cachedCourses = mutableListOf<CourseUiModel>()

    /**
     * Parsea el objeto JSON que devuelve Elasticsearch (campos en español,
     * misma forma que el índice indexado por elasticsearch_service.js).
     */
    private fun parseCursoES(o: JSONObject): CourseUiModel = CourseUiModel(
        id            = o.optLong("id", 0),
        titulo        = o.optString("titulo", "Sin título"),
        categoria     = o.optString("categoria", "General"),
        nivel         = o.optString("nivel", "Principiante"),
        instructor    = o.optString("instructor", "Instructor"),
        instructorId  = o.optString("instructor_id", ""),
        precio        = o.optDouble("precio", 0.0),
        oldPrecio     = o.optDouble("old_precio", 0.0).takeIf { it > 0 },
        imagen        = o.optString("imagen", ""),
        imagenesGaleria = emptyList(),
        descripcion   = o.optString("descripcion", ""),
        requisitos    = "",
        queAprenderas = "",
        rating        = o.optDouble("rating", 0.0),
        reviews       = o.optInt("reviews", 0),
        isBestSeller  = o.optInt("reviews", 0) >= 3
    )

    // ─────────────────────────────────────────────────────────────────────────
    // BANNERS — generados dinámicamente desde cursos publicados
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun getBanners(): List<BannerUiModel> = withContext(Dispatchers.IO) {
        val result = SupabaseHttp.getArray(
            "$BASE/cursos?select=id,titulo,descripcion,old_precio,precio&estado=eq.Publicado&order=id.asc&limit=5"
        )
        if (!result.isSuccess || result.data == null) {
            return@withContext com.skillacademy.mobile.data.mock.MockData.banners
        }
        val arr = result.data
        val banners = mutableListOf<BannerUiModel>()
        val colorKeys = listOf("navy", "emerald", "cyan", "purple", "coral")
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val precio    = o.optDouble("precio", 0.0)
            val oldPrecio = o.optDouble("old_precio", 0.0)
            val desc = if (oldPrecio > precio && oldPrecio > 0) {
                val pct = (((oldPrecio - precio) / oldPrecio) * 100).toInt()
                "-$pct% OFF"
            } else "Nuevo"
            banners.add(
                BannerUiModel(
                    id             = o.optLong("id", i.toLong()),
                    titulo         = o.optString("titulo", "Curso SkillAcademy"),
                    subtitulo      = o.optString("descripcion", "Aprende con los mejores").take(60),
                    descuentoTexto = desc,
                    imagenFondo    = colorKeys[i % colorKeys.size],
                    enlaceCursoId  = o.optLong("id")
                )
            )
        }
        banners.ifEmpty { com.skillacademy.mobile.data.mock.MockData.banners }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CATEGORÍAS — derivadas de los cursos existentes
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun getCategories(): List<CategoryUiModel> = withContext(Dispatchers.IO) {
        val result = SupabaseHttp.getArray("$BASE/cursos?select=categoria&estado=eq.Publicado")
        if (!result.isSuccess || result.data == null) {
            return@withContext com.skillacademy.mobile.data.mock.MockData.categories
        }
        val arr = result.data
        val countMap = mutableMapOf<String, Int>()
        for (i in 0 until arr.length()) {
            val cat = arr.getJSONObject(i).optString("categoria", "General")
            countMap[cat] = (countMap[cat] ?: 0) + 1
        }
        countMap.entries
            .sortedByDescending { it.value }
            .map { (nombre, total) ->
                CategoryUiModel(
                    id          = nombre.lowercase().replace(" ", "_"),
                    nombre      = nombre,
                    icono       = categoryIcon(nombre),
                    totalCursos = total
                )
            }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    fun parseCurso(o: JSONObject): CourseUiModel {
        val galeriaRaw = o.optJSONArray("imagenes_galeria")
        val galeria = if (galeriaRaw != null) {
            (0 until galeriaRaw.length()).map { galeriaRaw.getString(it) }
        } else emptyList()

        return CourseUiModel(
            id            = o.optLong("id", 0),
            titulo        = o.optString("titulo", "Sin título"),
            categoria     = o.optString("categoria", "General"),
            nivel         = o.optString("nivel", "Principiante"),
            instructor    = o.optString("instructor", "Instructor"),
            instructorId  = o.optString("instructor_id", ""),
            precio        = o.optDouble("precio", 0.0),
            oldPrecio     = o.optDouble("old_precio", 0.0).takeIf { it > 0 },
            imagen        = o.optString("imagen", ""),
            imagenesGaleria = galeria,
            descripcion   = o.optString("descripcion", ""),
            requisitos    = o.optString("requisitos", ""),
            queAprenderas = o.optString("que_aprenderas", ""),
            rating        = o.optDouble("rating", 0.0),
            reviews       = o.optInt("reviews", 0),
            isBestSeller  = o.optInt("reviews", 0) >= 3
        )
    }

    private fun <T> parseArray(arr: JSONArray, mapper: (JSONObject) -> T): List<T> {
        val list = mutableListOf<T>()
        for (i in 0 until arr.length()) {
            try { list.add(mapper(arr.getJSONObject(i))) } catch (_: Exception) {}
        }
        return list
    }

    private fun categoryIcon(nombre: String): String = when {
        nombre.contains("Excel", ignoreCase = true)                              -> "excel"
        nombre.contains("Diseño", ignoreCase = true) ||
        nombre.contains("Design", ignoreCase = true)                             -> "design"
        nombre.contains("Program", ignoreCase = true) ||
        nombre.contains("Código", ignoreCase = true) ||
        nombre.contains("Frontend", ignoreCase = true)                           -> "code"
        nombre.contains("Producto", ignoreCase = true) ||
        nombre.contains("Productiv", ignoreCase = true)                          -> "productivity"
        else                                                                      -> "code"
    }
}
