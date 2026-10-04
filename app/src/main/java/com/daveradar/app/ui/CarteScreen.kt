package com.daveradar.app.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.location.Location
import android.location.LocationManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.daveradar.app.model.Appareil
import com.daveradar.app.ui.theme.CardBg
import com.daveradar.app.ui.theme.Neon
import com.daveradar.app.ui.theme.RadarBg
import com.daveradar.app.ui.theme.TextFaint
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import java.io.File
import kotlin.math.min

@Composable
fun CarteScreen(
    vm: RadarViewModel,
    onLocate: (Appareil) -> Unit,
    showDeviceList: Boolean = true
) {
    val context = LocalContext.current
    val mapView = remember { buildMapView(context) }
    val iconeMoi = remember { marqueurMoi(context) }
    val iconeAppareil = remember { marqueurAppareil(context) }
    var centeredOnce by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        mapView.onResume()
        onDispose { mapView.onPause(); mapView.onDetach() }
    }

    // clipToBounds est indispensable : Compose place les vues Android natives dans un
    // conteneur avec clipChildren = false, et la MapView osmdroid deborde alors sur le
    // volet voisin en affichage cote a cote.
    Box(Modifier.fillMaxSize().clipToBounds().background(RadarBg)) {

        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize(),
            update = { map ->
                val loc = vm.location
                val userGp = loc?.let { GeoPoint(it.latitude, it.longitude) }

                if (!centeredOnce && userGp != null) {
                    map.controller.setZoom(18.5)
                    map.controller.setCenter(userGp)
                    centeredOnce = true
                }

                map.overlays.clear()

                // 1. Halo de precision du point, dessine en premier donc sous tout le reste.
                if (userGp != null && loc != null && loc.accuracy > 0f) {
                    val halo = Polygon(map)
                    halo.points = Polygon.pointsAsCircle(userGp, loc.accuracy.toDouble())
                    halo.setFillColor(android.graphics.Color.argb(38, 0, 150, 255))
                    halo.setStrokeColor(android.graphics.Color.argb(110, 0, 170, 255))
                    halo.setStrokeWidth(2f)
                    map.overlays.add(halo)
                }

                // 2. Les appareils, en pastilles discretes : 38 epingles par defaut
                //    recouvraient entierement la position de l'utilisateur.
                if (userGp != null) {
                    vm.devices.forEach { d ->
                        val bearing = vm.referenceBearings[d.stableId]?.toDouble()
                            ?: estimatedBearing(d.stableId)
                        val dist = if (d.distance > 0) min(d.distance, vm.maxRange.toDouble()) else 8.0
                        val (lat, lon) = destination(userGp.latitude, userGp.longitude, dist, bearing)
                        val marker = Marker(map)
                        marker.position = GeoPoint(lat, lon)
                        marker.icon = iconeAppareil
                        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        marker.title = d.displayName
                        marker.snippet = formatDistance(d.distance)
                        marker.setOnMarkerClickListener { _, _ -> onLocate(d); true }
                        map.overlays.add(marker)
                    }
                }

                // 3. « Moi » ajoute en dernier : toujours dessine au-dessus de la grappe.
                userGp?.let {
                    val moi = Marker(map)
                    moi.position = it
                    moi.icon = iconeMoi
                    moi.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    moi.title = "Ma position"
                    moi.snippet = loc?.let { l -> etatPosition(l) }
                    map.overlays.add(moi)
                }
                map.invalidate()
            }
        )

        // Barre supérieure
        TopBar(
            count = vm.devices.size,
            scanning = vm.isScanning,
            noLocation = vm.location == null,
            location = vm.location,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // Bouton de recentrage : avec 38 appareils autour, on perd vite sa position.
        if (vm.location != null) {
            IconButton(
                onClick = {
                    vm.location?.let { l ->
                        mapView.controller.setZoom(18.5)
                        mapView.controller.animateTo(GeoPoint(l.latitude, l.longitude))
                    }
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 78.dp, end = 16.dp)
                    .clip(CircleShape)
                    .background(Color(0xE60A1626))
            ) {
                Icon(Icons.Filled.MyLocation, contentDescription = "Recentrer sur ma position", tint = Neon)
            }
        }

        // Panneau bas : liste des appareils.
        // Masque sur grand ecran, ou la liste occupe deja son propre volet.
        if (showDeviceList) {
            BottomPanel(
                devices = vm.devices,
                onLocate = onLocate,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

/** Pastille bleue cerclee de blanc : volontairement distincte du vert des appareils. */
private fun marqueurMoi(context: Context): Drawable {
    val taille = (context.resources.displayMetrics.density * 22f).toInt().coerceAtLeast(22)
    val bmp = Bitmap.createBitmap(taille, taille, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val r = taille / 2f
    val p = Paint(Paint.ANTI_ALIAS_FLAG)
    p.color = android.graphics.Color.argb(70, 0, 170, 255)
    canvas.drawCircle(r, r, r, p)
    p.color = android.graphics.Color.WHITE
    canvas.drawCircle(r, r, r * 0.60f, p)
    p.color = android.graphics.Color.rgb(0, 132, 255)
    canvas.drawCircle(r, r, r * 0.42f, p)
    return BitmapDrawable(context.resources, bmp)
}

/** Pastille verte compacte, a la place de l'epingle osmdroid par defaut. */
private fun marqueurAppareil(context: Context): Drawable {
    val taille = (context.resources.displayMetrics.density * 13f).toInt().coerceAtLeast(13)
    val bmp = Bitmap.createBitmap(taille, taille, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val r = taille / 2f
    val p = Paint(Paint.ANTI_ALIAS_FLAG)
    p.color = android.graphics.Color.argb(235, 10, 22, 38)
    canvas.drawCircle(r, r, r, p)
    p.color = android.graphics.Color.rgb(57, 255, 136)
    canvas.drawCircle(r, r, r * 0.62f, p)
    return BitmapDrawable(context.resources, bmp)
}

/** Rend « GPS - 6 m » ou « Reseau - 140 m » pour que la source soit lisible. */
private fun etatPosition(l: Location): String {
    val source = if (l.provider == LocationManager.GPS_PROVIDER) "GPS" else "Reseau"
    val precision = if (l.accuracy > 0f) " \u00b1${l.accuracy.toInt()} m" else ""
    return source + precision
}

private fun buildMapView(context: Context): MapView {
    val config = Configuration.getInstance()
    config.load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
    config.userAgentValue = context.packageName
    // Cache dans le stockage privé de l'app (évite toute permission de stockage)
    config.osmdroidBasePath = File(context.cacheDir, "osmdroid")
    config.osmdroidTileCache = File(context.cacheDir, "osmdroid/tiles")
    return MapView(context).apply {
        setTileSource(TileSourceFactory.MAPNIK)
        setMultiTouchControls(true)
        setUseDataConnection(true)
        controller.setZoom(17.0)
    }
}

@Composable
private fun TopBar(
    count: Int,
    scanning: Boolean,
    noLocation: Boolean,
    location: Location? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth().padding(12.dp)) {
        Row(
            Modifier
                .clip(RoundedCornerShape(50))
                .background(Color(0xE60A1626))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Dave Radar", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Box(Modifier.weight(1f))
            location?.let { l ->
                Text(
                    etatPosition(l),
                    color = if (l.provider == LocationManager.GPS_PROVIDER) Neon else Color(0xFFFFD166),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(end = 14.dp)
                )
            }
            Text(
                "$count appareil${if (count > 1) "s" else ""}",
                color = Neon, fontSize = 13.sp, fontWeight = FontWeight.SemiBold
            )
        }
        if (noLocation) {
            Text(
                "Activez la localisation pour placer les appareils sur la carte.",
                color = Color(0xFFFFD166), fontSize = 12.sp,
                modifier = Modifier.padding(top = 8.dp, start = 6.dp)
            )
        }
    }
}

@Composable
private fun BottomPanel(devices: List<Appareil>, onLocate: (Appareil) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .fillMaxHeight(0.42f)
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(Color(0xF20A1626))
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Appareils détectés", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Box(Modifier.weight(1f))
            Text("${devices.size}", color = Neon, fontWeight = FontWeight.Bold)
        }
        if (devices.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Recherche autour de vous…", color = TextFaint, fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(devices, key = { it.stableId }) { d ->
                    DeviceRow(device = d, onClick = { onLocate(d) })
                }
            }
        }
    }
}
