package com.example.gpssatelliteviewer.data.viewmodel

import android.app.Application
import android.location.LocationManager
import android.location.OnNmeaMessageListener
import androidx.lifecycle.AndroidViewModel
import com.example.gpssatelliteviewer.data.NMEALocationData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.gpssatelliteviewer.data.parser.NMEAParser
import java.util.concurrent.Executors
import kotlinx.coroutines.*

import com.example.gpssatelliteviewer.data.NMEAMessage

@RequiresApi(Build.VERSION_CODES.R)
class NMEAViewModel(application: Application) : AndroidViewModel(application) {
    private val locationManager = application.getSystemService(Application.LOCATION_SERVICE) as LocationManager
    private val timeoutPeriod: Long = 15 * 1000 // 15 sec
    
    // Background parsing scope with IO dispatcher
    private val parsingScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _locationNMEA = MutableStateFlow<NMEALocationData>(NMEALocationData())
    val locationNMEA: StateFlow<NMEALocationData> = _locationNMEA

    private val _hasLocationNMEA = MutableStateFlow<Boolean>(false)
    val hasLocationNMEA: StateFlow<Boolean> = _hasLocationNMEA

    // Raw NMEA message strings by type
    private val _nmeaMessageMap = MutableStateFlow<Map<String, String>>(emptyMap())
    val nmeaMessageMap: StateFlow<Map<String, String>> = _nmeaMessageMap

    // Unified StateFlow for all parsed messages by type
    private val _latestMessages = MutableStateFlow<Map<String, NMEAMessage>>(emptyMap())
    val latestMessages: StateFlow<Map<String, NMEAMessage>> = _latestMessages
    
    // Message statistics - count of each message type
    private val _messageStatistics = MutableStateFlow<Map<String, Int>>(emptyMap())
    val messageStatistics: StateFlow<Map<String, Int>> = _messageStatistics

    private val handler = Handler(Looper.getMainLooper())
    private val noNMEAMessageTimeout = Runnable {
        _hasLocationNMEA.value = false
    }

    private val nmeaListener = OnNmeaMessageListener { message, _ ->
        _hasLocationNMEA.value = true
        handler.removeCallbacks(noNMEAMessageTimeout)
        handler.postDelayed(noNMEAMessageTimeout, timeoutPeriod)
        handleNMEAMessage(message)
    }

    private fun handleNMEAMessage(message: String) {
        // Quick validation before launching expensive parsing
        if (message.isBlank() || !message.startsWith("$")) return
        
        // Parse in background thread to avoid blocking UI
        parsingScope.launch {
            try {
                val messageType = NMEAParser.getMessageType(message)
                val parsedMessage = NMEAParser.parseMessage(message)

                // Update UI state on main thread
                withContext(Dispatchers.Main) {
                    // Determine consistent key for both raw and parsed messages
                    val key = parsedMessage?.let { parsed ->
                        when (parsed) {
                            is NMEAMessage.GSV -> parsed.talker  // Use talker for GSV messages
                            else -> parsed.messageType
                        }
                    } ?: messageType
                    
                    // Update message statistics
                    val currentStats = _messageStatistics.value.toMutableMap()
                    currentStats[key] = (currentStats[key] ?: 0) + 1
                    _messageStatistics.value = currentStats
                    
                    // Update raw message map with consistent key
                    _nmeaMessageMap.value = _nmeaMessageMap.value + (key to message)
                    
                    // Update parsed messages if parsing was successful
                    parsedMessage?.let { parsed ->
                        _latestMessages.value = _latestMessages.value + (key to parsed)
                        updateLocationData()
                    }
                }
            } catch (e: Exception) {
                Log.e("NMEAViewModel", "Failed to parse NMEA message: ${e.message}")
            }
        }
    }
    
    /**
     * Update combined location data from latest parsed messages
     */
    private fun updateLocationData() {
        val messages = _latestMessages.value
        val gga = messages["GGA"] as? NMEAMessage.GGA
        val rmc = messages["RMC"] as? NMEAMessage.RMC
        val gsa = messages["GSA"] as? NMEAMessage.GSA
        
        val combined = NMEALocationData(
            time = rmc?.time ?: gga?.time ?: "",
            date = rmc?.date ?: "",
            latitude = gga?.latitude?.takeIf { it != 0.0 } ?: (rmc?.latitude ?: 0.0),
            latHemisphere = gga?.latDirection ?: rmc?.latDirection ?: 'N',
            longitude = gga?.longitude?.takeIf { it != 0.0 } ?: (rmc?.longitude ?: 0.0),
            lonHemisphere = gga?.lonDirection ?: rmc?.lonDirection ?: 'E',
            fixQuality = gga?.fixQuality ?: 0,
            fixType = gsa?.fixType ?: 0,
            numSatellites = gga?.satelliteCount ?: 0,
            hdop = gga?.horizontalDilution ?: 0.0,
            altitude = gga?.altitude ?: 0.0,
            geoidHeight = gga?.geoidSeparation ?: 0.0,
            mslAltitude = gga?.let { it.altitude + (it.geoidSeparation ?: 0.0) } ?: 0.0,
            speedKnots = rmc?.speedOverGround ?: 0.0,
            course = rmc?.courseOverGround ?: 0.0,
            magneticVariation = rmc?.magneticVariation ?: 0.0
        )
        _locationNMEA.value = combined
    }

    fun startNMEAInfo() {
        try {
            val executor = Executors.newSingleThreadExecutor()
            locationManager.addNmeaListener(executor, nmeaListener)

        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    override fun onCleared() {
        super.onCleared()
        locationManager.removeNmeaListener(nmeaListener)
        // Cancel all background parsing operations
        parsingScope.cancel()
    }
}
