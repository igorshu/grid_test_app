package com.example.gridtestapp.core

import android.Manifest
import android.app.Activity
import android.app.Activity.NOTIFICATION_SERVICE
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_DEFAULT
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.gridtestapp.R
import org.koin.dsl.module

class NotificationsManager {

    private var builder: NotificationCompat.Builder? = null

    private fun prebuildNotification(context: Context): NotificationCompat.Builder {
        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL)
            .setContentTitle("Grid Notification")
            .setContentText("This is our notification!")
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(IMPORTANCE_DEFAULT)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setAutoCancel(false)

        return notification
    }

    fun showAppNotification(context: Context) {
        builder = prebuildNotification(context)

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, builder?.build())
    }

    fun showImageNotification(context: Context, bitmap: Bitmap?, url: String) {
        builder = prebuildNotification(context)
            .setContentText(url)
            .setStyle(
                NotificationCompat
                    .BigPictureStyle()
                    .bigPicture(bitmap)
                    .bigLargeIcon(null as Bitmap?)
            )

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, builder?.build())
    }

    fun hideNotification(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder?.let {
                it.setTimeoutAfter(5_000)

                val notificationManager = context.getSystemService(NotificationManager::class.java)
                notificationManager.notify(NOTIFICATION_ID, it.build())
            }
        } else {
            cancelNotifications(context)
        }

    }

    fun cancelNotifications(context: Context) {
        builder = null
        context.getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
    }

    fun requestPermissions(activity: ComponentActivity) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.S_V2) {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                getNotificationPermission(activity)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun getNotificationPermission(activity: Activity) {
        ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.POST_NOTIFICATIONS), PERMISSION_REQUEST_CODE)
    }

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(NOTIFICATION_CHANNEL, "Application Notification", IMPORTANCE_DEFAULT)
            notificationChannel.description = "description"

            val notificationManager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }

    companion object {

        const val NOTIFICATION_CHANNEL = "app_notification"
        private const val NOTIFICATION_ID = 1001
        const val PERMISSION_REQUEST_CODE = 10101

        val module = module {
            single { NotificationsManager() }
        }
    }
}