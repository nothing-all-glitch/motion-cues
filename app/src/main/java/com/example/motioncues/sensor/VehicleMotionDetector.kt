package com.example.motioncues.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import kotlin.math.abs
import kotlin.math.sqrt

class VehicleMotionDetector(
    context: Context,
    private val onMotionChanged: (MotionState) -> Unit
) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val linearAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
    private val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile private var linearAccelX = 0f
    @Volatile private var linearAccelY = 0f
    @Volatile private var linearAccelZ = 0f
    @Volatile private var rotationMagnitude = 0f

    @Volatile private var smoothedLateral = 0f
    @Volatile private var smoothedLongitudinal = 0f
    @Volatile private var smoothedRotation = 0f

    @Volatile private var isVehicleMotionDetected = false

    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            when (event.sensor.type) {
                Sensor.TYPE_LINEAR_ACCELERATION -> {
                    linearAccelX = event.values[0]
                    linearAccelY = event.values[1]
                    linearAccelZ = event.values[2]
                }

                Sensor.TYPE_GYROSCOPE -> {
                    val x = event.values[0]
                    val y = event.values[1]
                    val z = event.values[2]
                    rotationMagnitude = sqrt(x * x + y * y + z * z)
                }
            }
            detectMotion()
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    private fun detectMotion() {
        val enterThreshold = 0.4f
        val exitThreshold = 0.2f
        val smoothing = 0.12f

        smoothedLateral += (linearAccelX - smoothedLateral) * smoothing
        smoothedLongitudinal += (linearAccelY - smoothedLongitudinal) * smoothing
        smoothedRotation += (rotationMagnitude - smoothedRotation) * smoothing

        val lateralStrength = abs(smoothedLateral)
        val longitudinalStrength = abs(smoothedLongitudinal)
        val rotationStrength = smoothedRotation.coerceAtMost(2f)
        val motionSignal = lateralStrength * 0.45f + longitudinalStrength * 0.30f + rotationStrength * 0.25f

        val wasDetected = isVehicleMotionDetected
        isVehicleMotionDetected = if (wasDetected) {
            motionSignal > exitThreshold
        } else {
            motionSignal > enterThreshold
        }

        if (isVehicleMotionDetected != wasDetected) {
            val state = if (isVehicleMotionDetected) MotionState.IN_VEHICLE else MotionState.STATIONARY
            mainHandler.post { onMotionChanged(state) }
        }
    }

    fun getMotionVector(): MotionVector {
        return MotionVector(
            lateral = smoothedLateral,
            longitudinal = smoothedLongitudinal,
            rotation = smoothedRotation
        )
    }

    fun start() {
        smoothedLateral = 0f
        smoothedLongitudinal = 0f
        smoothedRotation = 0f
        linearAccelerometer?.let {
            sensorManager.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_UI)
        }
        gyroscope?.let {
            sensorManager.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(sensorListener)
    }
}

data class MotionVector(
    val lateral: Float,
    val longitudinal: Float,
    val rotation: Float
)

enum class MotionState {
    IN_VEHICLE,
    STATIONARY
}
