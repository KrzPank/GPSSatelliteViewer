package com.example.gnssmap.data.viewmodel

import android.app.Application
import android.icu.text.SimpleDateFormat
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import com.example.gnssmap.data.LOCATION_TIMEOUT_PERIOD
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

class LocationViewModel(application: Application) : AndroidViewModel(application) {
    private val locationManager = application.getSystemService(Application.LOCATION_SERVICE) as LocationManager
    private val handler = Handler(Looper.getMainLooper())
    private val locationExecutor = Executors.newSingleThreadExecutor()
    private val listenerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    data class ListenerData(
        val time: String = "",
        val latitude: Double = 0.0,
        val longitude: Double = 0.0,
        val altitude: Double = 0.0,
        val accuracy: Float = 0f,
        val speed: Float = 0f,
        val bearing: Float = 0f,
        val verticalAccuracy: Float? = null,
        val speedAccuracy: Float? = null,
        val bearingAccuracy: Float? = null,
        val provider: String = "",
        val latHemisphere: Char = 0.toChar(),
        val longHemisphere: Char = 0.toChar(),
        val elapsedRealtimeNanos: Long = 0L,
    )

    private val _locationAndroidApi = MutableStateFlow<ListenerData>(ListenerData())
    val locationAndroidApi: StateFlow<ListenerData> = _locationAndroidApi

    private val _hasLocationAndroidApi = MutableStateFlow<Boolean>(false)
    val hasLocationAndroidApi: StateFlow<Boolean> = _hasLocationAndroidApi

    private val noAndroidApiLocationTimeout = Runnable {
        _hasLocationAndroidApi.value = false
    }

    private val _isLocationEnabled = MutableStateFlow<Boolean>(false)
    val isLocationEnabled: StateFlow<Boolean> = _isLocationEnabled

    fun checkLocationEnabled() {
         _isLocationEnabled.value = locationManager.isLocationEnabled
    }

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            handler.removeCallbacks(noAndroidApiLocationTimeout)
            handler.postDelayed(noAndroidApiLocationTimeout, LOCATION_TIMEOUT_PERIOD)
            listenerScope.launch {
                _hasLocationAndroidApi.value = true

                val listenerData = ListenerData(
                    time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(location.time)),
                    latitude = location.latitude,
                    longitude = location.longitude,
                    altitude = location.altitude,
                    accuracy = location.accuracy,
                    speed = location.speed,
                    bearing = location.bearing,
                    verticalAccuracy = location.verticalAccuracyMeters,
                    speedAccuracy = location.speedAccuracyMetersPerSecond,
                    bearingAccuracy = location.bearingAccuracyDegrees,
                    provider = location.provider.toString(),
                    latHemisphere = if (location.latitude >= 0) 'N' else 'S',
                    longHemisphere = if (location.longitude >= 0) 'E' else 'W',
                    elapsedRealtimeNanos = location.elapsedRealtimeNanos
                )

                withContext(Dispatchers.Main) {
                    _locationAndroidApi.value = listenerData
                }
            }
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    fun startLocationListenerInfo() {
        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000L, // 1000 ms = 1sec
                1.0f,
                locationExecutor,
                locationListener
            )
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    override fun onCleared() {
        super.onCleared()
        locationManager.removeUpdates(locationListener)
        listenerScope.cancel()
        handler.removeCallbacks(noAndroidApiLocationTimeout)
        locationExecutor.shutdown()
    }
}
