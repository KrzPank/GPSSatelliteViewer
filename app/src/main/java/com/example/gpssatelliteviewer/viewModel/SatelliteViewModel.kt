package com.example.gpssatelliteviewer.viewModel

import android.app.Application
import android.location.GnssStatus
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SatelliteViewModel(application: Application) : AndroidViewModel(application) {

    private val _satelliteList = MutableStateFlow<List<String>>(listOf("Oczekiwanie na dane GNSS..."))
    val satelliteList: StateFlow<List<String>> = _satelliteList

    private val locationManager =
        application.getSystemService(Application.LOCATION_SERVICE) as LocationManager

    private val gnssCallback = object : GnssStatus.Callback() {
        override fun onSatelliteStatusChanged(status: GnssStatus) {
            val list = mutableListOf<String>()
            list.add("Liczba satelitów: ${status.satelliteCount}")

            for (i in 0 until status.satelliteCount) {
                list.add(
                    "Satelita $i: " +
                            "SNR=${status.getCn0DbHz(i)}, " +
                            "PRN=${status.getSvid(i)}, " +
                            "UsedInFix=${status.usedInFix(i)}"
                )
            }

            _satelliteList.value = list
        }
    }

    // Location listener do odświeżania pozycji co 5 sek.
    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            val current = _satelliteList.value.toMutableList()
            current.add(
                0,
                "Lokalizacja: lat=${location.latitude}, lon=${location.longitude}, alt=${location.altitude}"
            )
            _satelliteList.value = current
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                // Rejestracja callback GNSS
                locationManager.registerGnssStatusCallback(gnssCallback)

                // Rejestracja listenera lokalizacji (co 5 sek)
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    3000L, // co 3 sekund
                    0f,    // minimalny dystans
                    locationListener
                )
            } catch (e: SecurityException) {
                _satelliteList.value = listOf("Brak uprawnień do dostępu do lokalizacji!")
            }
        } else {
            _satelliteList.value = listOf("GnssStatus API wymaga Android N (7.0)+")
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                locationManager.unregisterGnssStatusCallback(gnssCallback)
            } catch (_: SecurityException) { }
        }
    }
}
