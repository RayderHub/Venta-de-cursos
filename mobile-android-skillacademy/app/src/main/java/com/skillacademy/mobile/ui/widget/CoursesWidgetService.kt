package com.skillacademy.mobile.ui.widget

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.skillacademy.mobile.R
import com.skillacademy.mobile.data.local.SessionManager

/**
 * CoursesWidgetService.kt
 *
 * Servicio de RemoteViews para abastecer el carrusel StackView del widget transparente.
 * Provee la lista de cursos pendientes por completar con sus porcentajes y datos visuales.
 */
class CoursesWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return CoursesRemoteViewsFactory(applicationContext)
    }
}

class CoursesRemoteViewsFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {

    data class WidgetCourseItem(
        val id: Long,
        val titulo: String,
        val categoria: String,
        val nivel: String,
        val instructor: String,
        val progreso: Int
    )

    private val pendingCourses = mutableListOf<WidgetCourseItem>()

    override fun onCreate() {
        loadPendingCourses()
    }

    override fun onDataSetChanged() {
        loadPendingCourses()
    }

    private fun loadPendingCourses() {
        pendingCourses.clear()

        // 1. Intentar cargar cursos reales inscritos por el usuario desde SessionManager
        try {
            val sessionManager = SessionManager(context)
            val cached = sessionManager.getCachedCourses()
            if (cached.isNotEmpty()) {
                val incomplete = cached.filter { it.progreso < 100 }
                val targetList = if (incomplete.isNotEmpty()) incomplete else cached
                for (enrolled in targetList) {
                    pendingCourses.add(
                        WidgetCourseItem(
                            id = enrolled.curso.id,
                            titulo = enrolled.curso.titulo,
                            categoria = enrolled.curso.categoria,
                            nivel = enrolled.curso.nivel,
                            instructor = enrolled.curso.instructor,
                            progreso = enrolled.progreso
                        )
                    )
                }
            }
        } catch (_: Exception) {}

        // 2. Si no hay cursos inscritos aún, mostrar cursos activos de muestra
        if (pendingCourses.isEmpty()) {
            pendingCourses.add(
                WidgetCourseItem(
                    id = 1,
                    titulo = "Excel Avanzado: Fórmulas y Dashboards",
                    categoria = "Excel",
                    nivel = "Avanzado",
                    instructor = "Prof. Laura Gómez",
                    progreso = 65
                )
            )
            pendingCourses.add(
                WidgetCourseItem(
                    id = 2,
                    titulo = "Desarrollo Frontend Práctico",
                    categoria = "Programación",
                    nivel = "Intermedio",
                    instructor = "Ing. Carlos Ruiz",
                    progreso = 40
                )
            )
            pendingCourses.add(
                WidgetCourseItem(
                    id = 3,
                    titulo = "Diseño UI para Productos Digitales",
                    categoria = "Diseño",
                    nivel = "Inicial",
                    instructor = "Lic. Ana Morales",
                    progreso = 20
                )
            )
            pendingCourses.add(
                WidgetCourseItem(
                    id = 4,
                    titulo = "Automatización de Reportes en Excel",
                    categoria = "Excel",
                    nivel = "Intermedio",
                    instructor = "Prof. Laura Gómez",
                    progreso = 10
                )
            )
        }
    }

    override fun onDestroy() {
        pendingCourses.clear()
    }

    override fun getCount(): Int = pendingCourses.size

    override fun getViewAt(position: Int): RemoteViews {
        if (position < 0 || position >= pendingCourses.size) {
            return RemoteViews(context.packageName, R.layout.widget_course_card_item)
        }

        val item = pendingCourses[position]
        val views = RemoteViews(context.packageName, R.layout.widget_course_card_item)

        views.setTextViewText(R.id.widget_item_category, item.categoria)
        views.setTextViewText(R.id.widget_item_level, item.nivel)
        views.setTextViewText(R.id.widget_item_title, item.titulo)
        views.setTextViewText(R.id.widget_item_instructor, item.instructor)
        views.setProgressBar(R.id.widget_item_progress_bar, 100, item.progreso, false)
        views.setTextViewText(R.id.widget_item_progress_text, "${item.progreso}% completado")

        // Fill-in Intent para que al tocar la tarjeta se lance MainActivity con los datos del curso
        val fillInIntent = Intent().apply {
            putExtra(CoursesPendingWidgetProvider.EXTRA_COURSE_ID, item.id)
        }
        views.setOnClickFillInIntent(R.id.widget_card_container, fillInIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = true
}
