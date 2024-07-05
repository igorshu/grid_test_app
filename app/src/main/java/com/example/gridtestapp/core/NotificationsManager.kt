package com.example.gridtestapp.core

import android.Manifest
import android.app.Activity
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_DEFAULT
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

class NotificationsManager(application: Application) {

    private var builder: NotificationCompat.Builder =
        NotificationCompat
            .Builder(application, NOTIFICATION_CHANNEL)
            .setContentTitle("Grid Notification")
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(IMPORTANCE_DEFAULT)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setAutoCancel(false)

    private var notificationManager: NotificationManager = application.getSystemService(NotificationManager::class.java)

    fun showAppNotification() {
        builder.apply {
            setTimeoutAfter(0)
            setContentText("This is our notification!")
            setStyle(null)
            notificationManager.notify(NOTIFICATION_ID, build())
        }
    }

    fun showImageNotification(bitmap: Bitmap?, url: String) {
        builder.apply {
            setTimeoutAfter(0)
            setContentText(url)
            setStyle(
                NotificationCompat
                    .BigPictureStyle()
                    .bigPicture(bitmap)
                    .bigLargeIcon(null as Bitmap?))
            notificationManager.notify(NOTIFICATION_ID, build())
        }
    }

    fun hideNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.apply {
                setTimeoutAfter(5_000)
                notificationManager.notify(NOTIFICATION_ID, build())
            }
        } else {
            cancelNotifications()
        }

    }

    fun cancelNotifications() {
        notificationManager.cancel(NOTIFICATION_ID)
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

    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(NOTIFICATION_CHANNEL, "Application Notification", IMPORTANCE_DEFAULT)
            notificationChannel.description = "description"

            notificationManager.createNotificationChannel(notificationChannel)
        }
    }

    fun showResumeNotification() {
        notificationManager.apply {
            cancel(NOTIFICATION_ID)
            notify(NOTIFICATION_ID, builder.run {
                setTimeoutAfter(0)
                build()
            })
        }
    }

    companion object {

        const val NOTIFICATION_CHANNEL = "app_notification"
        private const val NOTIFICATION_ID = 1001
        const val PERMISSION_REQUEST_CODE = 10101

        val module = module {
            single { NotificationsManager(get()) }
        }
    }
}