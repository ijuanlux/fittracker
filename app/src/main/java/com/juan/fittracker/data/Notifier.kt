package com.juan.fittracker.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

object Notifier {
    const val CHANNEL_REMINDERS = "workout_reminders"
    const val CHANNEL_ACHIEVEMENTS = "achievements"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val mgr = context.getSystemService(NotificationManager::class.java) ?: return
        mgr.createNotificationChannel(
            NotificationChannel(
                CHANNEL_REMINDERS,
                "Recordatorios de entreno",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = "Recordatorio diario para entrenar" },
        )
        mgr.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ACHIEVEMENTS,
                "Logros desbloqueados",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply { description = "Cuando ganas un nuevo logro" },
        )
    }

    fun hasPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun showReminder(context: Context, title: String, body: String) {
        ensureChannels(context)
        if (!hasPermission(context)) return
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP) }
        val pi = launchIntent?.let {
            PendingIntent.getActivity(
                context, 0, it,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        }
        val notif = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_menu_today)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(1001, notif) }
    }

    fun showAchievement(context: Context, achievement: Achievement) {
        ensureChannels(context)
        if (!hasPermission(context)) return
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP) }
        val pi = launchIntent?.let {
            PendingIntent.getActivity(
                context, achievement.id.hashCode(), it,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        }
        val notif = NotificationCompat.Builder(context, CHANNEL_ACHIEVEMENTS)
            .setSmallIcon(android.R.drawable.btn_star_big_on)
            .setContentTitle("${achievement.emoji} ${achievement.title}")
            .setContentText("${achievement.description} · +${achievement.xp} XP")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        runCatching {
            NotificationManagerCompat.from(context).notify(achievement.id.hashCode(), notif)
        }
    }
}
