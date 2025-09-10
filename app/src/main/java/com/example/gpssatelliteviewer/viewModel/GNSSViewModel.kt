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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import android.icu.util.TimeZone
import android.location.GnssNavigationMessage
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi
import com.example.gpssatelliteviewer.data.ListenerData
import com.example.gpssatelliteviewer.data.parsers.NMEAParser
import java.util.Date
import java.util.concurrent.Executors
import kotlin.Int


@RequiresApi(Build.VERSION_CODES.R)
class GNSSViewModel(application: Application) : AndroidViewModel(application) {

    private val _satelliteList = MutableStateFlow<List<GNSSStatusData>>(listOf())
    val satelliteList: StateFlow<List<GNSSStatusData>> = _satelliteList

    // Satellite3DViewModel
    private val _navMessages = MutableStateFlow<GnssNavigationMessage?>(null)
    val navMessages: StateFlow<GnssNavigationMessage?> = _navMessages

    private val _hasGNSSNavigationMessage = MutableStateFlow<String>("No capabilities")
    val hasGNSSNavigationMessage: StateFlow<String> = _hasGNSSNavigationMessage
    // Satellite3DViewModel

    private val _locationNMEA = MutableStateFlow<NMEAData>(NMEAData())
    val locationNMEA: StateFlow<NMEAData> = _locationNMEA

    private val _hasLocationNMEA = MutableStateFlow<Boolean>(true)
    val hasLocationNMEA: StateFlow<Boolean> = _hasLocationNMEA

    private var tmpGGA: NMEAData = NMEAData()
    private var tmpRMC: NMEAData = NMEAData()
    private var tmpGBS: NMEAData = NMEAData()

    private val _locationAndroidApi = MutableStateFlow<ListenerData>(ListenerData())
    val locationAndroidApi: StateFlow<ListenerData> = _locationAndroidApi

    private val _hasLocationAndroidApi = MutableStateFlow<Boolean>(false)
    val hasLocationAndroidApi: StateFlow<Boolean> = _hasLocationAndroidApi

    private val _messagePack = MutableStateFlow<String?>(String())
    var messagePack: StateFlow<String?> = _messagePack

    private val locationManager = application.getSystemService(Application.LOCATION_SERVICE) as LocationManager

    private val updateInterval = 2000L // in milis
    private val timeoutPeriod: Long = 30 * 1000 // 30 sec

    private val handler = Handler(Looper.getMainLooper())
    private val noNMEAMessageTimeout  = Runnable {
        _hasLocationNMEA.value = false
    }
    private val noAndroidApiLocationTimeout = Runnable {
        _hasLocationAndroidApi.value = false
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
                val azim = status.getAzimuthDegrees(i)
                val ele = status.getElevationDegrees(i)

                list.add(
                    GNSSStatusData(
                        constellation = constellation,
                        id = svid,
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

    private val navMessageCallback = object : GnssNavigationMessage.Callback() {
        override fun onGnssNavigationMessageReceived(event: GnssNavigationMessage) {
            _navMessages.value = event
        }

        override fun onStatusChanged(status: Int) {
            super.onStatusChanged(status)
        }
    }

    private val nmeaListener = OnNmeaMessageListener { message, _ ->

        _hasLocationNMEA.value = true
        handler.removeCallbacks(noNMEAMessageTimeout)
        handler.postDelayed(noNMEAMessageTimeout, timeoutPeriod)

        when {
            message.startsWith("\$GPGGA") || message.startsWith("\$GNGGA") -> {
                tmpGGA = NMEAParser.parseGGA(message, tmpGGA)
            }
            message.startsWith("\$GPRMC") || message.startsWith("\$GNRMC") -> {
                tmpRMC = NMEAParser.parseRMC(message, tmpRMC)
            }
            message.startsWith("\$GPGBS") || message.startsWith("\$GNGBS") -> {
                tmpGBS = NMEAParser.parseGBS(message, _locationNMEA.value)
            }
        }

        val combined = NMEAData(
            time = if (tmpRMC.time.isNotEmpty()) tmpRMC.time else tmpGGA.time,
            date = if (tmpRMC.date.isNotEmpty()) tmpRMC.date else tmpGGA.date,
            latitude = if (tmpGGA.latitude != 0.0) tmpGGA.latitude else tmpRMC.latitude,
            longitude = if (tmpGGA.longitude != 0.0) tmpGGA.longitude else tmpRMC.longitude,
            fixQuality = tmpGGA.fixQuality,
            numSatellites = tmpGGA.numSatellites,
            hdop = tmpGGA.hdop,
            altitude = tmpGGA.altitude,
            geoidHeight = tmpGGA.geoidHeight,
            mslAltitude = tmpGGA.mslAltitude,
            speedKnots = tmpRMC.speedKnots,
            course = tmpRMC.course,
            magneticVariation = tmpRMC.magneticVariation,
            gbsErrors = tmpGBS.gbsErrors
        )

        _locationNMEA.value = combined
    }

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {

            _hasLocationAndroidApi.value = true
            handler.removeCallbacks(noAndroidApiLocationTimeout)
            handler.postDelayed(noAndroidApiLocationTimeout, timeoutPeriod)

            if (_hasLocationNMEA.value) return

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
            //locationManager.registerGnssNavigationMessageCallback(executor, navMessageCallback)
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
                    usedInFix = false,
                    azimuth = 0.0f,
                    elevation = 0.0f
                )
            )
        }
    }

    fun startGNSSNavigation(){
        try {
            val tempExecutor = Executors.newSingleThreadExecutor()
            locationManager.registerGnssNavigationMessageCallback(tempExecutor, navMessageCallback)
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
