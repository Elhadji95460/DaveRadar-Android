package com.daveradar.app.data

import android.content.Context

/**
 * Stockage persistant des noms personnalisés, indexé par l'identifiant stable de
 * l'appareil. Ainsi, si le même appareil est redétecté plus tard, son nom
 * personnalisé réapparaît automatiquement.
 */
class NameStore(context: Context) {

    private val prefs = context.getSharedPreferences("dave_radar_names", Context.MODE_PRIVATE)

    fun nameFor(stableId: String): String? = prefs.getString(stableId, null)

    fun setName(stableId: String, name: String) {
        prefs.edit().putString(stableId, name.trim()).apply()
    }

    fun forget(stableId: String) {
        prefs.edit().remove(stableId).apply()
    }

    /** Toutes les paires (identifiant → nom personnalisé) connues. */
    fun all(): Map<String, String> {
        @Suppress("UNCHECKED_CAST")
        return prefs.all.filterValues { it is String } as Map<String, String>
    }
}
