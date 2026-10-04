package com.daveradar.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daveradar.app.ui.theme.CardBg
import com.daveradar.app.ui.theme.Neon
import com.daveradar.app.ui.theme.TextDim
import com.daveradar.app.ui.theme.TextFaint
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(vm: RadarViewModel) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Paramètres", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)

        Card {
            RowSwitch(
                title = "Alerte inconnu proche",
                subtitle = "Bip quand un appareil non identifié passe sous 3 m",
                checked = vm.soundAlertUnknown,
                onChecked = { vm.soundAlertUnknown = it }
            )
        }

        Card {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Portée de la carte", color = Color.White, fontSize = 15.sp, modifier = Modifier.weight(1f))
                    Text("${vm.maxRange.roundToInt()} m", color = Neon, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = vm.maxRange,
                    onValueChange = { vm.maxRange = it },
                    valueRange = 5f..150f,
                    colors = SliderDefaults.colors(
                        thumbColor = Neon,
                        activeTrackColor = Neon,
                        inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                    )
                )
                Text(
                    "Distance maximale à laquelle les appareils sont projetés autour de vous.",
                    color = TextFaint, fontSize = 12.sp
                )
            }
        }

        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("À propos", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                InfoLine("Version", "1.0")
                InfoLine("Détection", "Bluetooth LE")
                InfoLine("Localisation précise", "Boussole + RSSI")
                Text(
                    "Cet appareil ne dispose pas d'UWB : la direction est estimée via la boussole (calibration manuelle). L'UWB centimétrique nécessite un matériel compatible.",
                    color = TextFaint, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
    ) { content() }
}

@Composable
private fun RowSwitch(title: String, subtitle: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 15.sp)
            Text(subtitle, color = TextFaint, fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChecked,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = Neon,
                uncheckedTrackColor = Color.White.copy(alpha = 0.15f)
            )
        )
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, color = TextDim, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}
