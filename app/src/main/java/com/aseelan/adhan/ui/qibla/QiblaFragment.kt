package com.aseelan.adhan.ui.qibla

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.aseelan.adhan.databinding.FragmentQiblaBinding
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class QiblaFragment : Fragment(), SensorEventListener {

    private var _binding: FragmentQiblaBinding? = null
    private val binding get() = _binding!!

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var magnetometer: Sensor? = null

    private val gravity = FloatArray(3)
    private val geomagnetic = FloatArray(3)
    private var currentDegree = 0f

    companion object {
        private const val ASEELAN_LAT = 25.45
        private const val ASEELAN_LON = 43.15
        private const val KAABA_LAT = 21.4225
        private const val KAABA_LON = 39.8262
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQiblaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sensorManager = requireActivity().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        magnetometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> System.arraycopy(event.values, 0, gravity, 0, gravity.size)
            Sensor.TYPE_MAGNETIC_FIELD -> System.arraycopy(event.values, 0, geomagnetic, 0, geomagnetic.size)
        }

        val rotationMatrix = FloatArray(9)
        val success = SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic)
        if (success) {
            val orientation = FloatArray(3)
            SensorManager.getOrientation(rotationMatrix, orientation)
            val azimuth = (Math.toDegrees(orientation[0].toDouble())).toFloat()
            val normalizedAzimuth = (azimuth + 360) % 360

            val qiblaBearing = calculateQiblaBearing(ASEELAN_LAT, ASEELAN_LON, KAABA_LAT, KAABA_LON)
            val needleRotation = (qiblaBearing - normalizedAzimuth + 360) % 360

            val compassRotation = -normalizedAzimuth
            binding.imgCompassDial.rotation = compassRotation
            binding.imgQiblaNeedle.rotation = needleRotation

            currentDegree = normalizedAzimuth
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun calculateQiblaBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val lat1Rad = Math.toRadians(lat1)
        val lat2Rad = Math.toRadians(lat2)
        val deltaLon = Math.toRadians(lon2 - lon1)

        val y = sin(deltaLon) * cos(lat2Rad)
        val x = cos(lat1Rad) * sin(lat2Rad) - sin(lat1Rad) * cos(lat2Rad) * cos(deltaLon)
        val rawBearing = Math.toDegrees(atan2(y, x))
        val bearing = (rawBearing + 360) % 360
        return bearing.toFloat()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
