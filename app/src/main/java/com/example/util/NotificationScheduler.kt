package com.example.util

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.receiver.ReminderNotificationReceiver

object NotificationScheduler {

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun initChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                ReminderNotificationReceiver.CHANNEL_ID,
                "Tagada Follow-up Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Follow-up reminders for Tagada CRM contacts"
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun scheduleReminder(
        context: Context,
        reminderId: String,
        name: String,
        phone: String,
        note: String,
        timeMillis: Long
    ) {
        initChannel(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, ReminderNotificationReceiver::class.java).apply {
            putExtra(ReminderNotificationReceiver.EXTRA_REMINDER_ID, reminderId)
            putExtra(ReminderNotificationReceiver.EXTRA_NAME, name)
            putExtra(ReminderNotificationReceiver.EXTRA_PHONE, phone)
            putExtra(ReminderNotificationReceiver.EXTRA_NOTE, note)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerAt = if (timeMillis <= System.currentTimeMillis()) {
            System.currentTimeMillis() + 1000L // Trigger immediately if due now
        } else {
            timeMillis
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                pendingIntent
            )
        }
    }

    fun cancelReminder(context: Context, reminderId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderNotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun triggerImmediateNotification(
        context: Context,
        name: String,
        phone: String,
        note: String
    ) {
        val receiver = ReminderNotificationReceiver()
        val intent = Intent().apply {
            putExtra(ReminderNotificationReceiver.EXTRA_REMINDER_ID, UUID.randomUUID().toString())
            putExtra(ReminderNotificationReceiver.EXTRA_NAME, name)
            putExtra(ReminderNotificationReceiver.EXTRA_PHONE, phone)
            putExtra(ReminderNotificationReceiver.EXTRA_NOTE, note)
        }
        receiver.onReceive(context, intent)
    }
}
