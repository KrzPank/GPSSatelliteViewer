package com.example.gpssatelliteviewer.viewModel

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
import androidx.annotation.RequiresApi
import com.example.gpssatelliteviewer.data.parsers.NMEAParser
import java.util.concurrent.Executors
import kotlinx.coroutines.*
import androidx.lifecycle.viewModelScope

import com.example.gpssatelliteviewer.data.GGA
import com.example.gpssatelliteviewer.data.GSA
import com.example.gpssatelliteviewer.data.GSV
import com.example.gpssatelliteviewer.data.RMC
import com.example.gpssatelliteviewer.data.VTG

// Data class to hold parsed NMEA data for batch updates
private data class ParsedNMEAData(
    val gga: GGA? = null,
    val rmc: RMC? = null,
    val gsa: GSA? = null,
    val vtg: VTG? = null,
    val gsv: GSV? = null
)

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

    private fun handleNMEAMessage(message: String) {
        // Quick validation before launching expensive parsing
        if (message.isBlank() || !message.startsWith("$")) return
        
        // Parse in background thread to avoid blocking UI
        parsingScope.launch {
            try {
                val messageType = NMEAParser.getMessageTypeOptimized(message)
                
                // Update message map on main thread (quick operation)
                withContext(Dispatchers.Main) {
                    val updatedMap = _nmeaMessageMap.value.toMutableMap()
                    updatedMap[messageType] = message
                    _nmeaMessageMap.value = updatedMap
                }
                
                // Heavy parsing operations in background
                val parsedData = when {
                    messageType.endsWith("GGA") -> {
                        ParsedNMEAData(gga = NMEAParser.parseGGA(message))
                    }
                    messageType.endsWith("RMC") -> {
                        ParsedNMEAData(rmc = NMEAParser.parseRMC(message))
                    }
                    messageType.endsWith("GSA") -> {
                        ParsedNMEAData(gsa = NMEAParser.parseGSA(message))
                    }
                    messageType.endsWith("VTG") -> {
                        ParsedNMEAData(vtg = NMEAParser.parseVTG(message))
                    }
                    messageType.contains("GSV") -> {
                        val gsv = NMEAParser.parseGSV(message)
                        ParsedNMEAData(gsv = gsv)
                    }
                    else -> null
                }
                
                // Update UI state on main thread
                parsedData?.let { data ->
                    withContext(Dispatchers.Main) {
                        updateParsedDataStates(data)
                        combineData()
                    }
                }
            } catch (e: Exception) {
                // Log error but don't crash the app
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Update parsed data states in batch to reduce StateFlow emissions
     */
    private fun updateParsedDataStates(data: ParsedNMEAData) {
        data.gga?.let { 
            tmpGGA = it
            _parsedGGA.value = it 
        }
        data.rmc?.let { 
            tmpRMC = it
            _parsedRMC.value = it 
        }
        data.gsa?.let { _parsedGSA.value = it }
        data.vtg?.let { _parsedVTG.value = it }
        data.gsv?.let { gsv ->
            _parsedGSV.value = _parsedGSV.value + (gsv.talker to gsv)
        }
    }

    private fun combineData(){
        val combined = NMEALocationData(
            time = tmpRMC?.time ?: tmpGGA?.time ?: "",
            date = tmpRMC?.date ?: "",
            latitude = tmpGGA?.latitude?.takeIf { it != 0.0 } ?: (tmpRMC?.latitude ?: 0.0),
            latHemisphere = tmpGGA?.latDirection ?: tmpRMC?.latDirection ?: 'N',
            longitude = tmpGGA?.longitude?.takeIf { it != 0.0 } ?: (tmpRMC?.longitude ?: 0.0),
            lonHemisphere = tmpGGA?.lonDirection ?: tmpRMC?.lonDirection ?: 'E',
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