package com.daveradar.app.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeviceUnknown
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.TabletMac
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Watch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.daveradar.app.model.DeviceKind
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** Icône correspondant à un type d'appareil. */
fun iconFor(kind: DeviceKind): ImageVector = when (kind) {
    DeviceKind.PHONE -> Icons.Filled.Smartphone
    DeviceKind.LAPTOP -> Icons.Filled.Laptop
    DeviceKind.TABLET -> Icons.Filled.TabletMac
    DeviceKind.WATCH -> Icons.Filled.Watch
    DeviceKind.EARBUDS -> Icons.Filled.Headphones
    DeviceKind.SPEAKER -> Icons.Filled.Speaker
    DeviceKind.TV -> Icons.Filled.Tv
    DeviceKind.ACCESSORY -> Icons.Filled.Memory
    DeviceKind.UNKNOWN -> Icons.Filled.DeviceUnknown
}

/** Distance formatée : « 3,4 m » ou « — » si inconnue. */
fun formatDistance(d: Double): String {
    if (d <= 0) return "—"
    return if (d < 10) String.format("%.1f m", d) else "${d.roundToInt()} m"
}

/** Force du signal 0..4 à partir du RSSI. */
fun signalLevel(rssi: Int): Int = when {
    rssi == 0 -> 0
    rssi >= -55 -> 4
    rssi >= -67 -> 3
    rssi >= -78 -> 2
    rssi >= -90 -> 1
    else -> 0
}

/** Petites barres de signal (style Wi-Fi). */
@Composable
fun SignalBars(rssi: Int, color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    val level = signalLevel(rssi)
    // Alignment.Bottom est indispensable : sans lui, Compose aligne en haut par defaut
    // et les barres s'allongent vers le bas, ce qui retourne l'escalier de signal.
    Row(modifier, verticalAlignment = Alignment.Bottom) {
        for (i in 0 until 4) {
            val h = (5 + i * 3).dp
            Spacer(
                Modifier
                    .padding(horizontal = 1.dp)
                    .size(width = 3.dp, height = h)
                    .background(
                        if (i < level) color else color.copy(alpha = 0.18f),
                        RoundedCornerShape(1.dp)
                    )
            )
        }
    }
}

/**
 * Direction estimée stable (0..360) pour un appareil non calibré, dérivée de son
 * identifiant : les blips se répartissent ainsi autour de l'utilisateur.
 */
fun estimatedBearing(stableId: String): Double {
    return (abs(stableId.hashCode()) % 360).toDouble()
}

/**
 * Point destination à `distanceMeters` et `bearingDeg` d'un point de départ
 * (formule de navigation « great-circle »). Renvoie (lat, lon).
 */
fun destination(lat: Double, lon: Double, distanceMeters: Double, bearingDeg: Double): Pair<Double, Double> {
    val earth = 6_371_000.0
    val delta = distanceMeters / earth
    val theta = Math.toRadians(bearingDeg)
    val phi1 = Math.toRadians(lat)
    val lambda1 = Math.toRadians(lon)
    val phi2 = asin(sin(phi1) * cos(delta) + cos(phi1) * sin(delta) * cos(theta))
    val lambda2 = lambda1 + atan2(
        sin(theta) * sin(delta) * cos(phi1),
        cos(delta) - sin(phi1) * sin(phi2)
    )
    return Math.toDegrees(phi2) to Math.toDegrees(lambda2)
}

/** Ramène un angle dans [0, 360). */
fun normalizeDeg(deg: Double): Double {
    var a = deg % 360.0
    if (a < 0) a += 360.0
    return a
}
