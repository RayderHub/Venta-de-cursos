package com.skillacademy.mobile.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Credenciales Supabase del proyecto SkillAcademy
 * Las mismas que usa el backend-fastify y el frontend Angular
 */
object SupabaseConfig {
    const val URL  = "https://uudjczjsobvqpyvuarhk.supabase.co"
    const val ANON = "sb_publishable_uivXUxKRnq7zc0TYmLoBGQ_uXf4q2Jf"
}

// ─── Resultado genérico ───────────────────────────────────────────────────────

data class SupabaseResult<T>(
    val data: T? = null,
    val error: String? = null
) {
    val isSuccess get() = error == null
}

// ─── HTTP helper ─────────────────────────────────────────────────────────────

object SupabaseHttp {

    /** Realiza una petición REST y devuelve el body como String o un error. */
    suspend fun request(
        path: String,
        method: String = "GET",
        body: JSONObject? = null,
        accessToken: String? = null,
        extraHeaders: Map<String, String> = emptyMap()
    ): SupabaseResult<String> = withContext(Dispatchers.IO) {
        try {
            val url = URL("${SupabaseConfig.URL}$path")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = method
            conn.connectTimeout = 12_000
            conn.readTimeout    = 12_000
            conn.setRequestProperty("apikey",       SupabaseConfig.ANON)
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Prefer",       "return=representation")
            if (accessToken != null) {
                conn.setRequestProperty("Authorization", "Bearer $accessToken")
            } else {
                conn.setRequestProperty("Authorization", "Bearer ${SupabaseConfig.ANON}")
            }
            extraHeaders.forEach { (k, v) -> conn.setRequestProperty(k, v) }

            if (body != null) {
                conn.doOutput = true
                OutputStreamWriter(conn.outputStream, "UTF-8").use { it.write(body.toString()) }
            }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = BufferedReader(InputStreamReader(stream, "UTF-8")).use { it.readText() }

            if (code in 200..299) {
                SupabaseResult(data = text)
            } else {
                SupabaseResult(error = "HTTP $code: $text")
            }
        } catch (e: Exception) {
            SupabaseResult(error = e.message ?: "Error de conexión")
        }
    }

    /** Envía body como JSON y espera JSONObject de vuelta. */
    suspend fun post(path: String, body: JSONObject, accessToken: String? = null): SupabaseResult<JSONObject> {
        val r = request(path, "POST", body, accessToken)
        return if (r.isSuccess) {
            try {
                val data = r.data!!
                // Puede ser array o objeto según el endpoint
                val obj = if (data.trimStart().startsWith("[")) {
                    JSONArray(data).optJSONObject(0) ?: JSONObject()
                } else {
                    JSONObject(data)
                }
                SupabaseResult(data = obj)
            } catch (e: Exception) {
                SupabaseResult(error = "JSON parse error: ${e.message}")
            }
        } else {
            SupabaseResult(error = r.error)
        }
    }

    /** GET que devuelve JSONArray. */
    suspend fun getArray(path: String, accessToken: String? = null): SupabaseResult<JSONArray> {
        val r = request(path, "GET", accessToken = accessToken)
        return if (r.isSuccess) {
            try {
                SupabaseResult(data = JSONArray(r.data!!))
            } catch (e: Exception) {
                SupabaseResult(error = "JSON parse error: ${e.message}")
            }
        } else {
            SupabaseResult(error = r.error)
        }
    }

    /** PATCH (actualizar parcialmente). */
    suspend fun patch(path: String, body: JSONObject, accessToken: String? = null): SupabaseResult<String> =
        request(path, "PATCH", body, accessToken)

    /** DELETE. */
    suspend fun delete(path: String, accessToken: String? = null): SupabaseResult<String> =
        request(path, "DELETE", accessToken = accessToken)
}
