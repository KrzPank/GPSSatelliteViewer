package com.example.gpssatelliteviewer.viewModel

import android.Manifest
import android.R.bool
import android.app.Application
import android.location.GnssNavigationMessage
import android.location.LocationManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.Executors


@RequiresApi(Build.VERSION_CODES.R)
class Satellite3DViewModel (application: Application) : AndroidViewModel(application) {

    //private val _navMessages = MutableLiveData<GnssNavigationMessage>()
    //val navMessages: LiveData<GnssNavigationMessage> = _navMessages

    private val _navMessages = MutableStateFlow<GnssNavigationMessage?>(null)
    val navMessages: StateFlow<GnssNavigationMessage?> = _navMessages

    private val _hasGNSSNavigationMessage = MutableStateFlow<String>("No capabilities")
    val hasGNSSNavigationMessage: StateFlow<String> = _hasGNSSNavigationMessage

    private val locationManager: LocationManager =
        application.getSystemService(Application.LOCATION_SERVICE) as LocationManager


    private val navCallback = object : GnssNavigationMessage.Callback() {
        override fun onGnssNavigationMessageReceived(event: GnssNavigationMessage) {
            _navMessages.value = event
        }

        override fun onStatusChanged(status: Int) {
            super.onStatusChanged(status)
        }
    }

    fun startGNSSNavigation(){
        try {
            locationManager.registerGnssNavigationMessageCallback(navCallback)
            val gnssCapabilities = locationManager.gnssCapabilities
            _hasGNSSNavigationMessage.value = gnssCapabilities.toString()
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}