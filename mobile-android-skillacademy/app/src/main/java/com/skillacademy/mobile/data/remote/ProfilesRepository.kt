package com.skillacademy.mobile.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Práctica 4 — Almacenamiento estructurado con Supabase
 *
 * Usa la tabla `profiles` de Supabase que es el repositorio del proyecto.
 * Equivalente a usuarios.service.ts en el frontend Angular.
 *
 * Operaciones implementadas:
 *  - obtenerTodos()    → Consultar usuarios registrados  (Caso 2)
 *  - insertar()        → Registrar usuario               (Caso 1)
 *  - actualizar()      → Modificar información           (Caso 3 / Caso 4)
 *  - eliminar()        → Eliminar usuario                (Caso 4)
 */
object ProfilesRepository {

    /**
     * CASO 2 — dao.obtenerTodos() → consultar todos los usuarios.
     * Equivale a: client.from('profiles').select('*').order('created_at')
     */
    suspend fun obtenerTodos(accessToken: String): SupabaseResult<List<SupabaseProfile>> =
        withContext(Dispatchers.IO) {
            val r = SupabaseHttp.getArray(
                path = "/rest/v1/profiles?select=id,email,full_name,role_id,is_active,avatar_url,bio&order=created_at.asc",
                accessToken = accessToken
            )
            if (!r.isSuccess) return@withContext SupabaseResult(error = r.error)

            val list = mutableListOf<SupabaseProfile>()
            val arr = r.data!!
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    SupabaseProfile(
                        id        = obj.optString("id", ""),
                        email     = obj.optString("email", ""),
                        fullName  = obj.optString("full_name", ""),
                        roleId    = obj.optString("role_id", "student"),
                        isActive  = obj.optBoolean("is_active", true),
                        avatarUrl = obj.optString("avatar_url").takeIf { it.isNotBlank() },
                        bio       = obj.optString("bio").takeIf { it.isNotBlank() }
                    )
                )
            }
            SupabaseResult(data = list)
        }

    /**
     * CASO 1 — Insertar usuario / estudiante (Registrar estudiantes).
     * Registra el estudiante en Supabase Auth (/auth/v1/signup), activando
     * el trigger PostgreSQL handle_new_user() que inserta automáticamente
     * en la tabla `profiles`. Actualiza carrera y edad en el campo `bio`.
     */
    suspend fun insertarEstudiante(
        nombre: String,
        correo: String,
        carrera: String,
        edad: String,
        accessToken: String
    ): SupabaseResult<String> = withContext(Dispatchers.IO) {
        val cleanNombre = nombre.trim()
        val cleanEmail = if (correo.isNotBlank()) {
            correo.trim().lowercase()
        } else {
            val randomSuffix = (1000..9999).random()
            val safeName = cleanNombre.lowercase().replace("[^a-z0-9]".toRegex(), "")
            "${safeName.ifBlank { "estudiante" }}_$randomSuffix@skillacademy.com"
        }

        val body = JSONObject().apply {
            put("email", cleanEmail)
            put("password", "Password123!")
            put("data", JSONObject().apply {
                put("full_name", cleanNombre)
                put("role_id", "student")
            })
        }

        val result = SupabaseHttp.post("/auth/v1/signup", body)
        if (!result.isSuccess) {
            val err = result.error ?: "Error al registrar"
            val readableErr = when {
                err.contains("already been registered", ignoreCase = true) -> "El correo ya está registrado"
                err.contains("valid email", ignoreCase = true) -> "Formato de correo inválido"
                else -> err
            }
            return@withContext SupabaseResult(error = readableErr)
        }

        val userObj = result.data?.optJSONObject("user")
        val userId = userObj?.optString("id", "") ?: ""

        val bioParts = mutableListOf<String>()
        if (carrera.isNotBlank()) bioParts.add(carrera.trim())
        if (edad.isNotBlank()) bioParts.add("Edad: ${edad.trim()}")

        if (userId.isNotBlank() && bioParts.isNotEmpty()) {
            val bioString = bioParts.joinToString(" | ")
            actualizarBio(userId, bioString, accessToken)
        }

        SupabaseResult(data = "Estudiante $cleanNombre registrado correctamente")
    }

    /**
     * CASO 1 / 3 — Actualizar datos de usuario.
     */
    suspend fun actualizarNombreYRol(
        userId: String,
        fullName: String,
        roleId: String,
        accessToken: String
    ): SupabaseResult<String> = withContext(Dispatchers.IO) {
        val body = JSONObject().apply {
            put("full_name", fullName.trim())
            put("role_id", roleId)
        }
        SupabaseHttp.patch(
            path = "/rest/v1/profiles?id=eq.$userId",
            body = body,
            accessToken = accessToken
        )
    }

    /**
     * CASO 3 — Actualizar campo profesión/rol de un usuario.
     * Equivale a cambiarRol() en usuarios.service.ts
     */
    suspend fun actualizarRol(
        userId: String,
        nuevoRol: String,
        accessToken: String
    ): SupabaseResult<String> = withContext(Dispatchers.IO) {
        val body = JSONObject().apply {
            put("role_id", nuevoRol)
        }
        SupabaseHttp.patch(
            path = "/rest/v1/profiles?id=eq.$userId",
            body = body,
            accessToken = accessToken
        )
    }

    /**
     * CASO 4 — Actualizar campo "correo" (bio) y otros campos de la Entity.
     * En Supabase la tabla `profiles` tiene el campo `bio` que sirve de
     * "profesion/correo" adicional para la práctica.
     */
    suspend fun actualizarBio(
        userId: String,
        bio: String,
        accessToken: String
    ): SupabaseResult<String> = withContext(Dispatchers.IO) {
        val body = JSONObject().apply {
            put("bio", bio.trim())
        }
        SupabaseHttp.patch(
            path = "/rest/v1/profiles?id=eq.$userId",
            body = body,
            accessToken = accessToken
        )
    }

    /**
     * CASO 4 — Eliminar usuario.
     * Equivale a eliminarUsuario() en usuarios.service.ts
     * Solo disponible para admins (protegido por RLS en Supabase).
     */
    suspend fun eliminar(userId: String, accessToken: String): SupabaseResult<String> =
        withContext(Dispatchers.IO) {
            SupabaseHttp.delete(
                path = "/rest/v1/profiles?id=eq.$userId",
                accessToken = accessToken
            )
        }
}
