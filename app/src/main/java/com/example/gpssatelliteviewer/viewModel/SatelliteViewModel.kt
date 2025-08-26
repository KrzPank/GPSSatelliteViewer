package com.example.gpssatelliteviewer.viewModel

import android.app.Application
import android.content.Context
import android.location.GnssStatus
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.location.OnNmeaMessageListener
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.lifecycle.AndroidViewModel
import com.example.gpssatelliteviewer.data.GNSSData
import com.example.gpssatelliteviewer.data.NMEAData
import com.example.gpssatelliteviewer.utility.parseGGA
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.Executors

class SatelliteViewModel(application: Application) : AndroidViewModel(application) {

    private val _satelliteList = MutableStateFlow<List<GNSSData>>(listOf())
    val satelliteList: StateFlow<List<GNSSData>> = _satelliteList

    //private val _location = MutableStateFlow<String?>("Oczekiwanie na lokalizacje...")
    //val location: StateFlow<String?> = _location

    private val _location = MutableStateFlow<NMEAData?>(null)
    val location: StateFlow<NMEAData?> = _location

    private val locationManager = application.getSystemService(Application.LOCATION_SERVICE) as LocationManager

    private var lastGnssUpdateTime = 0L
    private var lastNmeaUpdateTime = 0L
    private val updateInterval = 5000L // 5s

    private val gnssCallback = object : GnssStatus.Callback() {
        override fun onSatelliteStatusChanged(status: GnssStatus) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastGnssUpdateTime < updateInterval) return
            lastGnssUpdateTime = currentTime

            val list = mutableListOf<GNSSData>()
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

                list.add(
                    GNSSData(
                        constellation = constellation,
                        id = svid,
                        snr = cn0,
                        usedInFix = used
                    )
                )
            }
            _satelliteList.value = list
        }
    }

    private val nmeaListener = OnNmeaMessageListener { message, _ ->
        if (message.startsWith("\$GPGGA") || message.startsWith("\$GNGGA")) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastNmeaUpdateTime < updateInterval) return@OnNmeaMessageListener
            lastNmeaUpdateTime = currentTime

            val data = parseGGA(message)
            data?.let {
                _location.value = it // now stores NMEAData instead of String
            }
        }
    }

/*
    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            _location.value =
                "lat=${location.latitude}, " +
                        "lon=${location.longitude}, " +
                        "alt=${location.altitude}, " +
                        "accuracy=${location.accuracy}"
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }
*/

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    // ✅ Android 11+ → use Executor-based APIs
                    val executor = Executors.newSingleThreadExecutor()

                    locationManager.registerGnssStatusCallback(
                        executor,
                        gnssCallback
                    )

                    locationManager.addNmeaListener(
                        executor,
                        nmeaListener
                    )

                } else {
                    // ✅ Android 7–10 → use Handler-based APIs
                    val handler = Handler(Looper.getMainLooper())

                    locationManager.registerGnssStatusCallback(
                        gnssCallback,
                        handler
                    )

                    locationManager.addNmeaListener(
                        nmeaListener,
                        handler
                    )
                }

            } catch (e: SecurityException) {
                _satelliteList.value = listOf(
                    GNSSData(
                        constellation = "Error",
                        id = 0,
                        snr = 0.0f,
                        usedInFix = false
                    )
                )
            }
        } else {
            _satelliteList.value = listOf(
                GNSSData(
                    constellation = "Error",
                    id = 0,
                    snr = 0.0f,
                    usedInFix = false
                )
            )
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
