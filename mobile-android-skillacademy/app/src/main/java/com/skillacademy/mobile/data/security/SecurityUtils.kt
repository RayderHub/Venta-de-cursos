package com.skillacademy.mobile.data.security

/**
 * SecurityUtils.kt
 * Utilidades de sanitización y validación de seguridad bajo ISO/IEC 27001 A.8.28
 */
object SecurityUtils {

    /**
     * Sanitiza parámetros de entrada para búsquedas de cursos o cupones,
     * previniendo ataques de inyección SQL o enumeración indebida de recursos REST.
     */
    fun sanitizeSearchQuery(input: String): String {
        // Eliminar caracteres peligrosos para inyecciones REST/SQL
        val clean = input.replace(Regex("[^a-zA-Z0-9áéíóúÁÉÍÓÚñÑ\\s]"), "")
        return clean.trim().take(50)
    }

    /**
     * Valida la entropía y robustez de contraseñas:
     * Requiere al menos 8 caracteres, mayúscula, minúscula, número y carácter especial.
     */
    fun isValidPasswordEntropy(pwd: String): Boolean {
        val regex = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,32}$")
        return regex.matches(pwd)
    }
}
