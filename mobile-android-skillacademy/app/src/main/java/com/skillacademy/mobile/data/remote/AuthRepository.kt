package com.skillacademy.mobile.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

// ─── Modelos de sesión ────────────────────────────────────────────────────────

data class SupabaseSession(
    val accessToken: String,
    val userId: String,
    val email: String
)

data class SupabaseProfile(
    val id: String,
    val email: String,
    val fullName: String,
    val roleId: String,       // "student" | "teacher" | "admin"
    val isActive: Boolean,
    val avatarUrl: String?,
    val bio: String? = null
) {
    val rolLabel get() = when (roleId) {
        "admin"   -> "Administrador"
        "teacher" -> "Profesor"
        else      -> "Estudiante"
    }
    val roleColor get() = when (roleId) {
        "admin"   -> 0xFFEF4444.toInt()   // rojo
        "teacher" -> 0xFF8B5CF6.toInt()   // morado
        else      -> 0xFF0D9488.toInt()   // esmeralda
    }
}

// ─── Repositorio de autenticación ─────────────────────────────────────────────

object AuthRepository {

    /**
     * Inicia sesión con email + contraseña mediante la API REST de Supabase Auth.
     * Misma lógica que auth.service.ts → signIn() en el frontend web.
     */
    suspend fun signIn(email: String, password: String): SupabaseResult<SupabaseSession> =
        withContext(Dispatchers.IO) {
            val body = JSONObject().apply {
                put("email", email.trim())
                put("password", password)
            }
            val result = SupabaseHttp.post(
                path = "/auth/v1/token?grant_type=password",
                body = body
            )
            if (!result.isSuccess) return@withContext SupabaseResult(error = result.error)

            val json = result.data!!
            // Supabase devuelve { access_token, user: { id, email } }
            val token  = json.optString("access_token", "")
            val userObj = json.optJSONObject("user")
            val userId = userObj?.optString("id", "") ?: ""
            val userEmail = userObj?.optString("email", email) ?: email

            if (token.isEmpty() || userId.isEmpty()) {
                return@withContext SupabaseResult(error = "Invalid login credentials")
            }
            SupabaseResult(data = SupabaseSession(token, userId, userEmail))
        }

    /**
     * Carga el perfil desde la tabla `profiles` igual que auth.service.ts → loadProfile().
     */
    suspend fun loadProfile(userId: String, accessToken: String): SupabaseResult<SupabaseProfile> =
        withContext(Dispatchers.IO) {
            val result = SupabaseHttp.getArray(
                path = "/rest/v1/profiles?id=eq.$userId&select=*&limit=1",
                accessToken = accessToken
            )
            if (!result.isSuccess) return@withContext SupabaseResult(error = result.error)

            val arr  = result.data!!
            if (arr.length() == 0) return@withContext SupabaseResult(error = "Perfil no encontrado")

            val obj = arr.getJSONObject(0)
            SupabaseResult(
                data = SupabaseProfile(
                    id        = obj.optString("id", userId),
                    email     = obj.optString("email", ""),
                    fullName  = obj.optString("full_name", ""),
                    roleId    = obj.optString("role_id", "student"),
                    isActive  = obj.optBoolean("is_active", true),
                    avatarUrl = obj.optString("avatar_url").takeIf { it.isNotBlank() },
                    bio       = obj.optString("bio").takeIf { it.isNotBlank() }
                )
            )
        }
}
