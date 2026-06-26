package com.example.gnssmap.data.viewmodel

import android.app.Application
import android.location.GnssMeasurementsEvent
import android.location.GnssStatus
import android.location.LocationManager
import androidx.lifecycle.AndroidViewModel
import com.example.gnssmap.data.GNSSStatusData
import com.example.gnssmap.data.GNSSHardwareInfo
import com.example.gnssmap.data.GNSSMeasurementData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import android.os.Build
import com.example.gnssmap.data.AzElHistory
import com.example.gnssmap.data.CHART_UPDATE_WINDOW
import com.example.gnssmap.data.EPS
import com.example.gnssmap.data.TimestampedData
import com.example.gnssmap.utils.averageOrNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

class GNSSViewModel(application: Application) : AndroidViewModel(application) {
    private val locationManager = application.getSystemService(Application.LOCATION_SERVICE) as LocationManager
    private val gnssStatusExecutor = Executors.newSingleThreadExecutor()
    private val gnssMeasurementsExecutor = Executors.newSingleThreadExecutor()
    private val gnssCallbackScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _satelliteList = MutableStateFlow<List<GNSSStatusData>>(listOf())
    val satelliteList: StateFlow<List<GNSSStatusData>> = _satelliteList

    private val _gnssMeasurements = MutableStateFlow<List<GNSSMeasurementData>>(listOf())
    val gnssMeasurements: StateFlow<List<GNSSMeasurementData>> = _gnssMeasurements

    private val _azElHistory = MutableStateFlow<Map<String, AzElHistory>>(emptyMap())
    val azElHistory: StateFlow<Map<String, AzElHistory>> = _azElHistory


    private val _gnssHardwareInfo = MutableStateFlow(GNSSHardwareInfo())
    val gnssHardwareInfo: StateFlow<GNSSHardwareInfo> = _gnssHardwareInfo


    private val _constellationSNRHistory = MutableStateFlow<Map<String, MutableList<TimestampedData>>>(emptyMap())
    val constellationSNRHistory: StateFlow<Map<String, List<TimestampedData>>> = _constellationSNRHistory

    private val _satelliteSNRHistory = MutableStateFlow<Map<String, MutableList<TimestampedData>>>(emptyMap())
    val satelliteSNRHistory: StateFlow<Map<String, List<TimestampedData>>> = _satelliteSNRHistory

    private val _satelliteCountHistory = MutableStateFlow<Map<String, MutableList<TimestampedData>>>(emptyMap())
    val satelliteCountHistory: StateFlow<Map<String, List<TimestampedData>>> = _satelliteCountHistory

