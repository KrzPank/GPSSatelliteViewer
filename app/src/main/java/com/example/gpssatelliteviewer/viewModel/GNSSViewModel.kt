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
import com.example.gpssatelliteviewer.data.NMEALocationData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import android.icu.util.TimeZone
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi
import com.example.gpssatelliteviewer.data.ListenerData
import com.example.gpssatelliteviewer.data.parsers.NMEAParser
import java.util.Date
import java.util.concurrent.Executors
import kotlin.Int

import com.example.gpssatelliteviewer.data.GGA
import com.example.gpssatelliteviewer.data.GSA
import com.example.gpssatelliteviewer.data.GSV
import com.example.gpssatelliteviewer.data.RMC
import com.example.gpssatelliteviewer.data.VTG

@RequiresApi(Build.VERSION_CODES.R)
class GNSSViewModel(application: Application) : AndroidViewModel(application) {

    private val locationManager = application.getSystemService(Application.LOCATION_SERVICE) as LocationManager
    private val updateInterval = 2000L // in milis
    private val timeoutPeriod: Long = 15 * 1000 // 15 sec


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

    /* Satellite3DViewModel
    private val _navMessages = MutableStateFlow<GnssNavigationMessage?>(null)
    val navMessages: StateFlow<GnssNavigationMessage?> = _navMessages

    private val _hasGNSSNavigationMessage = MutableStateFlow<String>("No capabilities")
    val hasGNSSNavigationMessage: StateFlow<String> = _hasGNSSNavigationMessage

    private val navMessageCallback = object : GnssNavigationMessage.Callback() {
        override fun onGnssNavigationMessageReceived(event: GnssNavigationMessage) {
            _navMessages.value = event
        }

        override fun onStatusChanged(status: Int) {
            super.onStatusChanged(status)
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
     */


    private val _locationNMEA = MutableStateFlow<NMEALocationData>(NMEALocationData())
    val locationNMEA: StateFlow<NMEALocationData> = _locationNMEA

    private val _hasLocationNMEA = MutableStateFlow<Boolean>(false)
    val hasLocationNMEA: StateFlow<Boolean> = _hasLocationNMEA

    private val _nmeaMessageMap = MutableStateFlow<Map<String, String>>(mapOf())
    val nmeaMessageMap: StateFlow<Map<String, String>> = _nmeaMessageMap

    private val _parsedGGA = MutableStateFlow<GGA?>(null)
    val parsedGGA: StateFlow<GGA?> = _parsedGGA

    private val _parsedRMC = MutableStateFlow<RMC?>(null)
    val parsedRMC: StateFlow<RMC?> = _parsedRMC

    private val _parsedGSA = MutableStateFlow<GSA?>(null)
    val parsedGSA: StateFlow<GSA?> = _parsedGSA

    private val _parsedVTG = MutableStateFlow<VTG?>(null)
    val parsedVTG: StateFlow<VTG?> = _parsedVTG

    private val _parsedGSV = MutableStateFlow<Map<String, GSV>>(emptyMap())
    val parsedGSV: StateFlow<Map<String, GSV>> = _parsedGSV

    private var tmpGGA: GGA? = null
    private var tmpRMC: RMC? = null

    private val handler = Handler(Looper.getMainLooper())
    private val noNMEAMessageTimeout  = Runnable {
        _hasLocationNMEA.value = false
    }

    private val nmeaListener = OnNmeaMessageListener { message, _ ->

        _hasLocationNMEA.value = true
        handler.removeCallbacks(noNMEAMessageTimeout)
        handler.postDelayed(noNMEAMessageTimeout, timeoutPeriod)

        handleNMEAMessage(message)
    }


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
        //startGNSSNavigation()
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

        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun handleNMEAMessage(message: String) {
        val messageType = NMEAParser.getMessageType(message)
        val updatedMap = _nmeaMessageMap.value.toMutableMap()
        updatedMap[messageType] = message

        _nmeaMessageMap.value = updatedMap

        when {
            message.startsWith("\$GPGGA") || message.startsWith("\$GNGGA") -> {
                tmpGGA = NMEAParser.parseGGA(message)
                _parsedGGA.value = tmpGGA
            }
            message.startsWith("\$GPRMC") || message.startsWith("\$GNRMC") -> {
                tmpRMC = NMEAParser.parseRMC(message)
                _parsedRMC.value = tmpRMC
            }
            message.startsWith("\$GPGSA") || message.startsWith("\$GNGSA") -> {
                _parsedGSA.value = NMEAParser.parseGSA(message)
            }
            message.startsWith("\$GPVTG") || message.startsWith("\$GNVTG") -> {
                _parsedVTG.value = NMEAParser.parseVTG(message)
            }
            message.contains("GSV") -> {
                val gsv = NMEAParser.parseGSV(message)
                if (gsv != null) {
                    _parsedGSV.value = _parsedGSV.value + (gsv.talker to gsv)
                }
            }
        }


        val combined = NMEALocationData(
            time = tmpRMC?.time ?: tmpGGA?.time.orEmpty(),
            date = tmpRMC?.date ?: tmpGGA?.time.orEmpty(),
            latitude = tmpGGA?.latitude?.takeIf { it != 0.0 } ?: (tmpRMC?.latitude ?: 0.0),
            latHemisphere = (tmpGGA?.latDirection ?: tmpRMC?.latDirection)!!,
            longitude = tmpGGA?.longitude?.takeIf { it != 0.0 } ?: (tmpRMC?.longitude ?: 0.0),
            lonHemisphere = (tmpGGA?.lonDirection ?: tmpRMC?.lonDirection)!!,
            fixQuality = tmpGGA?.fixQuality ?: 0,
            fixType = _parsedGSA.value?.fixType ?: 0,
            numSatellites = tmpGGA?.numSatellites ?: 0,
            hdop = tmpGGA?.horizontalDilution ?: 0.0,
            altitude = tmpGGA?.altitude ?: 0.0,
            geoidHeight = tmpGGA?.geoidSeparation ?: 0.0,
            mslAltitude = tmpGGA?.let { gga ->
                gga.altitude + (gga.geoidSeparation ?: 0.0)
            } ?: 0.0,
            speedKnots = tmpRMC?.speedOverGround ?: 0.0,
            course = tmpRMC?.courseOverGround ?: 0.0,
            magneticVariation = tmpRMC?.magneticVariation ?: 0.0
        )
        _locationNMEA.value = combined
    }

    override fun onCleared() {
        super.onCleared()
        locationManager.unregisterGnssStatusCallback(gnssCallback)
        locationManager.removeNmeaListener(nmeaListener)
        locationManager.removeUpdates(locationListener)
        //locationManager.unregisterGnssNavigationMessageCallback(navMessageCallback)
    }
}
