package com.example.gpssatelliteviewer.viewModel

import android.app.Application
import android.icu.text.SimpleDateFormat
import android.location.GnssStatus
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.location.OnNmeaMessageListener
import android.os.Bundle
import android.util.Log
import java.util.Locale
import androidx.lifecycle.AndroidViewModel
import com.example.gpssatelliteviewer.data.GNSSData
import com.example.gpssatelliteviewer.data.NMEAData
import com.example.gpssatelliteviewer.utility.parseGGA
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import android.icu.util.TimeZone
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi
import com.example.gpssatelliteviewer.data.ListenerData
import com.example.gpssatelliteviewer.utility.parseGBS
import com.example.gpssatelliteviewer.utility.parseRMC
import java.util.Date
import java.util.concurrent.Executors
import kotlin.Int


@RequiresApi(Build.VERSION_CODES.R)
class SatelliteViewModel(application: Application) : AndroidViewModel(application) {

    private val _satelliteList = MutableStateFlow<List<GNSSData>>(listOf())
    val satelliteList: StateFlow<List<GNSSData>> = _satelliteList

    private val _locationNMEA = MutableStateFlow<NMEAData?>(null)
    val locationNMEA: StateFlow<NMEAData?> = _locationNMEA

    private var tmpGGA: NMEAData? = null
    private var tmpRMC: NMEAData? = null
    private var tmpGBS: List<Double>? = null

    private val _locationAndroidApi = MutableStateFlow<ListenerData?>(null)
    val locationAndroidApi: StateFlow<ListenerData?> = _locationAndroidApi

    private val _messagePack = MutableStateFlow<String?>(String())
    var messagePack: StateFlow<String?> = _messagePack

    private val locationManager = application.getSystemService(Application.LOCATION_SERVICE) as LocationManager

    private var lastGnssUpdateTime = 0L
    private val updateInterval = 5000L // in milis

    private var hasNmeaData = false
    private val nmeaTimeout: Long = 30 * 1000 // 30 sec in ms
    private val handler = Handler(Looper.getMainLooper())
    private val fallbackRunnable = Runnable {
        hasNmeaData = false
        _locationNMEA.value = null
    }

    private val gnssCallback = object : GnssStatus.Callback() {
        override fun onSatelliteStatusChanged(status: GnssStatus) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastGnssUpdateTime < updateInterval) return
            lastGnssUpdateTime = currentTime

            Log.d("GNSSCallback", "Raw message: $status")

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
        when {
            message.startsWith("\$GPGGA") || message.startsWith("\$GNGGA") -> {
                tmpGGA = parseGGA(message, tmpGGA)
            }
            message.startsWith("\$GPRMC") || message.startsWith("\$GNRMC") -> {
                tmpRMC = parseRMC(message, tmpRMC)
            }
            message.startsWith("\$GPGBS") || message.startsWith("\$GNGBS") -> {
                tmpGBS = parseGBS(message, _locationNMEA.value)?.gbsErrors
            }
        }

        val combined = NMEAData(
            time = tmpRMC?.time ?: tmpGGA?.time,
            date = tmpRMC?.date ?: tmpGGA?.date,
            latitude = tmpGGA?.latitude,
            longitude = tmpGGA?.longitude,
            fixQuality = tmpGGA?.fixQuality,
            numSatellites = tmpGGA?.numSatellites,
            hdop = tmpGGA?.hdop,
            altitude = tmpGGA?.altitude,
            geoidHeight = tmpGGA?.geoidHeight,
            speedKnots = tmpRMC?.speedKnots,
            course = tmpRMC?.course,
            magneticVariation = tmpRMC?.magneticVariation,
            gbsErrors = tmpGBS
        )

        _locationNMEA.value = combined
    }

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            if (hasNmeaData) return

            val list = mutableListOf<ListenerData>()

            val sdf = SimpleDateFormat("HHmmss", Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            val formattedTime = sdf.format(Date(System.currentTimeMillis()))

            list.add(
                ListenerData(
                    time = formattedTime,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    altitude = location.altitude,
                    latHemisphere = if (location.latitude >= 0) "N" else "S",
                    longHemisphere = if (location.longitude >= 0) "E" else "W"
                )
            )

            _locationAndroidApi.value = list.first() // Store the latest one
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    init {
        try {
            //  UNDO IF THREADING IS SOMEHOW NOT DOABLE         ***LAG IN UI***
            //locationManager.registerGnssStatusCallback(gnssCallback)
            //locationManager.addNmeaListener(nmeaListener)

            val executor = Executors.newSingleThreadExecutor()

            locationManager.registerGnssStatusCallback(
                executor,
                gnssCallback
            )

            locationManager.addNmeaListener(
                executor,
                nmeaListener
            )

            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                updateInterval,
                0f,
                locationListener
            )

            handler.postDelayed(fallbackRunnable, nmeaTimeout)

        } catch (_: SecurityException) {
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
        try {
            locationManager.unregisterGnssStatusCallback(gnssCallback)
        } catch (_: SecurityException) { }
    }
}
