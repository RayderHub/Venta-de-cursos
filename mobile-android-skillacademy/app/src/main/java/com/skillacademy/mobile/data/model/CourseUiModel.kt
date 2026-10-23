package com.skillacademy.mobile.data.model

/**
 * Modelo principal de curso con datos completos de Supabase.
 * Incluye galería de imágenes, lecciones, reseñas y detalles del instructor.
 */
data class CourseUiModel(
    val id: Long,
    val titulo: String,
    val categoria: String,
    val nivel: String,               // Principiante, Intermedio, Avanzado
    val instructor: String,
    val instructorId: String = "",
    val precio: Double,
    val oldPrecio: Double? = null,
    val imagen: String,              // Imagen principal (clave o URL)
    val imagenesGaleria: List<String> = emptyList(), // Carrusel de imágenes reales
    val descripcion: String,
    val requisitos: String = "",     // Campo nuevo de Supabase
    val queAprenderas: String = "",  // Campo nuevo de Supabase
    val rating: Double,
    val reviews: Int,
    val horas: Int = 0,
    val estudiantes: Int = 0,
    val lecciones: List<LessonUiModel> = emptyList(),
    val resenas: List<ReviewUiModel> = emptyList(),
    val instructorPerfil: InstructorUiModel? = null,
    val isBestSeller: Boolean = false
) {
    val descuentoPorcentaje: Int?
        get() = if (oldPrecio != null && oldPrecio > precio) {
            (((oldPrecio - precio) / oldPrecio) * 100).toInt()
        } else null

    /** Todas las imágenes del carrusel: primero galería, luego la principal como fallback */
    val todasImagenes: List<String>
        get() = if (imagenesGaleria.isNotEmpty()) imagenesGaleria else listOf(imagen)
}

/** Lección individual de un curso */
data class LessonUiModel(
    val id: Long,
    val orden: Int,
    val titulo: String,
    val duracionMinutos: Int = 0,
    val descripcion: String = "",
    val esGratis: Boolean = false,
    val completada: Boolean = false
)

/** Reseña de un alumno (view curso_reviews_public de Supabase) */
data class ReviewUiModel(
    val id: Long,
    val rating: Int,
    val comment: String?,
    val reviewerName: String,
    val reviewerAvatar: String?,
    val reviewerRole: String,
    val createdAt: String
)

/** Perfil público del instructor */
data class InstructorUiModel(
    val id: String,
    val fullName: String,
    val avatarUrl: String?,
    val email: String = ""
)

data class BannerUiModel(
    val id: Long,
    val titulo: String,
    val subtitulo: String,
    val descuentoTexto: String,
    val imagenFondo: String,
    val enlaceCursoId: Long? = null
)

data class CategoryUiModel(
    val id: String,
    val nombre: String,
    val icono: String,
    val totalCursos: Int
)

data class CartItemUiModel(
    val curso: CourseUiModel,
    val fechaAgregado: Long = System.currentTimeMillis()
)
