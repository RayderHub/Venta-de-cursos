package com.skillacademy.mobile.data.security

import java.io.File

/**
 * RootDetector.kt
 * Detección de binarios de superusuario (su) y entornos vulnerados
 * bajo ISO/IEC 27001:2022 A.8.8 / A.8.9 (Defecto D6-03)
 */
object RootDetector {

    private val rootPaths = arrayOf(
        "/system/app/Superuser.apk",
        "/sbin/su",
        "/system/bin/su",
        "/system/xbin/su",
        "/data/local/xbin/su",
        "/data/local/bin/su",
        "/system/sd/xbin/su"
    )

    fun isDeviceRooted(): Boolean {
        return checkRootBinaries() || checkTestKeys() || canExecuteSuCommand()
    }

    private fun checkRootBinaries(): Boolean {
        for (path in rootPaths) {
            if (File(path).exists()) return true
        }
        return false
    }

    private fun checkTestKeys(): Boolean {
        val buildTags = android.os.Build.TAGS
        return buildTags != null && buildTags.contains("test-keys")
    }

    private fun canExecuteSuCommand(): Boolean {
        var process: Process? = null
        return try {
            process = Runtime.getRuntime().exec(arrayOf("/system/xbin/which", "su"))
            val inReader = java.io.BufferedReader(java.io.InputStreamReader(process.inputStream))
            inReader.readLine() != null
        } catch (t: Throwable) {
            false
        } finally {
            process?.destroy()
        }
    }
}

