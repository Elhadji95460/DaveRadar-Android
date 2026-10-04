package com.daveradar.app.model

import kotlin.math.pow

/** Catégorie d'appareil, avec libellé français. */
enum class DeviceKind(val label: String) {
    PHONE("Téléphone"),
    LAPTOP("Ordinateur"),
    TABLET("Tablette"),
    WATCH("Montre"),
    EARBUDS("Écouteurs"),
    SPEAKER("Enceinte"),
    TV("TV / Cast"),
    ACCESSORY("Accessoire"),
    UNKNOWN("Inconnu")
}

/**
 * Un appareil détecté et fusionné.
 *
 * `stableId` est l'identifiant persistant (adresse BLE quand elle est stable, sinon
 * signature dérivée du nom + fabricant) qui permet de retrouver le nom personnalisé.
 */
data class Appareil(
    val stableId: String,
    val nomAuto: String,
    val nomPersonnalise: String? = null,
    val vendor: String? = null,
    val kind: DeviceKind = DeviceKind.UNKNOWN,
    val rssi: Int = 0,
    val lastSeen: Long = 0L,
    val supportsUwb: Boolean = false,
    val sources: Set<String> = emptySet()
) {
    /** Nom affiché : le personnalisé s'il existe, sinon le nom détecté automatiquement. */
    val displayName: String
        get() = nomPersonnalise?.takeIf { it.isNotBlank() } ?: nomAuto

    /** Distance estimée en mètres (modèle log-distance sur le RSSI). -1 si inconnue. */
    val distance: Double
        get() {
            if (rssi == 0) return -1.0
            val txPower = -59.0        // RSSI de référence à 1 m
            val n = 2.5                // facteur d'environnement (intérieur)
            return 10.0.pow((txPower - rssi) / (10.0 * n))
        }
}
