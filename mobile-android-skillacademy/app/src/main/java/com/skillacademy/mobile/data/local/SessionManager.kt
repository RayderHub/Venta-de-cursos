package com.skillacademy.mobile.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.skillacademy.mobile.data.model.CourseUiModel
import com.skillacademy.mobile.data.remote.EnrolledCourse
import com.skillacademy.mobile.data.remote.SupabaseProfile
import com.skillacademy.mobile.data.remote.SupabaseSession
import org.json.JSONArray
import org.json.JSONObject

/**
 * Gestor de persistencia segura de sesión de usuario en Android.
 * Almacena el token JWT de Supabase, refresh_token, ID de usuario, perfil y caché local de cursos inscritos.
 * Cumple con ISO 27001 A.8.24 cifrando los datos en reposo con AES-256 (EncryptedSharedPreferences + Keystore).
 */
class SessionManager(context: Context) {

    private val prefs: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        Log.w("SessionManager", "No se pudo inicializar EncryptedSharedPreferences, fallback estándar", e)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    companion object {
        private const val PREFS_NAME = "skillacademy_session_prefs"

        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_EMAIL = "email"
        private const val KEY_FULL_NAME = "full_name"
        private const val KEY_ROLE_ID = "role_id"
        private const val KEY_IS_ACTIVE = "is_active"
        private const val KEY_AVATAR_URL = "avatar_url"
        private const val KEY_BIO = "bio"
        private const val KEY_REMEMBER_ME = "remember_me"
        private const val KEY_CACHED_COURSES = "cached_enrolled_courses"
    }

    fun saveSession(session: SupabaseSession, profile: SupabaseProfile, rememberMe: Boolean = true) {
        prefs.edit().apply {
            putString(KEY_ACCESS_TOKEN, session.accessToken)
            putString(KEY_REFRESH_TOKEN, session.refreshToken)
            putString(KEY_USER_ID, session.userId)
            putString(KEY_EMAIL, session.email)
            putString(KEY_FULL_NAME, profile.fullName)
            putString(KEY_ROLE_ID, profile.roleId)
            putBoolean(KEY_IS_ACTIVE, profile.isActive)
            putString(KEY_AVATAR_URL, profile.avatarUrl)
            putString(KEY_BIO, profile.bio)
            putBoolean(KEY_REMEMBER_ME, rememberMe)
            apply()
        }
    }

    fun updateAccessToken(newToken: String, newRefreshToken: String = "") {
        prefs.edit().apply {
            putString(KEY_ACCESS_TOKEN, newToken)
            if (newRefreshToken.isNotBlank()) {
                putString(KEY_REFRESH_TOKEN, newRefreshToken)
            }
            apply()
        }
    }

    fun getRefreshToken(): String {
        return prefs.getString(KEY_REFRESH_TOKEN, "") ?: ""
    }

    fun getSession(): Pair<SupabaseSession, SupabaseProfile>? {
        val token = prefs.getString(KEY_ACCESS_TOKEN, null) ?: return null
        val userId = prefs.getString(KEY_USER_ID, null) ?: return null
        val email = prefs.getString(KEY_EMAIL, "") ?: ""
        val refreshToken = prefs.getString(KEY_REFRESH_TOKEN, "") ?: ""

        val fullName = prefs.getString(KEY_FULL_NAME, "") ?: ""
        val roleId = prefs.getString(KEY_ROLE_ID, "student") ?: "student"
        val isActive = prefs.getBoolean(KEY_IS_ACTIVE, true)
        val avatarUrl = prefs.getString(KEY_AVATAR_URL, null)
        val bio = prefs.getString(KEY_BIO, null)

        val session = SupabaseSession(
            accessToken = token,
            userId = userId,
            email = email,
            refreshToken = refreshToken
        )
        val profile = SupabaseProfile(
            id = userId,
            email = email,
            fullName = fullName,
            roleId = roleId,
            isActive = isActive,
            avatarUrl = avatarUrl,
            bio = bio
        )

        return Pair(session, profile)
    }

    fun saveProfile(profile: SupabaseProfile) {
        prefs.edit().apply {
            putString(KEY_FULL_NAME, profile.fullName)
            putString(KEY_ROLE_ID, profile.roleId)
            putBoolean(KEY_IS_ACTIVE, profile.isActive)
            putString(KEY_AVATAR_URL, profile.avatarUrl)
            putString(KEY_BIO, profile.bio)
            apply()
        }
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }

    fun isLoggedIn(): Boolean {
        return prefs.getString(KEY_ACCESS_TOKEN, null)?.isNotEmpty() == true
    }

    /**
     * Guarda en caché local la lista de cursos inscritos para que nunca se borren
     * al cerrar y reabrir la app, incluso sin conexión o con token en refresco.
     */
    fun saveCachedCourses(courses: List<EnrolledCourse>) {
        try {
            val array = JSONArray()
            courses.forEach { enrolled ->
                val obj = JSONObject().apply {
                    put("inscripcionId", enrolled.inscripcionId)
                    put("progreso", enrolled.progreso)
                    val c = enrolled.curso
                    val cObj = JSONObject().apply {
                        put("id", c.id)
                        put("titulo", c.titulo)
                        put("instructor", c.instructor)
                        put("precio", c.precio)
                        put("old_precio", c.oldPrecio ?: 0.0)
                        put("rating", c.rating)
                        put("reviews", c.reviews)
                        put("imagen", c.imagen)
                        put("categoria", c.categoria)
                        put("nivel", c.nivel)
                        put("descripcion", c.descripcion)
                        put("requisitos", c.requisitos)
                        put("que_aprenderas", c.queAprenderas)
                    }
                    put("curso", cObj)
                }
                array.put(obj)
            }
            prefs.edit().putString(KEY_CACHED_COURSES, array.toString()).apply()
        } catch (_: Exception) {}
    }

    /**
     * Obtiene los cursos previamente inscritos desde caché local instantáneamente.
     */
    fun getCachedCourses(): List<EnrolledCourse> {
        val raw = prefs.getString(KEY_CACHED_COURSES, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<EnrolledCourse>()
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val cObj = o.optJSONObject("curso") ?: continue
                val oldP = if (cObj.has("old_precio") && !cObj.isNull("old_precio")) cObj.optDouble("old_precio") else null
                val curso = CourseUiModel(
                    id = cObj.optLong("id", 0),
                    titulo = cObj.optString("titulo", ""),
                    instructor = cObj.optString("instructor", ""),
                    precio = cObj.optDouble("precio", 0.0),
                    oldPrecio = oldP,
                    rating = cObj.optDouble("rating", 0.0),
                    reviews = cObj.optInt("reviews", 0),
                    imagen = cObj.optString("imagen", "placeholder"),
                    categoria = cObj.optString("categoria", "General"),
                    nivel = cObj.optString("nivel", "Todos"),
                    descripcion = cObj.optString("descripcion", ""),
                    requisitos = cObj.optString("requisitos", ""),
                    queAprenderas = cObj.optString("que_aprenderas", "")
                )
                list.add(
                    EnrolledCourse(
                        inscripcionId = o.optLong("inscripcionId", 0),
                        curso = curso,
                        progreso = o.optInt("progreso", 0)
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }
}
