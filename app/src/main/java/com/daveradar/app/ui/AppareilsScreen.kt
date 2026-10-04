package com.daveradar.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daveradar.app.model.Appareil
import com.daveradar.app.ui.theme.Neon
import com.daveradar.app.ui.theme.TextFaint

@Composable
fun AppareilsScreen(vm: RadarViewModel, onLocate: (Appareil) -> Unit) {
    var editing by remember { mutableStateOf<Appareil?>(null) }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Appareils", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Box(Modifier.weight(1f))
            Text("${vm.devices.size}", color = Neon, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        if (vm.devices.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Recherche d'appareils…\nBluetooth actif requis.",
                    color = TextFaint, fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(vm.devices, key = { it.stableId }) { device ->
                    DeviceRow(
                        device = device,
                        onClick = { onLocate(device) },
                        onLongClick = { editing = device }
                    )
                }
            }
        }
    }

    editing?.let { device ->
        RenameDialog(
            device = device,
            onDismiss = { editing = null },
            onRename = { name -> vm.rename(device, name); editing = null },
            onForget = { vm.forget(device); editing = null }
        )
    }
}

@Composable
private fun RenameDialog(
    device: Appareil,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit,
    onForget: () -> Unit
) {
    var text by remember { mutableStateOf(device.nomPersonnalise ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Renommer l'appareil") },
        text = {
            Column {
                Text("Détecté : ${device.nomAuto}", color = TextFaint, fontSize = 13.sp)
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    label = { Text("Nom personnalisé") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onRename(text) },
                colors = ButtonDefaults.buttonColors(containerColor = Neon, contentColor = Color.Black)
            ) { Text("Enregistrer") }
        },
        dismissButton = {
            TextButton(onClick = onForget) { Text("Oublier le nom", color = Color(0xFFFF6B6B)) }
        }
    )
}
