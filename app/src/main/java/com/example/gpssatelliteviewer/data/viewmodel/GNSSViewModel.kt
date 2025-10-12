package com.example.gpssatelliteviewer.data.viewmodel

import android.app.Application
import android.location.GnssMeasurementsEvent
import android.location.GnssStatus
import android.location.LocationManager
import androidx.lifecycle.AndroidViewModel
import com.example.gpssatelliteviewer.data.GNSSStatusData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.gpssatelliteviewer.data.CHART_UPDATE_WINDOW
import com.example.gpssatelliteviewer.data.GNSSHardwareInfo
import com.example.gpssatelliteviewer.data.GNSSMeasurementData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

@RequiresApi(Build.VERSION_CODES.R)
class GNSSViewModel(application: Application) : AndroidViewModel(application) {
    private val locationManager = application.getSystemService(Application.LOCATION_SERVICE) as LocationManager

    private val parsingScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _satelliteList = MutableStateFlow<List<GNSSStatusData>>(listOf())
    val satelliteList: StateFlow<List<GNSSStatusData>> = _satelliteList

    private val _gnssMeasurements = MutableStateFlow<List<GNSSMeasurementData>>(listOf())
    val gnssMeasurements: StateFlow<List<GNSSMeasurementData>> = _gnssMeasurements


    private val _snrHistory = MutableStateFlow<Map<String, MutableList<Float>>>(emptyMap())
    val snrHistory: StateFlow<Map<String, List<Float>>> = _snrHistory

    private val _gnssHardwareInfo = MutableStateFlow(GNSSHardwareInfo())
    val gnssHardwareInfo: StateFlow<GNSSHardwareInfo> = _gnssHardwareInfo

    // Keep reference to last parsing job
    private var parseJob: Job? = null

    private val gnssCallback = object : GnssStatus.Callback() {
        override fun onSatelliteStatusChanged(status: GnssStatus) {
            // Cancel any ongoing parse
            parseJob?.cancel()

            // Start a new background parsing job
            parseJob = parsingScope.launch {
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

                    if (status.getSvid(i) == 13 && constellation == "GPS") Log.d("az/el_test", "${status.getAzimuthDegrees(i)}, ${status.getElevationDegrees(i)}")

                    list.add(
                        GNSSStatusData(
                            constellation = constellation,
                            prn = status.getSvid(i),
                            snr = status.getCn0DbHz(i),
                            usedInFix = status.usedInFix(i),
                            azimuth = status.getAzimuthDegrees(i),
                            elevation = status.getElevationDegrees(i)
                        )
                    )
                }
                val updated = updateSNRHistory(list)
                // Update UI state on main thread
                withContext(Dispatchers.Main) {
                    _satelliteList.value = list
                    _snrHistory.value = updated
                }
            }
        }
    }

    // === GNSS Measurements Callback ===
    private val gnssMeasurementCallback = object : GnssMeasurementsEvent.Callback() {
        override fun onGnssMeasurementsReceived(event: GnssMeasurementsEvent) {
            parsingScope.launch {
                val measurements = event.measurements.map { m ->
                    val constellation = when (m.constellationType) {
                        GnssStatus.CONSTELLATION_GPS -> "GPS"
                        GnssStatus.CONSTELLATION_GLONASS -> "GLONASS"
                        GnssStatus.CONSTELLATION_BEIDOU -> "BeiDou"
                        GnssStatus.CONSTELLATION_GALILEO -> "Galileo"
                        GnssStatus.CONSTELLATION_QZSS -> "QZSS"
                        GnssStatus.CONSTELLATION_IRNSS -> "IRNSS"
                        GnssStatus.CONSTELLATION_SBAS -> "SBAS"
                        else -> "Unknown"
                    }

                    GNSSMeasurementData(
                        svid = m.svid,
                        constellation = constellation,
                        cn0DbHz = m.cn0DbHz,
                        snrInDb = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) m.snrInDb else null,
                        accumulatedDeltaRangeMeters = m.accumulatedDeltaRangeMeters,
                        pseudorangeRateMetersPerSecond = m.pseudorangeRateMetersPerSecond,
                        accumulatedDeltaRangeUncertaintyMeters = m.accumulatedDeltaRangeUncertaintyMeters,
                        timeOffsetNanos = m.timeOffsetNanos
                    )
                }

                withContext(Dispatchers.Main) {
                    _gnssMeasurements.value = measurements
                }
            }
        }

        override fun onStatusChanged(status: Int) {
            Log.d("GNSS_Measurements", "Status changed: $status")
        }
    }

    init {
        loadGNSSHardwareInfo()
    }

    fun startGNSSInfo() {
        try {
            val executor = Executors.newSingleThreadExecutor()
            locationManager.registerGnssStatusCallback(executor, gnssCallback)
            locationManager.registerGnssMeasurementsCallback(executor, gnssMeasurementCallback)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun loadGNSSHardwareInfo() {
        val model = try { locationManager.gnssHardwareModelName } catch (_: Exception) { null } // API 28+
        val year = try { locationManager.gnssYearOfHardware } catch (_: Exception) { null }
        val caps =   try { locationManager.gnssCapabilities } catch (_: Exception) { null } // API 30+

        _gnssHardwareInfo.value = GNSSHardwareInfo(
            modelName = model,
            hardwareYear = year,
            capabilities = caps
        )
    }

    private fun updateSNRHistory(gnssStatusList: List<GNSSStatusData>): MutableMap<String, MutableList<Float>> {
        val allConstellations = (_snrHistory.value.keys + gnssStatusList.map { it.constellation }).toSet()

        val updated = mutableMapOf<String, MutableList<Float>>()

        allConstellations.forEach { constellation ->
            val oldHistory = _snrHistory.value[constellation] ?: emptyList()

            val newValue = gnssStatusList
                .filter { it.constellation == constellation && it.usedInFix }
                .map { it.snr }
                .averageOrNull()
                ?.toFloat() ?: 0f

            val newHistory = (oldHistory.takeLast(CHART_UPDATE_WINDOW - 1) + newValue).toMutableList()
            updated[constellation] = newHistory
        }

        return updated
    }

    private fun Iterable<Float>.averageOrNull(): Double? = if (this.any()) this.average() else null

    override fun onCleared() {
        super.onCleared()
        locationManager.unregisterGnssStatusCallback(gnssCallback)
        locationManager.unregisterGnssMeasurementsCallback(gnssMeasurementCallback)
        parsingScope.cancel()
    }
}
