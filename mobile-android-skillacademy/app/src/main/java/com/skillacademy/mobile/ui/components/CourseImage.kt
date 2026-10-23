package com.skillacademy.mobile.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.skillacademy.mobile.ui.theme.SkillNavy

/**
 * Mapeo temático de imágenes cuando el curso tiene una clave en vez de URL/Base64.
 * Mismas temáticas que maneja el frontend web en courseImagePath.
 */
object CourseImageHelper {
    fun resolveImageUrl(image: String?): String? {
        if (image.isNullOrBlank()) return null
        val trimmed = image.trim()
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed
        }
        if (trimmed.startsWith("data:image/")) {
            return null // Se maneja como Bitmap decodificado
        }
        return when (trimmed.lowercase()) {
            "excel"   -> "https://images.unsplash.com/photo-1551288049-bebda4e38f71?w=800&auto=format&fit=crop&q=80"
            "code"    -> "https://images.unsplash.com/photo-1555066931-4365d14bab8c?w=800&auto=format&fit=crop&q=80"
            "laptop"  -> "https://images.unsplash.com/photo-1498050108023-c5249f4df085?w=800&auto=format&fit=crop&q=80"
            "screen"  -> "https://images.unsplash.com/photo-1460925895917-afdab827c52f?w=800&auto=format&fit=crop&q=80"
            "design"  -> "https://images.unsplash.com/photo-1581291518857-4e27b48ff24e?w=800&auto=format&fit=crop&q=80"
            "purple"  -> "https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?w=800&auto=format&fit=crop&q=80"
            "banner"  -> "https://images.unsplash.com/photo-1517694712202-14dd9538aa97?w=800&auto=format&fit=crop&q=80"
            else      -> "https://images.unsplash.com/photo-1516321497487-e288fb19713f?w=800&auto=format&fit=crop&q=80"
        }
    }

    fun decodeBase64Bitmap(dataUri: String): Bitmap? {
        return try {
            val base64Index = dataUri.indexOf("base64,")
            val base64Str = if (base64Index != -1) {
                dataUri.substring(base64Index + 7)
            } else {
                dataUri
            }
            val decodedBytes = Base64.decode(base64Str, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (_: Exception) {
            null
        }
    }
}

/**
 * Componente unificado para renderizar la miniatura / portada real de cualquier curso:
 *  - Soporta imágenes Base64 creadas desde el dashboard del profesor (data:image/...)
 *  - Soporta URLs directas de internet (https://...)
 *  - Soporta claves de temas ("excel", "code", "design", etc.)
 *  - Fallback elegante con degradado si la imagen falla o está vacía.
 */
@Composable
fun CourseImage(
    imageSource: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val src = imageSource?.trim() ?: ""

    if (src.startsWith("data:image/")) {
        val bitmap = remember(src) { CourseImageHelper.decodeBase64Bitmap(src) }
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = modifier
            )
            return
        }
    }

    val resolvedUrl = remember(src) { CourseImageHelper.resolveImageUrl(src) }

    if (!resolvedUrl.isNullOrBlank()) {
        AsyncImage(
            model = resolvedUrl,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier
        )
    } else {
        // Fallback visual agradable
        Box(
            modifier = modifier.background(
                Brush.linearGradient(
                    colors = listOf(SkillNavy, Color(0xFF1E293B))
                )
            ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Image,
                contentDescription = contentDescription,
                tint = Color.White.copy(alpha = 0.4f)
            )
        }
    }
}
