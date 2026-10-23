package com.skillacademy.mobile.data.remote

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.skillacademy.mobile.data.local.SessionManager
import com.skillacademy.mobile.data.model.CourseUiModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

data class EnrolledCourse(
    val inscripcionId: Long,
    val curso: CourseUiModel,
    val progreso: Int
)

data class TareaUiModel(
    val id: Long,
    val cursoId: Long,
    val titulo: String,
    val descripcion: String,
    val tipoContenido: String,
    val ponderacion: Int,
    val estado: String = "Pendiente"
)

/**
 * Bloque de contenido de una lección (equivale a ContenidoLeccion del modelo Angular).
 * tipo: 'titulo' | 'texto' | 'imagen' | 'tarjeta'
 */
data class ContenidoLeccionUiModel(
    val id: Long,
    val leccionId: Long,
    val tipo: String,   // titulo | texto | imagen | tarjeta
    val contenido: String,
    val orden: Int
)

/**
 * Pregunta o bloque interactivo de una tarea desde `tarea_contenido`.
 * tipo: 'titulo' | 'texto' | 'opcion_multiple' | 'respuesta_texto'
 */
data class TareaContenidoUiModel(
    val id: Long,
    val tareaId: Long,
    val tipo: String,
    val contenido: String,
    val orden: Int
)

data class TareaResultadoUiModel(
    val id: Long,
    val usuarioId: String,
    val tareaId: Long,
    val calificacion: Int,
    val aprobada: Boolean,
    val respuestasJson: String = ""
)

/**
 * Resuelve cualquier tipo de fuente de imagen (JSON {url:...}, Base64 crudo, data:image URI o HTTP/HTTPS)
 * retornando un Bitmap listo para renderizar o la URL limpia para Coil.
 */
