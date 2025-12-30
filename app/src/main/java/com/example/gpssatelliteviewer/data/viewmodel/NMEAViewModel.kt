package com.example.gpssatelliteviewer.data.viewmodel

import android.app.Application
import android.location.LocationManager
import android.location.OnNmeaMessageListener
import androidx.lifecycle.AndroidViewModel
import com.example.gpssatelliteviewer.data.NMEALocationData
import com.example.gpssatelliteviewer.data.NMEAMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.gpssatelliteviewer.data.LOCATION_TIMEOUT_PERIOD
import com.example.gpssatelliteviewer.data.parser.NMEAParser
import java.util.concurrent.Executors
import kotlinx.coroutines.*

class NMEAViewModel(application: Application) : AndroidViewModel(application) {
    private val locationManager = application.getSystemService(Application.LOCATION_SERVICE) as LocationManager
    private val executor = Executors.newSingleThreadExecutor()
    
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
        handler.postDelayed(noNMEAMessageTimeout, LOCATION_TIMEOUT_PERIOD)
        handleNMEAMessage(message)
    }

    private fun handleNMEAMessage(message: String) {
        if (message.isBlank() || !message.startsWith("$")) return
        
        // Parse in background thread to avoid blocking UI
        parsingScope.launch {
            try {
                val messageType = NMEAParser.getMessageType(message)
                val parsedMessage = NMEAParser.parseMessage(message)

                // Determine consistent key for both raw and parsed messages
                val key = parsedMessage?.let { parsed ->
                    when (parsed) {
                        is NMEAMessage.GSV -> parsed.talker  // talker for GSV messages
                        else -> parsed.messageType
                    }
                } ?: messageType

                // Update message statistics
                val currentStats = _messageStatistics.value.toMutableMap()
                currentStats[key] = (currentStats[key] ?: 0) + 1

                // Update UI state on main thread
                withContext(Dispatchers.Main) {
                    _messageStatistics.value = currentStats
                    _nmeaMessageMap.value = _nmeaMessageMap.value + (key to message)

                    parsedMessage?.let { parsed ->
                        _latestMessages.value = _latestMessages.value + (key to parsed)
                        _locationNMEA.value = NMEALocationData.combine(_latestMessages.value)
                    }
                }
            } catch (e: Exception) {
                Log.e("NMEAViewModel", "Failed to parse NMEA message: ${e.message}")
            }
        }
    }

    fun startNMEAInfo() {
        try {
            locationManager.addNmeaListener(executor, nmeaListener)

        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    override fun onCleared() {
        super.onCleared()
        locationManager.removeNmeaListener(nmeaListener)
        parsingScope.cancel()
        executor.shutdown()
    }
}
