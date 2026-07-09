package ru.kkalscan.data.steps

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import ru.kkalscan.data.storage.AndroidDeviceIdContext
import kotlin.coroutines.resume

class AndroidLocalStepCounter(
    private val context: Context,
) : ILocalStepCounter {

    private val sensorManager: SensorManager? =
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

    override suspend fun isSensorAvailable(): Boolean = withContext(Dispatchers.Default) {
        sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null
    }

    override suspend fun hasPermission(): Boolean = withContext(Dispatchers.Default) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) ==
            PackageManager.PERMISSION_GRANTED
    }

    override suspend fun readCumulativeSteps(): Long? = withContext(Dispatchers.Main) {
        val manager = sensorManager ?: return@withContext null
        val sensor = manager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) ?: return@withContext null
        withTimeoutOrNull(2_000) {
            suspendCancellableCoroutine { continuation ->
                val listener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent) {
                        manager.unregisterListener(this)
                        if (continuation.isActive) {
                            continuation.resume(event.values.firstOrNull()?.toLong())
                        }
                    }

                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
                }
                continuation.invokeOnCancellation { manager.unregisterListener(listener) }
                manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
            }
        }
    }
}

actual fun createLocalStepCounter(): ILocalStepCounter =
    AndroidLocalStepCounter(AndroidDeviceIdContext.appContext)
