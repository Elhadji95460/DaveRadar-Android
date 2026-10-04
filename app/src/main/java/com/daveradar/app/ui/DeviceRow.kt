package com.daveradar.app.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daveradar.app.model.Appareil
import com.daveradar.app.ui.theme.CardBg
import com.daveradar.app.ui.theme.Neon
import com.daveradar.app.ui.theme.TextFaint

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DeviceRow(
    device: Appareil,
    selected: Boolean = false,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) Neon.copy(alpha = 0.12f) else CardBg)
            .border(
                width = 1.dp,
                color = if (selected) Neon.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.06f),
                shape = RoundedCornerShape(16.dp)
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Neon.copy(alpha = 0.14f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(iconFor(device.kind), contentDescription = null, tint = Neon, modifier = Modifier.size(20.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                device.displayName,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                maxLines = 1
            )
            val subtitle = buildString {
                append(device.kind.label)
                device.vendor?.let { append(" · $it") }
                if (device.nomPersonnalise != null) append(" · nommé")
            }
            Text(subtitle, color = TextFaint, fontSize = 12.sp, maxLines = 1)
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                formatDistance(device.distance),
                color = Neon,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            SignalBars(device.rssi, Neon, Modifier.padding(top = 4.dp))
        }
    }
}
