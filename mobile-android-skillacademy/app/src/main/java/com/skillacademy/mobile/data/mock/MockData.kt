package com.skillacademy.mobile.data.mock

import com.skillacademy.mobile.data.model.BannerUiModel
import com.skillacademy.mobile.data.model.CategoryUiModel
import com.skillacademy.mobile.data.model.CourseUiModel
import com.skillacademy.mobile.data.model.LessonUiModel

object MockData {
    val categories = listOf(
        CategoryUiModel("excel", "Excel", "table", 8),
        CategoryUiModel("programacion", "Programación", "code", 14),
        CategoryUiModel("diseno", "Diseño", "palette", 6),
        CategoryUiModel("productividad", "Productividad", "bolt", 5)
    )

    val banners = listOf(
        BannerUiModel(
            id = 1,
            titulo = "Oferta Especial de Lanzamiento",
            subtitulo = "Domina Excel y Dashboards ejecutivos con 35% de descuento.",
            descuentoTexto = "35% OFF",
            imagenFondo = "emerald_banner",
            enlaceCursoId = 1
        ),
        BannerUiModel(
            id = 2,
            titulo = "Ruta de Programación Web",
            subtitulo = "Aprende TypeScript, Angular y Node.js con proyectos reales.",
            descuentoTexto = "25% OFF",
            imagenFondo = "navy_banner",
            enlaceCursoId = 2
        ),
        BannerUiModel(
            id = 3,
            titulo = "Diseño UI/UX Profesional",
            subtitulo = "Crea interfaces modernas con Figma y sistemas de diseño.",
            descuentoTexto = "40% OFF",
            imagenFondo = "purple_banner",
            enlaceCursoId = 3
        )
    )

    val courses = listOf(
        CourseUiModel(
            id = 1,
            titulo = "Excel Avanzado",
            categoria = "Excel",
            nivel = "Avanzado",
            instructor = "Nora Excel",
            precio = 49.99,
            oldPrecio = 79.99,
            imagen = "excel",
            descripcion = "Aprende a construir dashboards ejecutivos, tablas dinámicas avanzadas y automatizaciones en Excel.",
            rating = 4.9,
            reviews = 142,
            horas = 16,
            estudiantes = 1890,
            isBestSeller = true,
            lecciones = emptyList()
        ),
        CourseUiModel(
            id = 2,
            titulo = "Automatizacion de Reportes en Excel",
            categoria = "Excel",
            nivel = "Intermedio",
            instructor = "Nora Excel",
            precio = 59.99,
            oldPrecio = 89.99,
            imagen = "excel",
            descripcion = "Automatiza reportes masivos y procesos de consolidación de datos con macros y Power Query.",
            rating = 4.8,
            reviews = 98,
            horas = 20,
            estudiantes = 1240,
            isBestSeller = true,
            lecciones = emptyList()
        ),
        CourseUiModel(
            id = 3,
            titulo = "Desarrollo Frontend Practico",
            categoria = "Programacion",
            nivel = "Principiante",
            instructor = "Renato Codigo",
            precio = 39.99,
            oldPrecio = 69.99,
            imagen = "code",
            descripcion = "Domina el desarrollo frontend con JavaScript, HTML5 y CSS moderno creando aplicaciones web interactivas.",
            rating = 4.9,
            reviews = 215,
            horas = 14,
            estudiantes = 2300,
            isBestSeller = true,
            lecciones = emptyList()
        ),
        CourseUiModel(
            id = 4,
            titulo = "Angular para Dashboards",
            categoria = "Programacion",
            nivel = "Intermedio",
            instructor = "Renato Codigo",
            precio = 44.99,
            oldPrecio = 74.99,
            imagen = "code",
            descripcion = "Crea paneles administrativos profesionales y dashboards interactivos con Angular 19 y TypeScript.",
            rating = 4.7,
            reviews = 84,
            horas = 12,
            estudiantes = 950,
            isBestSeller = false,
            lecciones = emptyList()
        ),
        CourseUiModel(
            id = 5,
            titulo = "Diseno UI para Productos Digitales",
            categoria = "Diseno",
            nivel = "Intermedio",
            instructor = "Lucia Diseno",
            precio = 54.99,
            oldPrecio = 84.99,
            imagen = "design",
            descripcion = "Diseña interfaces intuitivas y atractivas para aplicaciones móviles y plataformas web.",
            rating = 4.9,
            reviews = 156,
            horas = 18,
            estudiantes = 1620,
            isBestSeller = false,
            lecciones = emptyList()
        ),
        CourseUiModel(
            id = 6,
            titulo = "Branding Visual Express",
            categoria = "Diseno",
            nivel = "Principiante",
            instructor = "Lucia Diseno",
            precio = 34.99,
            oldPrecio = 59.99,
            imagen = "design",
            descripcion = "Construye identidades visuales memorables, sistemas de color y manuales de marca desde cero.",
            rating = 4.8,
            reviews = 72,
            horas = 10,
            estudiantes = 810,
            isBestSeller = false,
            lecciones = emptyList()
        ),
        CourseUiModel(
            id = 7,
            titulo = "Curso Cheems",
            categoria = "Programacion",
            nivel = "Intermedio",
            instructor = "Job De la vega",
            precio = 49.99,
            oldPrecio = 69.99,
            imagen = "code",
            descripcion = "Curso especializado creado por el docente Job De la vega con lecciones interactivas.",
            rating = 5.0,
            reviews = 12,
            horas = 8,
            estudiantes = 110,
            isBestSeller = true,
            lecciones = listOf(
                LessonUiModel(1, 1, "ejemplo", 20, esGratis = true, descripcion = "ejemplo2"),
                LessonUiModel(2, 2, "Leccion 2", 30, esGratis = false, descripcion = "dgfgdfgfd")
            )
        ),
        CourseUiModel(
            id = 8,
            titulo = "ejemplo2",
            categoria = "Programacion",
            nivel = "Principiante",
            instructor = "Job De la vega",
            precio = 29.99,
            oldPrecio = 49.99,
            imagen = "code",
            descripcion = "Curso práctico de introducción a la programación estructurada.",
            rating = 4.5,
            reviews = 5,
            horas = 6,
            estudiantes = 45,
            isBestSeller = false,
            lecciones = emptyList()
        )
    )
}
