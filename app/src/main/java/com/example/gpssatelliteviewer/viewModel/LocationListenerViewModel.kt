package com.example.gpssatelliteviewer.viewModel

import android.app.Application
import android.icu.text.SimpleDateFormat
import android.icu.util.TimeZone
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import com.example.gpssatelliteviewer.data.ListenerData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Date
import java.util.Locale

class LocationListenerViewModel(application: Application) : AndroidViewModel(application) {
    private val locationManager = application.getSystemService(Application.LOCATION_SERVICE) as LocationManager
    private val updateInterval = 1000L // in milis
    private val timeoutPeriod: Long = 15 * 1000 // 15 sec
    private val handler = Handler(Looper.getMainLooper())

    private val _locationAndroidApi = MutableStateFlow<ListenerData>(ListenerData())
    val locationAndroidApi: StateFlow<ListenerData> = _locationAndroidApi

    private val _hasLocationAndroidApi = MutableStateFlow<Boolean>(false)
    val hasLocationAndroidApi: StateFlow<Boolean> = _hasLocationAndroidApi

    private val noAndroidApiLocationTimeout = Runnable {
        _hasLocationAndroidApi.value = false
    }


    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {

            _hasLocationAndroidApi.value = true
            handler.removeCallbacks(noAndroidApiLocationTimeout)
            handler.postDelayed(noAndroidApiLocationTimeout, timeoutPeriod)

            //if (_hasLocationNMEA.value) return

            val sdf = SimpleDateFormat("HHmmss", Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            val formattedTime = sdf.format(Date(System.currentTimeMillis()))

            val listenerData = ListenerData(
                time = formattedTime,
                latitude = location.latitude,
                longitude = location.longitude,
                altitude = location.altitude,
                latHemisphere = if (location.latitude >= 0) "N" else "S",
                longHemisphere = if (location.longitude >= 0) "E" else "W"
            )
            _locationAndroidApi.value = listenerData
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    fun startLocationListenerInfo() {
        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                updateInterval,
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
    }
}