fun resolveImageModel(raw: String): Any? {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return null

    // 1. Si es JSON tipo {"url": "...", "imagen": "..."}
    if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
        try {
            val json = JSONObject(trimmed)
            val extracted = json.optString("url", json.optString("imagen", json.optString("src", "")))
            if (extracted.isNotBlank()) return resolveImageModel(extracted)
        } catch (_: Exception) {}
    }

    // 2. Si es data URI con base64
    if (trimmed.startsWith("data:image", ignoreCase = true) && trimmed.contains("base64,")) {
        val base64Part = trimmed.substringAfter("base64,")
        return try {
            val bytes = Base64.decode(base64Part, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (_: Exception) {
            trimmed
        }
    }

    // 3. Si es base64 puro (comienza por cabeceras típicas jpeg/png/webp)
    if (trimmed.startsWith("/9j/") || trimmed.startsWith("iVBORw0") || trimmed.startsWith("UklGR") ||
        (trimmed.length > 80 && !trimmed.startsWith("http", ignoreCase = true) && !trimmed.startsWith("/"))
    ) {
        return try {
            val bytes = Base64.decode(trimmed, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (_: Exception) {
            trimmed
        }
    }

    // 4. URL estándar
    return trimmed
}

object InscripcionesRepository {

    private const val BASE = "/rest/v1"

    /**
     * Consulta los cursos reales en los que está inscrito el usuario desde la tabla `inscripciones`.
     * Idéntico a `inscripciones.service.ts → listMisCursos()` en la web.
     * Cruza con `lecciones_vistas` y `tareas_resultados` para calcular el progreso dinámico igual que la web.
     * Incorpora renovación de token automático y respaldo en caché local para que nunca desaparezcan los cursos.
     */
    suspend fun fetchMisCursos(
        userId: String,
        accessToken: String,
        refreshToken: String = "",
        sessionManager: SessionManager? = null
    ): List<EnrolledCourse> = withContext(Dispatchers.IO) {
        if (userId.isBlank()) return@withContext emptyList()

        var currentToken = accessToken
        val rawEnrolled = mutableListOf<EnrolledCourse>()

        // 1. Intento con join: /inscripciones?usuario_id=eq.$userId&select=*,curso:cursos(*)
        var result = SupabaseHttp.getArray(
            path = "$BASE/inscripciones?usuario_id=eq.$userId&select=*,curso:cursos(*)&order=id.asc",
            accessToken = currentToken
        )

        // Si el token expiró (HTTP 401), intentar refrescarlo si tenemos refresh_token
        if ((!result.isSuccess && (result.error?.contains("401") == true || result.error?.contains("JWT") == true)) && refreshToken.isNotBlank()) {
            val refreshResult = AuthRepository.refreshToken(refreshToken)
            if (refreshResult.isSuccess && refreshResult.data != null) {
                currentToken = refreshResult.data.accessToken
                sessionManager?.updateAccessToken(currentToken, refreshResult.data.refreshToken)
                // Reintentar con el nuevo token
                result = SupabaseHttp.getArray(
                    path = "$BASE/inscripciones?usuario_id=eq.$userId&select=*,curso:cursos(*)&order=id.asc",
                    accessToken = currentToken
                )
            }
        }

        if (result.isSuccess && result.data != null && result.data.length() > 0) {
            val arr = result.data
            for (i in 0 until arr.length()) {
                val row = arr.optJSONObject(i) ?: continue
                val inscId = row.optLong("id", 0)
                val progreso = row.optInt("progreso", 0)
                val cursoObj = row.optJSONObject("curso")
                if (cursoObj != null) {
                    val curso = CoursesRepository.parseCurso(cursoObj)
                    rawEnrolled.add(EnrolledCourse(inscId, curso, progreso))
                }
            }
        } else {
            // 2. Fallback: consultar solo inscripciones y cruzar con los cursos
            var fallbackResult = SupabaseHttp.getArray(
                path = "$BASE/inscripciones?usuario_id=eq.$userId&order=id.asc",
                accessToken = currentToken
            )
            // Si también falla por token, intentar anónimo como última alternativa de lectura
            if (!fallbackResult.isSuccess) {
                fallbackResult = SupabaseHttp.getArray(
                    path = "$BASE/inscripciones?usuario_id=eq.$userId&order=id.asc"
                )
            }

            if (fallbackResult.isSuccess && fallbackResult.data != null) {
                val allCourses = CoursesRepository.getCourses().associateBy { it.id }
                val arr = fallbackResult.data
                for (i in 0 until arr.length()) {
                    val row = arr.optJSONObject(i) ?: continue
                    val inscId = row.optLong("id", 0)
                    val cursoId = row.optLong("curso_id", 0)
                    val progreso = row.optInt("progreso", 0)
                    val curso = allCourses[cursoId] ?: CoursesRepository.getCourseDetail(cursoId)
                    if (curso != null) {
                        rawEnrolled.add(EnrolledCourse(inscId, curso, progreso))
                    }
                }
            }
        }

        // Si la red falló o devolvió vacío inesperadamente, rescatar del caché local guardado
        if (rawEnrolled.isEmpty()) {
            val cached = sessionManager?.getCachedCourses().orEmpty()
            if (cached.isNotEmpty()) return@withContext cached
            return@withContext emptyList()
        }

        // 3. Sincronizar con el progreso real basado en lecciones_vistas y tareas_resultados (como la web)
        val vistas = fetchLeccionesVistas(userId, currentToken)
        val tareasAprobadas = fetchTareasAprobadas(userId, currentToken)

        val computedEnrolled = rawEnrolled.map { item ->
            var finalProgreso = item.progreso
            try {
                val lecciones = fetchLecciones(item.curso.id)
                val tareas = fetchTareas(item.curso.id, item.curso.titulo, currentToken)
                val total = lecciones.size + tareas.size
                if (total > 0) {
                    val compLecciones = lecciones.count { vistas.contains(it.id) }
                    val compTareas = tareas.count { tareasAprobadas.contains(it.id) }
                    val calculado = (((compLecciones + compTareas).toFloat() / total.toFloat()) * 100).toInt().coerceIn(0, 100)
                    finalProgreso = maxOf(item.progreso, calculado)
                    if (finalProgreso > item.progreso) {
                        actualizarProgreso(userId, item.curso.id, finalProgreso, currentToken)
                    }
                }
            } catch (_: Exception) {}
            EnrolledCourse(item.inscripcionId, item.curso, finalProgreso)
        }

        // Guardar en caché local para que nunca desaparezcan al reiniciar la app
        sessionManager?.saveCachedCourses(computedEnrolled)

        computedEnrolled
    }

    /**
     * Inscribe al usuario en un curso en Supabase (`inscripciones`).
     */
    suspend fun inscribir(userId: String, cursoId: Long, accessToken: String): SupabaseResult<Boolean> =
        withContext(Dispatchers.IO) {
            val body = JSONObject().apply {
                put("usuario_id", userId)
                put("curso_id", cursoId)
                put("progreso", 0)
            }
            val result = SupabaseHttp.request(
                path = "$BASE/inscripciones",
                method = "POST",
                body = body,
                accessToken = accessToken,
                extraHeaders = mapOf("Prefer" to "resolution=merge-duplicates")
            )
            if (result.isSuccess) {
                SupabaseResult(data = true)
            } else {
                SupabaseResult(error = result.error ?: "Error al inscribirse al curso")
            }
        }

    /**
     * Actualiza el progreso de un curso para el alumno (0 a 100%) en la tabla `inscripciones`.
     */
    suspend fun actualizarProgreso(
        userId: String,
        cursoId: Long,
        progreso: Int,
        accessToken: String
    ): SupabaseResult<Boolean> = withContext(Dispatchers.IO) {
        val body = JSONObject().apply {
            put("progreso", progreso.coerceIn(0, 100))
        }
        val result = SupabaseHttp.patch(
            path = "$BASE/inscripciones?usuario_id=eq.$userId&curso_id=eq.$cursoId",
            body = body,
            accessToken = accessToken
        )
        if (result.isSuccess) {
            SupabaseResult(data = true)
        } else {
            SupabaseResult(error = result.error)
        }
    }

    /**
     * Obtiene los IDs de las lecciones vistas por el usuario desde `lecciones_vistas`.
     */
    suspend fun fetchLeccionesVistas(userId: String, accessToken: String): Set<Long> =
        withContext(Dispatchers.IO) {
            if (userId.isBlank() || accessToken.isBlank()) return@withContext emptySet()
            val result = SupabaseHttp.getArray(
                path = "$BASE/lecciones_vistas?usuario_id=eq.$userId&select=leccion_id",
                accessToken = accessToken
            )
            if (!result.isSuccess || result.data == null) return@withContext emptySet()
            val set = mutableSetOf<Long>()
            val arr = result.data
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val id = o.optLong("leccion_id", 0)
                if (id > 0) set.add(id)
            }
            set
        }

    /**
     * Marca una lección como vista en `lecciones_vistas`.
     */
    suspend fun marcarLeccionVista(
        userId: String,
        leccionId: Long,
        accessToken: String
    ): SupabaseResult<Boolean> = withContext(Dispatchers.IO) {
        val body = JSONObject().apply {
            put("usuario_id", userId)
            put("leccion_id", leccionId)
        }
        val result = SupabaseHttp.request(
            path = "$BASE/lecciones_vistas",
            method = "POST",
            body = body,
            accessToken = accessToken,
            extraHeaders = mapOf("Prefer" to "resolution=ignore-duplicates")
        )
        if (result.isSuccess) {
            SupabaseResult(data = true)
        } else {
            SupabaseResult(error = result.error)
        }
    }

    /**
     * Desmarca una lección eliminándola de `lecciones_vistas`.
     */
    suspend fun desmarcarLeccionVista(
        userId: String,
        leccionId: Long,
        accessToken: String
    ): SupabaseResult<Boolean> = withContext(Dispatchers.IO) {
        val result = SupabaseHttp.delete(
            path = "$BASE/lecciones_vistas?usuario_id=eq.$userId&leccion_id=eq.$leccionId",
            accessToken = accessToken
        )
        if (result.isSuccess) {
            SupabaseResult(data = true)
        } else {
            SupabaseResult(error = result.error)
        }
    }

    /**
     * Obtiene los IDs de las tareas aprobadas o completadas por el alumno desde `tareas_resultados`.
     */
    suspend fun fetchTareasAprobadas(userId: String, accessToken: String): Set<Long> =
        withContext(Dispatchers.IO) {
            if (userId.isBlank() || accessToken.isBlank()) return@withContext emptySet()
            val result = SupabaseHttp.getArray(
                path = "$BASE/tareas_resultados?usuario_id=eq.$userId&select=tarea_id,aprobada",
                accessToken = accessToken
            )
            if (!result.isSuccess || result.data == null) return@withContext emptySet()
            val set = mutableSetOf<Long>()
            val arr = result.data
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                if (o.optBoolean("aprobada", false)) {
                    val id = o.optLong("tarea_id", 0)
                    if (id > 0) set.add(id)
                }
            }
            set
        }

    /**
     * Obtiene el resultado guardado de una tarea específica para el alumno.
     */
    suspend fun fetchTareaResultado(
        userId: String,
        tareaId: Long,
        accessToken: String
    ): TareaResultadoUiModel? = withContext(Dispatchers.IO) {
        if (userId.isBlank() || accessToken.isBlank()) return@withContext null
        val result = SupabaseHttp.getArray(
            "$BASE/tareas_resultados?usuario_id=eq.$userId&tarea_id=eq.$tareaId&select=*",
            accessToken = accessToken
        )
        if (!result.isSuccess || result.data == null || result.data.length() == 0) return@withContext null
        val o = result.data.optJSONObject(0) ?: return@withContext null
        TareaResultadoUiModel(
            id = o.optLong("id", 0),
            usuarioId = o.optString("usuario_id", userId),
            tareaId = o.optLong("tarea_id", tareaId),
            calificacion = o.optInt("calificacion", 0),
            aprobada = o.optBoolean("aprobada", false),
            respuestasJson = o.opt("respuestas")?.toString() ?: ""
        )
    }

    /**
     * Guarda o actualiza el resultado de una tarea en `tareas_resultados`.
     */
    suspend fun guardarTareaResultado(
        userId: String,
        tareaId: Long,
        aprobada: Boolean,
        calificacion: Int = if (aprobada) 100 else 0,
        respuestas: JSONObject = JSONObject(),
        accessToken: String
    ): SupabaseResult<Boolean> = withContext(Dispatchers.IO) {
        val body = JSONObject().apply {
            put("usuario_id", userId)
            put("tarea_id", tareaId)
            put("calificacion", calificacion)
            put("aprobada", aprobada)
            put("respuestas", respuestas)
        }
        val result = SupabaseHttp.request(
            path = "$BASE/tareas_resultados",
            method = "POST",
            body = body,
            accessToken = accessToken,
            extraHeaders = mapOf("Prefer" to "resolution=merge-duplicates")
        )
        if (result.isSuccess) {
            SupabaseResult(data = true)
        } else {
            SupabaseResult(error = result.error)
        }
    }

    /**
     * Elimina el resultado de una tarea en `tareas_resultados`.
     */
    suspend fun eliminarTareaResultado(
        userId: String,
        tareaId: Long,
        accessToken: String
    ): SupabaseResult<Boolean> = withContext(Dispatchers.IO) {
        val result = SupabaseHttp.delete(
            path = "$BASE/tareas_resultados?usuario_id=eq.$userId&tarea_id=eq.$tareaId",
            accessToken = accessToken
        )
        if (result.isSuccess) {
            SupabaseResult(data = true)
        } else {
            SupabaseResult(error = result.error)
        }
    }

    /**
     * Obtiene las lecciones reales de un curso desde la tabla `lecciones` de Supabase.
     */
    suspend fun fetchLecciones(cursoId: Long): List<com.skillacademy.mobile.data.model.LessonUiModel> =
        withContext(Dispatchers.IO) {
            val result = SupabaseHttp.getArray(
                "$BASE/lecciones?curso_id=eq.$cursoId&order=orden.asc"
            )
            if (!result.isSuccess || result.data == null) return@withContext emptyList()
            val list = mutableListOf<com.skillacademy.mobile.data.model.LessonUiModel>()
            val arr = result.data
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                list.add(
                    com.skillacademy.mobile.data.model.LessonUiModel(
                        id              = o.optLong("id", 0),
                        orden           = o.optInt("orden", i + 1),
                        titulo          = o.optString("titulo", "Lección ${i + 1}"),
                        descripcion     = o.optString("descripcion", ""),
                        duracionMinutos = o.optInt("duracion_minutos", 0),
                        esGratis        = o.optBoolean("es_gratis", false)
                    )
                )
            }
            list
        }

    /**
     * Obtiene los bloques de contenido de una lección desde `contenido_leccion`.
     * Equivale a LeccionesService.getWithContenido() en la web Angular.
     */
    suspend fun fetchContenidoLeccion(leccionId: Long): List<ContenidoLeccionUiModel> =
        withContext(Dispatchers.IO) {
            val result = SupabaseHttp.getArray(
                "$BASE/contenido_leccion?leccion_id=eq.$leccionId&order=orden.asc"
            )
            if (!result.isSuccess || result.data == null) return@withContext emptyList()
            val list = mutableListOf<ContenidoLeccionUiModel>()
            val arr = result.data
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                list.add(
                    ContenidoLeccionUiModel(
                        id        = o.optLong("id", 0),
                        leccionId = o.optLong("leccion_id", leccionId),
                        tipo      = o.optString("tipo", "texto"),
                        contenido = o.optString("contenido", ""),
                        orden     = o.optInt("orden", i + 1)
                    )
                )
            }
            list
        }

    /**
     * Obtiene los bloques interactivos de una tarea desde `tarea_contenido`.
     * Equivale a TareasService.listContenido(tareaId) en la web Angular.
     */
    suspend fun fetchTareaContenido(tareaId: Long): List<TareaContenidoUiModel> =
        withContext(Dispatchers.IO) {
            val result = SupabaseHttp.getArray(
                "$BASE/tarea_contenido?tarea_id=eq.$tareaId&order=orden.asc"
            )
            if (!result.isSuccess || result.data == null) return@withContext emptyList()
            val list = mutableListOf<TareaContenidoUiModel>()
            val arr = result.data
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                list.add(
                    TareaContenidoUiModel(
                        id        = o.optLong("id", 0),
                        tareaId   = o.optLong("tarea_id", tareaId),
                        tipo      = o.optString("tipo", "texto"),
                        contenido = o.optString("contenido", ""),
                        orden     = o.optInt("orden", i + 1)
                    )
                )
            }
            list
        }

    /**
     * Obtiene las tareas reales asociadas al curso desde la tabla `tareas` de Supabase.
     */
    suspend fun fetchTareas(
        cursoId: Long,
        cursoTitulo: String = "",
        accessToken: String? = null
    ): List<TareaUiModel> = withContext(Dispatchers.IO) {
        var result = SupabaseHttp.getArray(
            "$BASE/tareas?curso_id=eq.$cursoId&order=id.asc",
            accessToken = accessToken
        )
        var arr = if (result.isSuccess) result.data else null

        if ((arr == null || arr.length() == 0) && cursoTitulo.isNotBlank()) {
            val encodedTitle = URLEncoder.encode(cursoTitulo, "UTF-8").replace("+", "%20")
            result = SupabaseHttp.getArray(
                "$BASE/tareas?curso=eq.$encodedTitle&order=id.asc",
                accessToken = accessToken
            )
            if (result.isSuccess && result.data != null && result.data.length() > 0) {
                arr = result.data
            }
        }

        if (arr == null || arr.length() == 0) {
            val allRes = SupabaseHttp.getArray("$BASE/tareas?order=id.asc", accessToken = accessToken)
            if (allRes.isSuccess && allRes.data != null) {
                val allArr = allRes.data
                val filtered = JSONArray()
                for (i in 0 until allArr.length()) {
                    val o = allArr.optJSONObject(i) ?: continue
                    val cId = o.optLong("curso_id", -1)
                    val cName = o.optString("curso", "")
                    if (cId == cursoId || (cursoTitulo.isNotBlank() && cName.equals(cursoTitulo, ignoreCase = true))) {
                        filtered.put(o)
                    }
                }
                if (filtered.length() > 0) arr = filtered
            }
        }

        if (arr == null) return@withContext emptyList()

        val list = mutableListOf<TareaUiModel>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val id = o.optLong("id", 0)
            val titulo = o.optString("titulo", "Tarea ${i + 1}")
            val fecha = o.optString("fecha", "")
            val prioridad = o.optString("prioridad", "Media")
            val estado = o.optString("estado", "Pendiente")
            val ponderacion = o.optInt("porcentaje_aprobacion", o.optInt("ponderacion", 60))

            var descripcion = o.optString("descripcion", "")
            if (descripcion.isBlank()) {
                val fechaLabel = if (fecha.isNotBlank()) "Fecha límite: $fecha" else "Sin fecha límite"
                descripcion = "$fechaLabel • Prioridad: $prioridad • Mínimo de aprobación: $ponderacion%"
            }

            list.add(
                TareaUiModel(
                    id            = id,
                    cursoId       = o.optLong("curso_id", cursoId),
                    titulo        = titulo,
                    descripcion   = descripcion,
                    tipoContenido = o.optString("tipo_contenido", "texto"),
                    ponderacion   = ponderacion,
                    estado        = estado
                )
            )
        }
        list
    }
}
