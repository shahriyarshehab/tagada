package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R

class ReminderNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getStringExtra(EXTRA_REMINDER_ID) ?: ""
        val name = intent.getStringExtra(EXTRA_NAME) ?: "Contact"
        val phone = intent.getStringExtra(EXTRA_PHONE) ?: ""
        val note = intent.getStringExtra(EXTRA_NOTE) ?: "Follow-up required"

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create Channel on API 26+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Tagada Follow-up Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for Tagada CRM customer follow-ups and due alerts"
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Tap intent to open app
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            reminderId.hashCode(),
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("📞 Follow-up: $name")
            .setContentText(if (note.isNotBlank()) "$note ($phone)" else "Time to call $name ($phone)")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(if (note.isNotBlank()) "$note\nPhone: $phone" else "Follow-up scheduled with $name.\nPhone: $phone")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)

        // Add Quick "Call Now" Action button if phone is present
        if (phone.isNotBlank()) {
            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
            val dialPendingIntent = PendingIntent.getActivity(
                context,
                reminderId.hashCode() + 1,
                dialIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(
                android.R.drawable.ic_menu_call,
                "Call Now",
                dialPendingIntent
            )
        }

        val notificationId = if (reminderId.isNotBlank()) reminderId.hashCode() else System.currentTimeMillis().toInt()
        try {
            notificationManager.notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            // In case notification permission was revoked
        }
    }

    companion object {
        const val CHANNEL_ID = "tagada_reminders"
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_NAME = "extra_name"
        const val EXTRA_PHONE = "extra_phone"
        const val EXTRA_NOTE = "extra_note"
    }
}
