package com.example.data.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.data.model.AlertSeverity
import com.example.data.model.WeatherAlert

class WeatherNotificationManager(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "weather_severe_alerts_channel"
        const val CHANNEL_NAME = "Severe Weather Alerts"
        private val notifiedAlertIds = mutableSetOf<String>()
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "Critical notices and severe weather warnings for your area"
                enableLights(true)
                enableVibration(true)
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun postSevereAlertNotification(alert: WeatherAlert) {
        if (notifiedAlertIds.contains(alert.id)) {
            return // Already notified for this exact alert event
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            alert.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val iconRes = android.R.drawable.ic_dialog_alert

        val title = when (alert.severity) {
            AlertSeverity.WARNING -> "⚠️ ${alert.event} (WARNING)"
            AlertSeverity.WATCH -> "⚡ ${alert.event} (WATCH)"
            AlertSeverity.ADVISORY -> "ℹ️ ${alert.event} (ADVISORY)"
        }

        val content = if (alert.headline.isNotBlank()) alert.headline else alert.description.take(120)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(iconRes)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText("${alert.headline}\n\n${alert.description}"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            NotificationManagerCompat.from(context).notify(alert.id.hashCode(), builder.build())
            notifiedAlertIds.add(alert.id)
        } catch (e: SecurityException) {
            // Handled safely
        }
    }
}
