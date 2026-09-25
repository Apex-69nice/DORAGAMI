package com.nikhil.warmlight

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat

class OverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var currentIntensity = 55

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                currentIntensity = intent.getIntExtra(EXTRA_INTENSITY, currentIntensity)
                startForeground(NOTIFICATION_ID, buildNotification())
                showOrUpdateOverlay()
            }
            ACTION_UPDATE -> {
                currentIntensity = intent.getIntExtra(EXTRA_INTENSITY, currentIntensity)
                showOrUpdateOverlay()
            }
            ACTION_STOP -> {
                removeOverlay()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun showOrUpdateOverlay() {
        val color = warmColorFor(currentIntensity)
        val existing = overlayView
        if (existing == null) {
            val view = View(this).apply { setBackgroundColor(color) }

            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                type,
                // NOT_FOCUSABLE + NOT_TOUCHABLE = the tint never intercepts your taps/typing
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply { gravity = Gravity.TOP or Gravity.START }

            windowManager?.addView(view, params)
            overlayView = view
        } else {
            existing.setBackgroundColor(color)
        }
    }

    private fun removeOverlay() {
        overlayView?.let {
            windowManager?.removeView(it)
            overlayView = null
        }
    }

    private fun buildNotification(): android.app.Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Warm night light", NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        val stopIntent = Intent(this, OverlayService::class.java).apply { action = ACTION_STOP }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openIntent = Intent(this, MainActivity::class.java)
        val openPendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentIntent(openPendingIntent)
            .addAction(0, getString(R.string.turn_off), stopPendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        removeOverlay()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "com.nikhil.warmlight.action.START"
        const val ACTION_STOP = "com.nikhil.warmlight.action.STOP"
        const val ACTION_UPDATE = "com.nikhil.warmlight.action.UPDATE"
        const val EXTRA_INTENSITY = "extra_intensity"
        private const val CHANNEL_ID = "warmlight_channel"
        private const val NOTIFICATION_ID = 1001

        /**
         * Maps a 0-100 warmth level to an ARGB overlay color.
         * Low values: a faint amber wash. High values: a deep, saturated orange —
         * this is what actually does the "cut the blue light" work, since a plain
         * overlay can only add warm color, not truly remove blue wavelengths.
         */
        fun warmColorFor(intensity: Int): Int {
            val level = intensity.coerceIn(0, 100)
            val fraction = level / 100f
            val alpha = (18 + fraction * 150).toInt().coerceIn(0, 255)
            val red = 255
            val green = (200 - fraction * 90).toInt().coerceIn(90, 200)
            val blue = (140 - fraction * 120).toInt().coerceIn(15, 140)
            return Color.argb(alpha, red, green, blue)
        }
    }
}
