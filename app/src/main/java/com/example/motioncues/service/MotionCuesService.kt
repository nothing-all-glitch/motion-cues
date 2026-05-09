package com.example.motioncues.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.example.motioncues.R
import com.example.motioncues.prefs.MotionMode
import com.example.motioncues.prefs.PrefsManager
import com.example.motioncues.sensor.MotionState
import com.example.motioncues.sensor.VehicleMotionDetector
import com.example.motioncues.view.MotionCuesView

class MotionCuesService : Service() {

    private lateinit var prefsManager: PrefsManager
    private lateinit var windowManager: WindowManager
    private lateinit var motionDetector: VehicleMotionDetector
    private lateinit var motionCuesView: MotionCuesView

    private val motionUpdateHandler = Handler(Looper.getMainLooper())
    private var isOverlayVisible = false

    private val motionUpdateRunnable = object : Runnable {
        override fun run() {
            if (isOverlayVisible) {
                motionCuesView.updateMotion(motionDetector.getMotionVector())
            }
            motionUpdateHandler.postDelayed(this, MOTION_UPDATE_INTERVAL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        prefsManager = PrefsManager(this)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())

        motionCuesView = MotionCuesView(this)
        motionCuesView.setPrefsManager(prefsManager)

        motionDetector = VehicleMotionDetector(this) { motionState ->
            Log.d(TAG, "Motion state changed: $motionState")
            if (prefsManager.motionMode == MotionMode.AUTO) {
                when (motionState) {
                    MotionState.IN_VEHICLE -> showOverlay()
                    MotionState.STATIONARY -> hideOverlay()
                }
            }
        }

        motionDetector.start()

        applyCurrentMode()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand action: ${intent?.action}")
        intent?.action?.let { action ->
            when (action) {
                ACTION_SHOW -> showOverlay()
                ACTION_HIDE -> hideOverlay()
                ACTION_UPDATE_APPEARANCE -> {
                    motionCuesView.updateAppearance()
                    applyCurrentMode()
                }
                ACTION_MODE_CHANGED -> {
                    applyCurrentMode()
                }
                ACTION_STOP -> stopSelf()
            }
        }
        return START_STICKY
    }

    private fun applyCurrentMode() {
        val mode = prefsManager.motionMode
        Log.d(TAG, "Applying mode: $mode, overlayVisible=$isOverlayVisible")
        when (mode) {
            MotionMode.ON -> showOverlay()
            MotionMode.AUTO -> {
            }
            MotionMode.OFF -> {
                hideOverlay()
                stopSelf()
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        motionUpdateHandler.removeCallbacks(motionUpdateRunnable)
        motionDetector.stop()
        hideOverlay()
        super.onDestroy()
    }

    private fun showOverlay() {
        if (isOverlayVisible) return
        try {
            Log.d(TAG, "showOverlay: adding view")
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
            }

            windowManager.addView(motionCuesView, params)
            isOverlayVisible = true
            motionCuesView.setAnimating(true)
            motionUpdateHandler.post(motionUpdateRunnable)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add overlay", e)
        }
    }

    private fun hideOverlay() {
        if (!isOverlayVisible) return
        try {
            Log.d(TAG, "hideOverlay: removing view")
            motionUpdateHandler.removeCallbacks(motionUpdateRunnable)
            motionCuesView.setAnimating(false)
            windowManager.removeView(motionCuesView)
            isOverlayVisible = false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove overlay", e)
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notification_channel_description)
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .build()
    }

    companion object {
        private const val TAG = "MotionCuesService"
        const val CHANNEL_ID = "motion_cues_channel"
        const val NOTIFICATION_ID = 1001

        private const val MOTION_UPDATE_INTERVAL_MS = 33L

        const val ACTION_SHOW = "com.example.motioncues.ACTION_SHOW"
        const val ACTION_HIDE = "com.example.motioncues.ACTION_HIDE"
        const val ACTION_UPDATE_APPEARANCE = "com.example.motioncues.ACTION_UPDATE"
        const val ACTION_MODE_CHANGED = "com.example.motioncues.ACTION_MODE_CHANGED"
        const val ACTION_STOP = "com.example.motioncues.ACTION_STOP"
    }
}
