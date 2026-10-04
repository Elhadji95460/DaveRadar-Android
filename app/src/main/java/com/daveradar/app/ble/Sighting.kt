package com.daveradar.app.ble

import com.daveradar.app.model.DeviceKind

/** Détection brute émise par un scanner, avant fusion par le ViewModel. */
data class Sighting(
    val stableId: String,
    val nomAuto: String,
    val vendor: String?,
    val kind: DeviceKind,
    val rssi: Int,
    val source: String,
    val supportsUwb: Boolean = false
)
