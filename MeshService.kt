package com.example

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class MeshService : Service() {

    override fun onCreate() {
        super.onCreate()
        MeshRepository.init(this)
        
        createNotificationChannel()
        startForeground(1, createNotification())
        
        // Start scanning automatically when service starts
        MeshRepository.getConnectionManager().startAdvertising()
        MeshRepository.getConnectionManager().startDiscovery()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        MeshRepository.getConnectionManager().stopAll()
        MeshRepository.getAudioEngine().stopRecording()
        MeshRepository.getAudioEngine().stopPlaying()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "mesh_call_channel",
                "MeshCall Background Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val pendingIntent: PendingIntent =
            Intent(this, MainActivity::class.java).let { notificationIntent ->
                PendingIntent.getActivity(
                    this, 0, notificationIntent,
                    PendingIntent.FLAG_IMMUTABLE
                )
            }

        return NotificationCompat.Builder(this, "mesh_call_channel")
            .setContentTitle("MeshCall Active")
            .setContentText("Scanning for nearby devices...")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
}
