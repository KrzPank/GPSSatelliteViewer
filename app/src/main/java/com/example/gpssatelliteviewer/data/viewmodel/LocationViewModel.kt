package com.example.gpssatelliteviewer.data.viewmodel

import android.app.Application
import android.icu.text.SimpleDateFormat
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import com.example.gpssatelliteviewer.data.LOCATION_TIMEOUT_PERIOD
import com.example.gpssatelliteviewer.data.LOCATION_UPDATE_INTERVAL
import com.example.gpssatelliteviewer.data.ListenerData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Date
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.P)
class LocationViewModel(application: Application) : AndroidViewModel(application) {
    private val locationManager = application.getSystemService(Application.LOCATION_SERVICE) as LocationManager
    private val handler = Handler(Looper.getMainLooper())
    private val parsingScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

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
            parsingScope.launch {
                _hasLocationAndroidApi.value = true
                handler.removeCallbacks(noAndroidApiLocationTimeout)
                handler.postDelayed(noAndroidApiLocationTimeout, LOCATION_TIMEOUT_PERIOD)

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
                _locationAndroidApi.value = listenerData
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
                LOCATION_UPDATE_INTERVAL,
                0f,
                locationListener
            )
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    override fun onCleared() {
        super.onCleared()
        locationManager.removeUpdates(locationListener)
        parsingScope.cancel()
    }
}
