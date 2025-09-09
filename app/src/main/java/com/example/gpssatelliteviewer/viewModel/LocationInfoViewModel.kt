package com.example.gpssatelliteviewer.viewModel

import android.app.Application
import android.icu.text.SimpleDateFormat
import android.location.GnssStatus
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.location.OnNmeaMessageListener
import android.os.Bundle
import java.util.Locale
import androidx.lifecycle.AndroidViewModel
import com.example.gpssatelliteviewer.data.GNSSStatusData
import com.example.gpssatelliteviewer.data.NMEAData
import com.example.gpssatelliteviewer.utility.parseGGA
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import android.icu.util.TimeZone
import android.location.GnssNavigationMessage
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
class LocationInfoViewModel(application: Application) : AndroidViewModel(application) {

    private val _satelliteList = MutableStateFlow<List<GNSSStatusData>>(listOf())
    val satelliteList: StateFlow<List<GNSSStatusData>> = _satelliteList

    // Satellite3DViewModel
    private val _navMessages = MutableStateFlow<GnssNavigationMessage?>(null)
    val navMessages: StateFlow<GnssNavigationMessage?> = _navMessages

    private val _hasGNSSNavigationMessage = MutableStateFlow<String>("No capabilities")
    val hasGNSSNavigationMessage: StateFlow<String> = _hasGNSSNavigationMessage
    // Satellite3DViewModel

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

    private var nmeaMessageReceived = false
    private var androidApiLocation = false
    private val updateInterval = 2000L // in milis
    private val timeoutPeriod: Long = 30 * 1000 // 30 sec

    private val handler = Handler(Looper.getMainLooper())
    private val noNMEAMessageTimeout  = Runnable {
        nmeaMessageReceived = false
        _locationNMEA.value = null
    }
    private val noAndroidApiLocation = Runnable {
        _locationNMEA.value = null
    }

    private val gnssCallback = object : GnssStatus.Callback() {
        override fun onSatelliteStatusChanged(status: GnssStatus) {
            //val currentTime = System.currentTimeMillis()
            //if (currentTime - lastGnssUpdateTime < updateInterval) return
            //lastGnssUpdateTime = currentTime

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

                list.add(
                    GNSSStatusData(
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

    private val naviMessageCallback = object : GnssNavigationMessage.Callback() {
        override fun onGnssNavigationMessageReceived(event: GnssNavigationMessage) {
            _navMessages.value = event
        }

        override fun onStatusChanged(status: Int) {
            super.onStatusChanged(status)
        }
    }

    private val nmeaListener = OnNmeaMessageListener { message, _ ->

        handler.removeCallbacks(noNMEAMessageTimeout)
        nmeaMessageReceived = true
        handler.postDelayed(noNMEAMessageTimeout, timeoutPeriod)

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
            mslAltitude = tmpGGA?.mslAltitude,
            speedKnots = tmpRMC?.speedKnots,
            course = tmpRMC?.course,
            magneticVariation = tmpRMC?.magneticVariation,
            gbsErrors = tmpGBS
        )

        _locationNMEA.value = combined
    }

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            handler.removeCallbacks(noAndroidApiLocation)
            handler.postDelayed(noAndroidApiLocation, timeoutPeriod)

            if (nmeaMessageReceived) return

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
        startLocationInfo()
        startGNSSNavigation()
    }

    fun startLocationInfo() {
        try {
            //  UNDO IF THREADING IS SOMEHOW NOT DOABLE         ***LAG IN UI***
            //locationManager.registerGnssStatusCallback(gnssCallback)
            //locationManager.addNmeaListener(nmeaListener)

            val executor = Executors.newSingleThreadExecutor()

            locationManager.registerGnssStatusCallback(executor, gnssCallback)
            //locationManager.registerGnssNavigationMessageCallback(executor, naviMessageCallback)
            locationManager.addNmeaListener(executor, nmeaListener)
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                updateInterval,
                0f,
                locationListener
            )

            //val gnssCapabilities = locationManager.gnssCapabilities
            //_hasGNSSNavigationMessage.value = gnssCapabilities.toString()

        } catch (_: SecurityException) {
            // *** Change this later ***
            _satelliteList.value = listOf(
                GNSSStatusData(
                    constellation = "Error",
                    id = 0,
                    snr = 0.0f,
                    usedInFix = false
                )
            )
        }
    }

    fun startGNSSNavigation(){
        try {
            val tempExecutor = Executors.newSingleThreadExecutor()
            locationManager.registerGnssNavigationMessageCallback(tempExecutor, naviMessageCallback)
            val gnssCapabilities = locationManager.gnssCapabilities
            _hasGNSSNavigationMessage.value = gnssCapabilities.toString()
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    override fun onCleared() {
        super.onCleared()
        locationManager.unregisterGnssStatusCallback(gnssCallback)
        locationManager.removeNmeaListener(nmeaListener)
        locationManager.removeUpdates(locationListener)
    }
}
