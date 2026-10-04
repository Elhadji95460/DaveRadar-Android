package com.daveradar.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.daveradar.app.model.Appareil
import com.daveradar.app.ui.theme.Neon
import com.daveradar.app.ui.theme.RadarBg
import com.daveradar.app.ui.theme.RadarNavy

private const val ONGLET_CARTE = 0
private const val ONGLET_APPAREILS = 1
private const val ONGLET_PARAMETRES = 2

@Composable
fun AppRoot(vm: RadarViewModel) {
    var tab by remember { mutableIntStateOf(ONGLET_CARTE) }
    var locateId by remember { mutableStateOf<String?>(null) }

    // Écran Localiser en plein écran par-dessus tout.
    val id = locateId
    if (id != null) {
        PrecisionLocateScreen(vm, id, onClose = { locateId = null })
        return
    }

    val onLocate: (Appareil) -> Unit = { locateId = it.stableId }

    when (largeurEcran()) {
        LargeurEcran.COMPACTE -> DispositionTelephone(vm, tab, onLocate) { tab = it }
        LargeurEcran.MOYENNE -> DispositionTablette(vm, tab, onLocate, cote_a_cote = false) { tab = it }
        LargeurEcran.ETENDUE -> DispositionTablette(vm, tab, onLocate, cote_a_cote = true) { tab = it }
    }
}

/** Téléphone : barre de navigation en bas, un seul écran à la fois. */
@Composable
private fun DispositionTelephone(
    vm: RadarViewModel,
    tab: Int,
    onLocate: (Appareil) -> Unit,
    onTab: (Int) -> Unit
) {
    Scaffold(
        containerColor = RadarBg,
        bottomBar = {
            NavigationBar(containerColor = RadarNavy) {
                NavItem(tab == ONGLET_CARTE, "Carte", Icons.Filled.Map) { onTab(ONGLET_CARTE) }
                NavItem(tab == ONGLET_APPAREILS, "Appareils", Icons.AutoMirrored.Filled.FormatListBulleted) { onTab(ONGLET_APPAREILS) }
                NavItem(tab == ONGLET_PARAMETRES, "Paramètres", Icons.Filled.Settings) { onTab(ONGLET_PARAMETRES) }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            EcranSimple(tab, vm, onLocate)
        }
    }
}

/**
 * Tablette : rail de navigation sur le côté (il laisse toute la hauteur à la carte).
 * En paysage (cote_a_cote), la liste d'appareils et la carte sont affichées ensemble :
 * un écran de 10 pouces a la largeur pour les deux, et cliquer un appareil dans la
 * liste le met en évidence sur la carte sans changer d'écran.
 */
@Composable
private fun DispositionTablette(
    vm: RadarViewModel,
    tab: Int,
    onLocate: (Appareil) -> Unit,
    cote_a_cote: Boolean,
    onTab: (Int) -> Unit
) {
    val surParametres = tab == ONGLET_PARAMETRES

    Scaffold(containerColor = RadarBg) { padding ->
        Row(Modifier.fillMaxSize().padding(padding)) {

            NavigationRail(containerColor = RadarNavy) {
                if (cote_a_cote) {
                    // Carte et Appareils sont réunis : deux destinations suffisent.
                    RailItem(!surParametres, "Radar", Icons.Filled.Radar) { onTab(ONGLET_CARTE) }
                } else {
                    RailItem(tab == ONGLET_CARTE, "Carte", Icons.Filled.Map) { onTab(ONGLET_CARTE) }
                    RailItem(tab == ONGLET_APPAREILS, "Appareils", Icons.AutoMirrored.Filled.FormatListBulleted) { onTab(ONGLET_APPAREILS) }
                }
                RailItem(surParametres, "Paramètres", Icons.Filled.Settings) { onTab(ONGLET_PARAMETRES) }
            }

            Box(Modifier.weight(1f).fillMaxHeight()) {
                when {
                    surParametres -> PanneauCentre { SettingsScreen(vm) }

                    cote_a_cote -> Row(Modifier.fillMaxSize()) {
                        Box(Modifier.weight(0.38f).fillMaxHeight()) {
                            AppareilsScreen(vm, onLocate)
                        }
                        // Séparateur discret entre les deux volets.
                        Box(
                            Modifier.width(1.dp).fillMaxHeight()
                                .background(Color.White.copy(alpha = 0.12f))
                        )
                        Box(Modifier.weight(0.62f).fillMaxHeight()) {
                            CarteScreen(vm, onLocate, showDeviceList = false)
                        }
                    }

                    else -> EcranSimple(tab, vm, onLocate)
                }
            }
        }
    }
}

@Composable
private fun EcranSimple(tab: Int, vm: RadarViewModel, onLocate: (Appareil) -> Unit) {
    when (tab) {
        ONGLET_CARTE -> CarteScreen(vm, onLocate)
        ONGLET_APPAREILS -> AppareilsScreen(vm, onLocate)
        else -> SettingsScreen(vm)
    }
}

/** Les réglages sont des lignes de texte : inutile de les étirer sur 1280 dp. */
@Composable
private fun PanneauCentre(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Box(Modifier.widthIn(max = 720.dp).fillMaxWidth().fillMaxHeight()) {
            content()
        }
    }
}

@Composable
private fun RowScope.NavItem(
    selected: Boolean,
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = { Icon(icon, contentDescription = label) },
        label = { Text(label) },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = Color.Black,
            selectedTextColor = Neon,
            indicatorColor = Neon,
            unselectedIconColor = Color.White.copy(alpha = 0.6f),
            unselectedTextColor = Color.White.copy(alpha = 0.6f)
        )
    )
}

@Composable
private fun RailItem(
    selected: Boolean,
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    NavigationRailItem(
        selected = selected,
        onClick = onClick,
        icon = { Icon(icon, contentDescription = label) },
        label = { Text(label) },
        colors = NavigationRailItemDefaults.colors(
            selectedIconColor = Color.Black,
            selectedTextColor = Neon,
            indicatorColor = Neon,
            unselectedIconColor = Color.White.copy(alpha = 0.6f),
            unselectedTextColor = Color.White.copy(alpha = 0.6f)
        )
    )
}
