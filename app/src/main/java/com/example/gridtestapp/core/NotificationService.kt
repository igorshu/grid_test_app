package com.example.gridtestapp.core

import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_HIGH
import android.content.Context
import android.graphics.Bitmap
import androidx.core.app.NotificationCompat
import com.example.gridtestapp.R
import org.koin.dsl.module

class NotificationService {

        private fun prebuildNotification(context: Context): NotificationCompat.Builder {
            val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL)
                .setContentTitle("Grid Notification")
                .setContentText("This is our notification!")
                .setSmallIcon(R.drawable.splash_icon)
                .setPriority(IMPORTANCE_HIGH)
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .setAutoCancel(false)

            return notification
        }

        fun showAppNotification(context: Context) {
            val notification = prebuildNotification(context)
                .build()
            val notificationManager = context.getSystemService(NotificationManager::class.java)

            notificationManager.notify(NOTIFICATION_ID, notification)
        }

        fun showImageNotification(context: Context, bitmap: Bitmap?, url: String) {
            val notification = prebuildNotification(context)
                .setContentText(url)
                .setStyle(
                    NotificationCompat
                        .BigPictureStyle()
                        .bigPicture(bitmap)
                        .bigLargeIcon(null as Bitmap?)
                )
                .build()
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.notify(NOTIFICATION_ID, notification)
        }

        fun hideNotification(context: Context) {
            context.getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
        }

    companion object {

        const val NOTIFICATION_CHANNEL = "app_notification"
        private const val NOTIFICATION_ID = 1001

        val module = module {
            single { NotificationService() }
        }
    }
}