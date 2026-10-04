package com.daveradar.app.ui

import android.app.Application
import android.location.Location
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.daveradar.app.ble.BluetoothScanner
import com.daveradar.app.ble.Sighting
import com.daveradar.app.data.NameStore
import com.daveradar.app.model.Appareil
import com.daveradar.app.model.DeviceKind
import com.daveradar.app.sensors.SensorsManager

/**
 * ViewModel central de Dave Radar. Orchestre le scan Bluetooth, les capteurs
 * (boussole + position), la fusion des détections et la persistance des noms.
 */
class RadarViewModel(app: Application) : AndroidViewModel(app) {

    private val scanner = BluetoothScanner(app)
    private val sensors = SensorsManager(app)
    private val names = NameStore(app)
    private val main = Handler(Looper.getMainLooper())
    private val tone: ToneGenerator? = runCatching {
        ToneGenerator(AudioManager.STREAM_MUSIC, 90)
    }.getOrNull()

    /** Dernière détection connue par identifiant stable. */
    private val latest = LinkedHashMap<String, Appareil>()

    /** Liste observable, triée du plus proche au plus loin. */
    val devices = mutableStateListOf<Appareil>()

    var heading by mutableFloatStateOf(0f)
        private set
    var location by mutableStateOf<Location?>(null)
        private set
    var isScanning by mutableStateOf(false)
        private set
    var bluetoothOn by mutableStateOf(false)
        private set

    /** Directions calibrées manuellement (identifiant → cap de référence). */
    val referenceBearings = mutableStateMapOf<String, Float>()

    // Réglages
    var maxRange by mutableFloatStateOf(50f)
    var soundAlertUnknown by mutableStateOf(false)

    private var lastUnknownAlert = 0L

    init {
        scanner.onSighting = { s -> main.post { ingest(s) } }
        sensors.onHeading = { h -> heading = h }
        sensors.onLocation = { loc -> location = loc }
        scheduleTick()
    }

    // MARK: - Contrôle

    fun startScanning() {
        bluetoothOn = scanner.isBluetoothOn
        scanner.start()
        sensors.start()
        isScanning = scanner.isScanning
        location = sensors.lastLocation
    }

    fun stopScanning() {
        scanner.stop()
        sensors.stop()
        isScanning = false
    }

    fun refreshBluetoothState() {
        bluetoothOn = scanner.isBluetoothOn
        isScanning = scanner.isScanning
    }

    // MARK: - Fusion des détections

    private fun ingest(s: Sighting) {
        val custom = names.nameFor(s.stableId)
        latest[s.stableId] = Appareil(
            stableId = s.stableId,
            nomAuto = s.nomAuto,
            nomPersonnalise = custom,
            vendor = s.vendor,
            kind = s.kind,
            rssi = s.rssi,
            lastSeen = SystemClock.elapsedRealtime(),
            supportsUwb = s.supportsUwb,
            sources = setOf(s.source)
        )
        rebuild()
        maybeAlert(latest[s.stableId]!!)
    }

    private fun rebuild() {
        val cutoff = SystemClock.elapsedRealtime() - STALE_MS
        latest.entries.removeAll { it.value.lastSeen < cutoff }
        val sorted = latest.values.sortedBy {
            if (it.distance > 0) it.distance else Double.MAX_VALUE
        }
        devices.clear()
        devices.addAll(sorted)
    }

    /** Boucle de rafraîchissement (purge des appareils périmés) toutes les secondes. */
    private fun scheduleTick() {
        main.postDelayed({ rebuild(); scheduleTick() }, TICK_MS)
    }

    // MARK: - Alerte inconnu proche

    private fun maybeAlert(a: Appareil) {
        if (!soundAlertUnknown) return
        if (a.kind != DeviceKind.UNKNOWN) return
        val d = a.distance
        if (d <= 0 || d > 3.0) return
        val now = SystemClock.elapsedRealtime()
        if (now - lastUnknownAlert < 3000) return
        lastUnknownAlert = now
        runCatching { tone?.startTone(ToneGenerator.TONE_PROP_BEEP, 150) }
    }

    // MARK: - Noms & calibration

    fun rename(a: Appareil, newName: String) {
        val clean = newName.trim()
        if (clean.isEmpty()) { forget(a); return }
        names.setName(a.stableId, clean)
        latest[a.stableId]?.let { latest[a.stableId] = it.copy(nomPersonnalise = clean) }
        rebuild()
    }

    fun forget(a: Appareil) {
        names.forget(a.stableId)
        latest[a.stableId]?.let { latest[a.stableId] = it.copy(nomPersonnalise = null) }
        rebuild()
    }

    fun calibrate(a: Appareil) {
        referenceBearings[a.stableId] = heading
    }

    /** Version « vivante » de l'appareil (RSSI/distance à jour) pour l'écran Localiser. */
    fun current(stableId: String): Appareil? = devices.firstOrNull { it.stableId == stableId }

    override fun onCleared() {
        super.onCleared()
        main.removeCallbacksAndMessages(null)
        scanner.stop()
        sensors.stop()
        tone?.release()
    }

    companion object {
        private const val STALE_MS = 30_000L
        private const val TICK_MS = 1_000L
    }
}
