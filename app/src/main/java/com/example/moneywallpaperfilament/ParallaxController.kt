package com.example.moneywallpaperfilament

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.HandlerThread
import kotlin.math.PI
import kotlin.math.exp


class ParallaxController(context: Context) {

    private val sensorManager =
        context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val sensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    val hasSensor: Boolean
        get() = sensor != null


    @Volatile
    var sensitivity: Float = 1f

    private var enabled = false

    private var sensorThread: HandlerThread? = null
    private var sensorHandler: Handler? = null

    private val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) = handleSensorEvent(event)
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    private var hasBaseline = false
    private var baselineAzimuth = 0f
    private var baselinePitch = 0f

    @Volatile
    private var targetX = 0f
    @Volatile
    private var targetY = 0f
    private var smoothX = 0f
    private var smoothY = 0f


    private val maxAngle = (20.0 * PI / 180.0).toFloat()
    private val piF = PI.toFloat()
    private val twoPiF = 2f * PI.toFloat()

    fun setEnabled(value: Boolean) {
        if (enabled == value) return
        enabled = value
        if (value) {

            hasBaseline = false
            startListening()
        } else {
            stopListening()
            targetX = 0f
            targetY = 0f
        }
    }

    fun onResume() {
        if (enabled) {
            hasBaseline = false
            startListening()
        }
    }

    fun onPause() = stopListening()

    fun release() {
        stopListening()
        enabled = false
    }

    private fun startListening() {
        val s = sensor ?: return
        if (sensorThread != null) return
        val thread = HandlerThread("ParallaxSensor").also { it.start() }
        sensorThread = thread
        val handler = Handler(thread.looper)
        sensorHandler = handler
        if (!sensorManager.registerListener(
                listener,
                s,
                SensorManager.SENSOR_DELAY_GAME,
                handler
            )
        ) {
            stopListening()
        }
    }

    private fun stopListening() {
        sensorManager.unregisterListener(listener)
        sensorThread?.quitSafely()
        sensorThread = null
        sensorHandler = null
    }


    private fun handleSensorEvent(event: SensorEvent) {
        val values = event.values
        if (values == null || values.size < 3) return
        for (v in values) {
            if (!v.isFinite()) return
        }

        SensorManager.getRotationMatrixFromVector(rotationMatrix, values)
        SensorManager.getOrientation(rotationMatrix, orientationAngles)
        val azimuth = orientationAngles[0]
        val pitch = orientationAngles[1]
        if (!azimuth.isFinite() || !pitch.isFinite()) return

        if (!hasBaseline) {
            hasBaseline = true
            baselineAzimuth = azimuth
            baselinePitch = pitch
            return
        }

        var dAzimuth = azimuth - baselineAzimuth
        while (dAzimuth > piF) dAzimuth -= twoPiF
        while (dAzimuth < -piF) dAzimuth += twoPiF
        val dPitch = pitch - baselinePitch
        if (!dAzimuth.isFinite() || !dPitch.isFinite()) return



        targetX = (-dAzimuth / maxAngle).coerceIn(-1f, 1f)
        targetY = (dPitch / maxAngle).coerceIn(-1f, 1f)
    }


    fun update(dtSeconds: Double): Pair<Double, Double> {
        val tx = targetX
        val ty = targetY
        if (dtSeconds > 0.0 && tx.isFinite() && ty.isFinite()) {
            val k = (1.0 - exp(-dtSeconds * 8.0)).toFloat()
            smoothX += (tx - smoothX) * k
            smoothY += (ty - smoothY) * k
        }
        if (!smoothX.isFinite()) smoothX = 0f
        if (!smoothY.isFinite()) smoothY = 0f

        val s = sensitivity
        val ox = (smoothX * s).toDouble().coerceIn(-2.0, 2.0)
        val oy = (smoothY * s).toDouble().coerceIn(-2.0, 2.0)
        return ox to oy
    }
}