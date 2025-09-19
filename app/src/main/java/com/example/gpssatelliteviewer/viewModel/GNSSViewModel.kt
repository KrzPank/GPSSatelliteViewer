package com.example.gpssatelliteviewer.viewModel

import android.app.Application
import android.location.GnssStatus
import android.location.LocationManager
import androidx.lifecycle.AndroidViewModel
import com.example.gpssatelliteviewer.data.GNSSStatusData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import android.os.Build
import androidx.annotation.RequiresApi
import java.util.concurrent.Executors

@RequiresApi(Build.VERSION_CODES.R)
class GNSSViewModel(application: Application) : AndroidViewModel(application) {
    private val locationManager = application.getSystemService(Application.LOCATION_SERVICE) as LocationManager

    private val _satelliteList = MutableStateFlow<List<GNSSStatusData>>(listOf())
    val satelliteList: StateFlow<List<GNSSStatusData>> = _satelliteList

    private val gnssCallback = object : GnssStatus.Callback() {
        override fun onSatelliteStatusChanged(status: GnssStatus) {

            val list = mutableListOf<GNSSStatusData>()
            for (i in 0 until status.satelliteCount) {
                val constellation = when (status.getConstellationType(i)) {
                    GnssStatus.CONSTELLATION_GPS -> "GPS"
                    GnssStatus.CONSTELLATION_GLONASS -> "GLONASS"
                    GnssStatus.CONSTELLATION_BEIDOU -> "BeiDou"
                    GnssStatus.CONSTELLATION_GALILEO -> "Galileo"
                    GnssStatus.CONSTELLATION_QZSS -> "QZSS"
                    GnssStatus.CONSTELLATION_IRNSS -> "IRNSS"
                    GnssStatus.CONSTELLATION_SBAS -> "SBAS"
                    GnssStatus.CONSTELLATION_UNKNOWN -> "Unknown"
                    else -> "Other"
                }

                val svid = status.getSvid(i)
                val cn0 = status.getCn0DbHz(i)
                val used = status.usedInFix(i)
                val azim = status.getAzimuthDegrees(i)
                val ele = status.getElevationDegrees(i)

                list.add(
                    GNSSStatusData(
                        constellation = constellation,
                        prn = svid,
                        snr = cn0,
                        usedInFix = used,
                        azimuth = azim,
                        elevation = ele
                    )
                )
            }
            _satelliteList.value = list
        }
    }

    fun startLocationInfo() {
        try {
            val executor = Executors.newSingleThreadExecutor()
            locationManager.registerGnssStatusCallback(executor, gnssCallback)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    override fun onCleared() {
        super.onCleared()
        locationManager.unregisterGnssStatusCallback(gnssCallback)
    }
}
