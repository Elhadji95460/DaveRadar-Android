package com.daveradar.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

/**
 * Classes de largeur d'écran, d'après les seuils Material 3.
 *
 * Calculé depuis LocalConfiguration plutôt qu'avec la dépendance
 * material3-window-size-class : la configuration suit déjà les rotations
 * et le mode multi-fenêtre, et cela évite d'ajouter une bibliothèque.
 */
enum class LargeurEcran { COMPACTE, MOYENNE, ETENDUE }

@Composable
fun largeurEcran(): LargeurEcran {
    val largeurDp = LocalConfiguration.current.screenWidthDp
    return when {
        largeurDp < 600 -> LargeurEcran.COMPACTE   // téléphone
        largeurDp < 840 -> LargeurEcran.MOYENNE    // tablette en portrait
        else -> LargeurEcran.ETENDUE               // tablette en paysage
    }
}
