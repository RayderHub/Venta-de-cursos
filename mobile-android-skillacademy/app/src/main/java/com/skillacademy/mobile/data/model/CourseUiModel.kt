package com.skillacademy.mobile.data.model

data class CourseUiModel(
    val id: Long,
    val titulo: String,
    val categoria: String,
    val nivel: String, // Principiante, Intermedio, Avanzado
    val instructor: String,
    val precio: Double,
    val oldPrecio: Double? = null,
    val imagen: String, // Clave o URL
    val descripcion: String,
    val rating: Double,
    val reviews: Int,
    val horas: Int = 12,
    val estudiantes: Int = 1250,
    val lecciones: List<LessonUiModel> = emptyList(),
    val isBestSeller: Boolean = false
) {
    val descuentoPorcentaje: Int?
        get() = if (oldPrecio != null && oldPrecio > precio) {
            (((oldPrecio - precio) / oldPrecio) * 100).toInt()
        } else null
}

data class LessonUiModel(
    val id: Long,
    val orden: Int,
    val titulo: String,
    val duracionMinutos: Int,
    val esGratis: Boolean = false,
    val completada: Boolean = false
)

data class BannerUiModel(
    val id: Long,
    val titulo: String,
    val subtitulo: String,
    val descuentoTexto: String,
    val imagenFondo: String, // Color o recurso
    val enlaceCursoId: Long? = null
)

data class CategoryUiModel(
    val id: String,
    val nombre: String,
    val icono: String, // excel, code, design, productivity
    val totalCursos: Int
)

data class CartItemUiModel(
    val curso: CourseUiModel,
    val fechaAgregado: Long = System.currentTimeMillis()
)
