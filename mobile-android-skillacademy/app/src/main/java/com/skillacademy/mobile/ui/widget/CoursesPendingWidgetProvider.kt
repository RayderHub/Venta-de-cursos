package com.skillacademy.mobile.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.skillacademy.mobile.MainActivity
import com.skillacademy.mobile.R

/**
 * CoursesPendingWidgetProvider.kt
 *
 * Proveedor del widget transparente para la pantalla de inicio (HomeScreen).
 * Muestra un carrusel interactivo en 3D (StackView) deslizable con el dedo,
 * presentando los cursos pendientes del usuario.
 * Al tocar cualquier tarjeta se abre la aplicación para continuar el curso.
 */
class CoursesPendingWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH_WIDGET) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, CoursesPendingWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_stack_view)
        }
    }

    companion object {
        const val ACTION_REFRESH_WIDGET = "com.skillacademy.mobile.ACTION_REFRESH_WIDGET"
        const val EXTRA_COURSE_ID = "com.skillacademy.mobile.EXTRA_COURSE_ID"

        fun refreshAllWidgets(context: Context) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val componentName = ComponentName(context, CoursesPendingWidgetProvider::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                if (appWidgetIds.isNotEmpty()) {
                    appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_stack_view)
                }
            } catch (_: Exception) {}
        }

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_pending_courses_stack)

            // Vincular el servicio del carrusel StackView
            val serviceIntent = Intent(context, CoursesWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            views.setRemoteAdapter(R.id.widget_stack_view, serviceIntent)
            views.setEmptyView(R.id.widget_stack_view, R.id.widget_empty_view)

            // Configurar la acción al tocar cualquier tarjeta del carrusel (Abre MainActivity)
            val clickIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val clickPendingIntent = PendingIntent.getActivity(
                context,
                0,
                clickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            views.setPendingIntentTemplate(R.id.widget_stack_view, clickPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_root, clickPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_stack_view)
        }
    }
}
