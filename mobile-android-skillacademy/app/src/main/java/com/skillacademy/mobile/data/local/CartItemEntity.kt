package com.skillacademy.mobile.data.local

import com.skillacademy.mobile.data.model.CourseUiModel

data class CartItemEntity(
    val id: Long = 0,
    val cursoId: Long,
    val titulo: String,
    val categoria: String = "",
    val nivel: String = "Intermedio",
    val instructor: String = "",
    val precio: Double = 0.0,
    val oldPrecio: Double? = null,
    val imagen: String = "",
    val descripcion: String = "",
    val rating: Double = 4.8,
    val reviews: Int = 10,
    val fechaAgregado: Long = System.currentTimeMillis()
) {
    fun toCourseUiModel(): CourseUiModel {
        return CourseUiModel(
            id = cursoId,
            titulo = titulo,
            categoria = categoria,
            nivel = nivel,
            instructor = instructor,
            precio = precio,
            oldPrecio = oldPrecio,
            imagen = imagen,
            descripcion = descripcion,
            rating = rating,
            reviews = reviews
        )
    }

    companion object {
        fun fromCourseUiModel(course: CourseUiModel): CartItemEntity {
            return CartItemEntity(
                cursoId = course.id,
                titulo = course.titulo,
                categoria = course.categoria,
                nivel = course.nivel,
                instructor = course.instructor,
                precio = course.precio,
                oldPrecio = course.oldPrecio,
                imagen = course.imagen,
                descripcion = course.descripcion,
                rating = course.rating,
                reviews = course.reviews
            )
        }
    }
}
