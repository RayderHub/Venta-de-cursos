package com.skillacademy.mobile.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

/**
 * Entregable 3 — Sección 3.4: Notificaciones Push & Promocionales
 *
 * Administra los canales de notificación y el despacho de alertas de promociones flash,
 * cupones especiales y avisos de progreso en los cursos.
 */
object SkillNotificationHelper {

    private const val CHANNEL_PROMOS_ID = "skillacademy_promos_channel"
    private const val CHANNEL_PROMOS_NAME = "Promociones y Descuentos SkillAcademy"

    fun initChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_PROMOS_ID,
                CHANNEL_PROMOS_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Alertas de ofertas flash, cupones de descuento y novedades de cursos"
                enableLights(true)
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    fun showPromoNotification(context: Context, title: String, message: String) {
        initChannels(context)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val builder = NotificationCompat.Builder(context, CHANNEL_PROMOS_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        manager.notify(101, builder.build())
    }
}