    private val gnssCallback = object : GnssStatus.Callback() {
        override fun onSatelliteStatusChanged(status: GnssStatus) {
            gnssCallbackScope.launch {
                val list = mutableListOf<GNSSStatusData>()
                val newAzElMap = _azElHistory.value.toMutableMap()
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
                    val key = "${constellation}:${svid}"

                    val az = status.getAzimuthDegrees(i)
                    val el = status.getElevationDegrees(i)

                    val prev = newAzElMap[key]
                    if (prev == null) {
                        // first time we see this satellite -> set first==last==current
                        newAzElMap[key] = AzElHistory(
                            firstAz = az,
                            firstEl = el,
                            lastAz = az,
                            lastEl = el
                        )
                    } else {
                        // update last if changed
                        val azChanged = kotlin.math.abs(az - prev.lastAz) > EPS
                        val elChanged = kotlin.math.abs(el - prev.lastEl) > EPS
                        if (azChanged || elChanged) {
                            newAzElMap[key] = AzElHistory(
                                firstAz = prev.firstAz,
                                firstEl = prev.firstEl,
                                lastAz = az,
                                lastEl = el
                            )
                        }
                    }

                    list.add(
                        GNSSStatusData(
                            constellation = constellation,
                            prn = status.getSvid(i),
                            cn0DbHz = status.getCn0DbHz(i),
                            carrierFrequencyRangeHz = status.getCarrierFrequencyHz(i),
                            usedInFix = status.usedInFix(i),
                            azimuth = status.getAzimuthDegrees(i),
                            elevation = status.getElevationDegrees(i),
                            hasEphemeris = status.hasEphemerisData(i),
                            hasAlmanac = status.hasAlmanacData(i)
                        )
                    )
                }
                val updatedConstellation = updateConstellationSNRHistory(list)
                val updatedSatCount = updateConstellationFixCountHistory(list)
                val updatedSatellite = updateSatelliteSNRHistory(list)

                withContext(Dispatchers.Main) {
                    _satelliteList.value = list
                    _constellationSNRHistory.value = updatedConstellation
                    _satelliteCountHistory.value = updatedSatCount
                    _satelliteSNRHistory.value = updatedSatellite
                    _azElHistory.value = newAzElMap.toMap()
                }
            }
        }
    }

    private val gnssMeasurementCallback = object : GnssMeasurementsEvent.Callback() {
        override fun onGnssMeasurementsReceived(event: GnssMeasurementsEvent) {
            gnssCallbackScope.launch {
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
                        carrierFrequencyRangeHz = m.carrierFrequencyHz,
                        cn0DbHz = m.cn0DbHz,
                        snrInDb = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) m.snrInDb else null,
                        accumulatedDeltaRangeMeters = m.accumulatedDeltaRangeMeters,
                        accumulatedDeltaRangeUncertaintyMeters = m.accumulatedDeltaRangeUncertaintyMeters,
                        pseudorangeRateMetersPerSecond = m.pseudorangeRateMetersPerSecond,
                        pseudorangeRateUncertaintyMetersPerSecond = m.pseudorangeRateUncertaintyMetersPerSecond,
                        timeOffsetNanos = m.timeOffsetNanos
                    )
                }
                // Aktualizacja stanu w głównym wątku
                withContext(Dispatchers.Main) {
                    _gnssMeasurements.value = measurements
                }
            }
        }

        override fun onStatusChanged(status: Int) {
            //Log.d("GNSS_Measurements", "Status changed: $status")
        }
    }

    fun startGNSSInfo() {
        try {
            locationManager.registerGnssStatusCallback(gnssStatusExecutor, gnssCallback)
            locationManager.registerGnssMeasurementsCallback(gnssMeasurementsExecutor, gnssMeasurementCallback)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    fun loadGNSSHardwareInfo() {
        val model = try { locationManager.gnssHardwareModelName } catch (_: Exception) { null }
        val year = try { locationManager.gnssYearOfHardware } catch (_: Exception) { null }
        val caps = try { locationManager.gnssCapabilities } catch (_: Exception) { null }

        _gnssHardwareInfo.value = GNSSHardwareInfo(
            modelName = model,
            hardwareYear = year,
            capabilities = caps
        )
    }

    private fun updateSatelliteSNRHistory(gnssStatusData: List<GNSSStatusData>): MutableMap<String, MutableList<TimestampedData>> {
        val allSatellites = (_satelliteSNRHistory.value.keys + gnssStatusData.map { "${it.constellation}:${it.prn}" }).toSet()
        val updated = mutableMapOf<String, MutableList<TimestampedData>>()
        val now = System.currentTimeMillis()

        allSatellites.forEach { satelliteKey ->
            val oldHistory = _satelliteSNRHistory.value[satelliteKey] ?: emptyList()
            val (constellation, prn) = satelliteKey.split(":")
            val newValues = gnssStatusData
                .filter { it.constellation == constellation && it.prn.toString() == prn }
                .map { TimestampedData(now, it.cn0DbHz) }

            val newHistory = (oldHistory.takeLast(CHART_UPDATE_WINDOW - 1) + newValues).toMutableList()
            updated[satelliteKey] = newHistory
        }

        return updated
    }

    private fun updateConstellationSNRHistory(gnssStatusList: List<GNSSStatusData>): MutableMap<String, MutableList<TimestampedData>> {
        val allConstellations = (_constellationSNRHistory.value.keys + gnssStatusList.map { it.constellation }).toSet()
        val updated = mutableMapOf<String, MutableList<TimestampedData>>()
        val now = System.currentTimeMillis()

        allConstellations.forEach { constellation ->
            val oldHistory = _constellationSNRHistory.value[constellation] ?: emptyList()

            val newValue = gnssStatusList
                .filter { it.constellation == constellation && it.usedInFix }
                .map { it.cn0DbHz }
                .averageOrNull()
                ?.toFloat() ?: 0f

            val newEntry = TimestampedData(now, newValue)
            val newHistory = (oldHistory.takeLast(CHART_UPDATE_WINDOW - 1) + newEntry).toMutableList()
            updated[constellation] = newHistory
        }

        return updated
    }

    private fun updateConstellationFixCountHistory(gnssStatusList: List<GNSSStatusData>): MutableMap<String, MutableList<TimestampedData>> {

        val allConstellations = (satelliteCountHistory.value.keys + gnssStatusList.map { it.constellation }).toSet()
        val updated = mutableMapOf<String, MutableList<TimestampedData>>()
        val now = System.currentTimeMillis()

        allConstellations.forEach { constellation ->
            val oldHistory = satelliteCountHistory.value[constellation] ?: emptyList()

            val fixCount = gnssStatusList.count { it.constellation == constellation && it.usedInFix }

            val newEntry = TimestampedData(now, fixCount.toFloat())
            val newHistory = (oldHistory.takeLast(CHART_UPDATE_WINDOW - 1) + newEntry).toMutableList()
            updated[constellation] = newHistory
        }
        return updated
    }


    override fun onCleared() {
        super.onCleared()
        locationManager.unregisterGnssStatusCallback(gnssCallback)
        locationManager.unregisterGnssMeasurementsCallback(gnssMeasurementCallback)

        gnssCallbackScope.cancel()

        gnssStatusExecutor.shutdown()
        gnssMeasurementsExecutor.shutdown()
    }
}
