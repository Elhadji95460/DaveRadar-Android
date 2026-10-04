package com.daveradar.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.daveradar.app.ui.AppRoot
import com.daveradar.app.ui.RadarViewModel
import com.daveradar.app.ui.theme.DaveRadarTheme

class MainActivity : ComponentActivity() {

    private val vm: RadarViewModel by viewModels()

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            // Qu'elles soient toutes accordées ou non, on démarre : le scan et la
            // localisation s'activeront selon les permissions réellement obtenues.
            vm.startScanning()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { DaveRadarTheme { AppRoot(vm) } }
        ensurePermissions()
    }

    override fun onResume() {
        super.onResume()
        vm.refreshBluetoothState()
    }

    private fun ensurePermissions() {
        val needed = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_SCAN)
                add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        val missing = needed.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) vm.startScanning()
        else permissionLauncher.launch(missing.toTypedArray())
    }
}
