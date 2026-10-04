package com.daveradar.app.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import java.util.Locale
import kotlin.math.abs

/**
 * Scanner Bluetooth Low Energy.
 *
 * Balaie en continu les périphériques BLE, lit le RSSI, extrait le nom et le
 * « company ID » fabricant, puis émet un [Sighting] par détection.
 */
class BluetoothScanner(private val context: Context) {

    var onSighting: ((Sighting) -> Unit)? = null

    private val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val adapter get() = manager?.adapter
    private var scanner: BluetoothLeScanner? = null
    var isScanning: Boolean = false
        private set

    val isBluetoothOn: Boolean get() = adapter?.isEnabled == true

    private val settings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
        .apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
            }
        }
        .setReportDelay(0L)
        .build()

    private val callback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            handle(result)
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            results.forEach { handle(it) }
        }
    }

    @SuppressLint("MissingPermission")
    fun start() {
        if (isScanning) return
        if (!hasScanPermission(context)) return
        scanner = adapter?.bluetoothLeScanner ?: return
        try {
            scanner?.startScan(null, settings, callback)
            isScanning = true
        } catch (_: SecurityException) {
            isScanning = false
        } catch (_: IllegalStateException) {
            isScanning = false
        }
    }

    @SuppressLint("MissingPermission")
    fun stop() {
        if (!isScanning) return
        try {
            scanner?.stopScan(callback)
        } catch (_: Exception) {
            // ignore : Bluetooth peut avoir été coupé entre-temps
        }
        isScanning = false
    }

    private fun handle(result: ScanResult) {
        val record = result.scanRecord
        val name = record?.deviceName
        val msd = record?.manufacturerSpecificData
        val companyId = if (msd != null && msd.size() > 0) msd.keyAt(0) else null

        val ident = DeviceIdentifier.identify(name, companyId)
        onSighting?.invoke(
            Sighting(
                stableId = stableId(result, name, companyId),
                nomAuto = ident.nomAuto,
                vendor = ident.vendor,
                kind = ident.kind,
                rssi = result.rssi,
                source = "bluetooth"
            )
        )
    }

    /**
     * Identifiant stable : l'adresse MAC quand elle est exploitable (le cas le plus
     * fréquent sur Android, contrairement à iOS), sinon une signature dérivée du
     * nom + fabricant pour rester cohérent d'une session à l'autre.
     */
    private fun stableId(result: ScanResult, name: String?, companyId: Int?): String {
        val addr = result.device?.address
        val randomised = addr == null || addr.startsWith("02:00:00")
        return if (!randomised) {
            "mac:${addr!!.uppercase(Locale.ROOT)}"
        } else {
            val basis = (name.orEmpty() + "|" + (companyId ?: 0))
            "sig:${abs(basis.hashCode())}"
        }
    }

    companion object {
        /** Permission requise pour scanner selon la version d'Android. */
        fun hasScanPermission(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) ==
                    PackageManager.PERMISSION_GRANTED
            } else {
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED
            }
        }
    }
}
