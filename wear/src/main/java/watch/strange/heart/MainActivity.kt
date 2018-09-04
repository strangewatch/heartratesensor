package watch.strange.heart

import android.Manifest
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.wearable.activity.WearableActivity
import kotlinx.android.synthetic.main.activity_main.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : WearableActivity(), SensorEventListener {

    companion object {
        const val RC_SENSOR_PERMISSION = 2018
        val shortDateFormat = SimpleDateFormat("HH:mm:ss")
    }

    private lateinit var sensorManager: SensorManager
    private lateinit var heartSensor: Sensor

    private val readingIntervals = mutableListOf<Long>() // intervals in ms, not timestamps or long time

    private var lastReadingMs = 0L

    private fun now() = System.currentTimeMillis()

    // region: lifecycle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setAmbientEnabled()
        if (!hasPermission()) {
            requestPermission()
        } else {
            startMonitoring()
        }
    }

    // endregion

    // region: permissions

    private fun hasPermission(): Boolean {
        if (checkSelfPermission(Manifest.permission.BODY_SENSORS) != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.BODY_SENSORS) != PackageManager.PERMISSION_GRANTED) {
            return false
        }
        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>?, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RC_SENSOR_PERMISSION) {
            if (grantResults.isNotEmpty()) {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startMonitoring()
                } else {
                    finish()
                }
            }
        } else {
            finish()
        }
    }

    private fun requestPermission() {
        requestPermissions(arrayOf(Manifest.permission.BODY_SENSORS), RC_SENSOR_PERMISSION)
    }

    // endregion

    // region: monitoring

    private fun startMonitoring() {
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        heartSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
        sensorManager.registerListener(this, heartSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        SWLog.e("Accuracy changed: " + p1.toString())
    }

    override fun onSensorChanged(event: SensorEvent) {
        statusTv?.apply {
            setTextColor(ContextCompat.getColor(context, R.color.sw_active))
            text = "active"
        }
        if (event.sensor.type == Sensor.TYPE_HEART_RATE) {
            if (lastReadingMs > 0) {
                readingIntervals.add(now() - lastReadingMs)
            }

            val thisReadingMs = now()

            val thisReading = event.values[0].toInt()
            lastReadingTv.text = thisReading.toString()

            val avgInterval = calculateAverageInterval()

            lastReadingTimeTv.text = getString(R.string.last_reading_format, shortDateFormat.format(Date(now())))
            averageIntervalTv.text = getString(R.string.avg_interval_format, avgInterval.toString())

            lastReadingMs = thisReadingMs

            SWLog.e(thisReading.toString())
        }
    }

    private fun calculateAverageInterval(): Long {
        if (readingIntervals.isEmpty()) {
            return 0
        }
        var total = 0L
        readingIntervals.forEach { time ->
            total += time
        }
        return total / readingIntervals.size
    }

    // endregion
}
