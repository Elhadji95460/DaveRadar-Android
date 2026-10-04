package com.daveradar.app.ui

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daveradar.app.model.Appareil
import com.daveradar.app.ui.theme.Neon
import com.daveradar.app.ui.theme.RadarBg
import com.daveradar.app.ui.theme.TextDim
import com.daveradar.app.ui.theme.TextFaint
import com.daveradar.app.ui.theme.UwbCyan
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.min

@Composable
fun PrecisionLocateScreen(
    vm: RadarViewModel,
    stableId: String,
    onClose: () -> Unit
) {
    // On suit la version « vivante » de l'appareil (RSSI/distance à jour).
    val device: Appareil = vm.current(stableId) ?: run {
        LaunchedEffect(Unit) { onClose() }
        return
    }

    val isUwb = device.supportsUwb
    val accent = if (isUwb) UwbCyan else Neon

    val ref = vm.referenceBearings[stableId]
    val arrowAngle: Float? = ref?.let { normalizeDeg((it - vm.heading).toDouble()).toFloat() }
    val animatedAngle by animateFloatAsState(targetValue = arrowAngle ?: 0f, label = "arrow")

    val closeness = if (device.distance in 0.01..10.0) (1.0 - device.distance / 10.0).toFloat() else 0f

    // Bip de proximité (s'accélère quand on approche).
    var soundOn by remember { mutableStateOf(false) }
    val tone = remember { runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 100) }.getOrNull() }
    DisposableEffect(Unit) { onDispose { tone?.release() } }
    LaunchedEffect(soundOn) {
        while (soundOn) {
            runCatching { tone?.startTone(ToneGenerator.TONE_PROP_BEEP, 120) }
            val d = device.distance
            val interval = if (d <= 0) 1300L else max(140.0, min(1400.0, d / 8.0 * 1000)).toLong()
            delay(interval)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(accent.copy(alpha = 0.16f + 0.24f * closeness), RadarBg),
                    radius = 1400f
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // En-tête
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text("LOCALISER", color = TextFaint, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        device.displayName,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2
                    )
                    val sub = buildString {
                        append(device.kind.label)
                        device.vendor?.let { append(" · $it") }
                    }
                    Text(sub, color = TextFaint, fontSize = 13.sp)
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.Close, contentDescription = "Fermer", tint = TextDim)
                }
            }

            Spacer(Modifier.weight(1f))

            // Flèche directionnelle
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(240.dp)) {
                Box(
                    Modifier
                        .size(220.dp)
                        .border(2.dp, accent.copy(alpha = 0.12f + 0.18f * closeness), CircleShape)
                )
                if (arrowAngle != null) {
                    Icon(
                        Icons.Filled.Navigation,
                        contentDescription = "Direction",
                        tint = accent,
                        modifier = Modifier
                            .size(170.dp)
                            .rotate(animatedAngle)
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.LocationSearching,
                            contentDescription = null,
                            tint = accent.copy(alpha = 0.6f),
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text("Direction inconnue", color = TextDim, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // Distance
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val d = device.distance
                Text(
                    if (d <= 0) "—" else String.format("%.1f", d),
                    color = accent,
                    fontSize = 68.sp,
                    fontWeight = FontWeight.Black
                )
                Text("m", color = TextDim, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
            }

            // Badge mode de connexion
            ConnectionBadge(isUwb = isUwb, accent = accent)

            Spacer(Modifier.height(18.dp))

            // Bouton alerte sonore
            SoundButton(active = soundOn, accent = accent) { soundOn = !soundOn }

            Spacer(Modifier.height(16.dp))

            // Aide / calibration
            if (arrowAngle == null) {
                Text(
                    "Pointez le téléphone vers l'appareil, puis calibrez pour activer la flèche.",
                    color = TextFaint, fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
                CalibrateButton(accent) { vm.calibrate(device) }
            } else {
                Text(
                    "Direction estimée via la boussole. Déplacez-vous : la distance s'affine avec le signal.",
                    color = TextFaint, fontSize = 12.sp
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ConnectionBadge(isUwb: Boolean, accent: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .background(accent.copy(alpha = 0.14f), RoundedCornerShape(50))
            .border(1.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(50))
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Icon(
            if (isUwb) Icons.Filled.NearMe else Icons.Filled.Bluetooth,
            contentDescription = null, tint = accent, modifier = Modifier.size(16.dp)
        )
        Text(
            if (isUwb) "UWB · ultra-précis" else "Bluetooth · direction estimée",
            color = accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SoundButton(active: Boolean, accent: Color, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (active) accent else Color.White.copy(alpha = 0.08f))
            .border(
                1.dp,
                accent.copy(alpha = if (active) 0f else 0.35f),
                RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 15.dp)
    ) {
        Icon(
            Icons.Filled.VolumeUp,
            contentDescription = null,
            tint = if (active) Color.Black else accent,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.size(10.dp))
        Text(
            if (active) "Alerte sonore activée" else "Activer l'alerte sonore",
            color = if (active) Color.Black else Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )
    }
}

@Composable
private fun CalibrateButton(accent: Color, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(accent)
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 12.dp)
    ) {
        Icon(Icons.Filled.NearMe, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
        Spacer(Modifier.size(8.dp))
        Text("Calibrer la direction", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}
