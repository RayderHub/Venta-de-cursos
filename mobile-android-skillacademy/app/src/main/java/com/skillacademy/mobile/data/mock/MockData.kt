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
            titulo = "Excel Avanzado: Fórmulas y Dashboards",
            categoria = "Excel",
            nivel = "Avanzado",
            instructor = "Prof. Laura Gómez",
            precio = 49.99,
            oldPrecio = 79.99,
            imagen = "excel",
            descripcion = "Aprende a construir dashboards ejecutivos, tablas dinámicas avanzadas y automatizaciones con macros y Power Query en Excel.",
            rating = 4.9,
            reviews = 142,
            horas = 16,
            estudiantes = 1890,
            isBestSeller = true,
            lecciones = listOf(
                LessonUiModel(1, 1, "Introducción al modelado de datos en Excel", 15, esGratis = true),
                LessonUiModel(2, 2, "Fórmulas matriciales y BUSCARX avanzado", 25, esGratis = true),
                LessonUiModel(3, 3, "Construcción de tablas dinámicas multidimensionales", 30),
                LessonUiModel(4, 4, "Diseño de KPIs y visualizaciones de impacto", 40),
                LessonUiModel(5, 5, "Automatización con Power Query paso a paso", 35)
            )
        ),
        CourseUiModel(
            id = 2,
            titulo = "TypeScript & Fastify Backend Master",
            categoria = "Programación",
            nivel = "Intermedio",
            instructor = "Ing. Carlos Rivera",
            precio = 59.99,
            oldPrecio = 89.99,
            imagen = "code",
            descripcion = "Desarrolla APIs REST de alto rendimiento con Fastify, Node.js y TypeScript integradas con PostgreSQL y Elasticsearch.",
            rating = 4.8,
            reviews = 98,
            horas = 20,
            estudiantes = 1240,
            isBestSeller = true,
            lecciones = listOf(
                LessonUiModel(1, 1, "Fundamentos de TypeScript moderno", 20, esGratis = true),
                LessonUiModel(2, 2, "Arquitectura de un servidor Fastify", 30, esGratis = true),
                LessonUiModel(3, 3, "Validación de esquemas con TypeBox", 25),
                LessonUiModel(4, 4, "Conexión a Supabase y PostgreSQL", 35),
                LessonUiModel(5, 5, "Indexación de datos en Elasticsearch", 45)
            )
        ),
        CourseUiModel(
            id = 3,
            titulo = "Diseño UI/UX con Figma de Cero a Pro",
            categoria = "Diseño",
            nivel = "Principiante",
            instructor = "Elena Morales",
            precio = 39.99,
            oldPrecio = 69.99,
            imagen = "design",
            descripcion = "Domina Figma desde los conceptos básicos de diseño visual hasta prototipos interactivos y Design Systems completos.",
            rating = 4.9,
            reviews = 215,
            horas = 14,
            estudiantes = 2300,
            isBestSeller = true,
            lecciones = listOf(
                LessonUiModel(1, 1, "Principios de diseño visual y tipografía", 18, esGratis = true),
                LessonUiModel(2, 2, "Auto-layout y componentes reutilizables", 32, esGratis = true),
                LessonUiModel(3, 3, "Creación de un Design System corporativo", 40),
                LessonUiModel(4, 4, "Prototipado interactivo avanzado", 35)
            )
        ),
        CourseUiModel(
            id = 4,
            titulo = "Automatización y Productividad con Python",
            categoria = "Productividad",
            nivel = "Intermedio",
            instructor = "David Soto",
            precio = 44.99,
            oldPrecio = 74.99,
            imagen = "laptop",
            descripcion = "Automatiza tareas repetitivas de oficina: procesar hojas de cálculo, envío de correos y scraping web con scripts de Python.",
            rating = 4.7,
            reviews = 84,
            horas = 12,
            estudiantes = 950,
            isBestSeller = false,
            lecciones = listOf(
                LessonUiModel(1, 1, "Configuración del entorno de automatización", 15, esGratis = true),
                LessonUiModel(2, 2, "Lectura y edición masiva de archivos Excel", 28),
                LessonUiModel(3, 3, "Extracción de datos web automatizada", 35)
            )
        ),
        CourseUiModel(
            id = 5,
            titulo = "Power BI & Análisis de Datos Empresarial",
            categoria = "Excel",
            nivel = "Intermedio",
            instructor = "Prof. Laura Gómez",
            precio = 54.99,
            oldPrecio = 84.99,
            imagen = "screen",
            descripcion = "Transforma datos crudos en dashboards ejecutivos con Power BI, lenguaje DAX y modelado relacional en estrella.",
            rating = 4.9,
            reviews = 156,
            horas = 18,
            estudiantes = 1620,
            isBestSeller = false,
            lecciones = listOf(
                LessonUiModel(1, 1, "Introducción al Business Intelligence", 20, esGratis = true),
                LessonUiModel(2, 2, "Modelado relacional y medidas DAX", 35),
                LessonUiModel(3, 3, "Publicación y compartición en Power BI Service", 25)
            )
        )
    )
}
