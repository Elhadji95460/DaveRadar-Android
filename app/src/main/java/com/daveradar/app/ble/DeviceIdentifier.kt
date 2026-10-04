package com.daveradar.app.ble

import com.daveradar.app.model.DeviceKind

/**
 * Identification d'un appareil BLE à partir de son nom annoncé et de son
 * « company ID » (données fabricant Bluetooth SIG).
 *
 * Effort particulier sur l'écosystème Android : Samsung Galaxy, Google Pixel,
 * Xiaomi/Redmi/POCO, Huawei/Honor, OPPO/OnePlus/Realme, vivo…
 */
object DeviceIdentifier {

    data class Result(
        val nomAuto: String,
        val vendor: String?,
        val kind: DeviceKind
    )

    /** Company IDs Bluetooth SIG les plus courants. */
    private val companyNames: Map<Int, String> = mapOf(
        0x004C to "Apple",
        0x0075 to "Samsung",
        0x00E0 to "Google",
        0x0006 to "Microsoft",
        0x038F to "Xiaomi",
        0x027D to "Huawei",
        0x0157 to "Huami (Amazfit)",
        0x012D to "Sony",
        0x009E to "Bose",
        0x00C4 to "LG",
        0x0059 to "Nordic",
        0x000F to "Broadcom",
        0x0499 to "Ruuvi",
        0x0171 to "Garmin",
        0x022B to "Tile",
        0x0087 to "Garmin Intl",
        0x00D2 to "Dialog",
        0x05A7 to "Sonos",
        0x0201 to "JBL / Harman"
    )

    /**
     * Heuristiques par nom : (motif en minuscule) -> (vendor, type).
     * L'ordre compte : les motifs les plus spécifiques d'abord.
     */
    private data class NameRule(val needles: List<String>, val vendor: String?, val kind: DeviceKind)

    private val nameRules: List<NameRule> = listOf(
        // --- Android : téléphones ---
        NameRule(listOf("galaxy s", "galaxy z", "galaxy a", "galaxy note", "galaxy m", "sm-"), "Samsung", DeviceKind.PHONE),
        NameRule(listOf("galaxy tab"), "Samsung", DeviceKind.TABLET),
        NameRule(listOf("galaxy watch", "galaxy fit"), "Samsung", DeviceKind.WATCH),
        NameRule(listOf("galaxy buds", "buds"), "Samsung", DeviceKind.EARBUDS),
        NameRule(listOf("galaxy"), "Samsung", DeviceKind.PHONE),
        NameRule(listOf("pixel buds"), "Google", DeviceKind.EARBUDS),
        NameRule(listOf("pixel watch"), "Google", DeviceKind.WATCH),
        NameRule(listOf("pixel"), "Google", DeviceKind.PHONE),
        NameRule(listOf("redmi", "poco", "mi band", "xiaomi", "mi watch", "mi tv", "mi "), "Xiaomi", DeviceKind.PHONE),
        NameRule(listOf("oneplus", "op "), "OnePlus", DeviceKind.PHONE),
        NameRule(listOf("realme"), "Realme", DeviceKind.PHONE),
        NameRule(listOf("oppo"), "OPPO", DeviceKind.PHONE),
        NameRule(listOf("vivo", "iqoo"), "vivo", DeviceKind.PHONE),
        NameRule(listOf("huawei", "honor", "mate", "p30", "p40", "nova"), "Huawei", DeviceKind.PHONE),
        NameRule(listOf("motorola", "moto "), "Motorola", DeviceKind.PHONE),
        NameRule(listOf("nokia"), "Nokia", DeviceKind.PHONE),
        NameRule(listOf("nothing"), "Nothing", DeviceKind.PHONE),
        // --- Apple ---
        NameRule(listOf("airpods"), "Apple", DeviceKind.EARBUDS),
        NameRule(listOf("apple watch"), "Apple", DeviceKind.WATCH),
        NameRule(listOf("macbook", "imac", "mac mini", "mac studio"), "Apple", DeviceKind.LAPTOP),
        NameRule(listOf("ipad"), "Apple", DeviceKind.TABLET),
        NameRule(listOf("iphone"), "Apple", DeviceKind.PHONE),
        NameRule(listOf("beats"), "Apple", DeviceKind.EARBUDS),
        // --- Audio / TV ---
        NameRule(listOf("jbl"), "JBL", DeviceKind.SPEAKER),
        NameRule(listOf("bose"), "Bose", DeviceKind.SPEAKER),
        NameRule(listOf("sony", "wh-", "wf-"), "Sony", DeviceKind.EARBUDS),
        NameRule(listOf("sonos"), "Sonos", DeviceKind.SPEAKER),
        NameRule(listOf("chromecast", "google tv", "android tv", "bravia", "shield", "tv"), null, DeviceKind.TV),
        NameRule(listOf("amazfit", "huawei band", "mi band", "band "), null, DeviceKind.WATCH),
        NameRule(listOf("watch"), null, DeviceKind.WATCH),
        NameRule(listOf("buds", "headset", "earbuds", "écouteurs"), null, DeviceKind.EARBUDS),
        NameRule(listOf("speaker", "enceinte", "soundbar"), null, DeviceKind.SPEAKER),
        NameRule(listOf("tile", "airtag"), null, DeviceKind.ACCESSORY)
    )

    /** Identifie un appareil Bluetooth. `name` peut être nul (appareil anonyme). */
    fun identify(name: String?, companyId: Int?): Result {
        val vendorFromCompany = companyId?.let { companyNames[it] }
        val lower = name?.trim()?.lowercase().orEmpty()

        // 1) Règle par nom (la plus fiable quand un nom est annoncé).
        if (lower.isNotEmpty()) {
            for (rule in nameRules) {
                if (rule.needles.any { lower.contains(it) }) {
                    val vendor = rule.vendor ?: vendorFromCompany
                    val display = name!!.trim()
                    return Result(display, vendor, rule.kind)
                }
            }
        }

        // 2) Pas de correspondance de nom : on s'appuie sur le fabricant.
        val vendor = vendorFromCompany
        val kind = DeviceKind.UNKNOWN
        val nomAuto = when {
            !name.isNullOrBlank() -> name.trim()
            vendor != null -> "Appareil $vendor"
            else -> "Appareil inconnu"
        }
        return Result(nomAuto, vendor, kind)
    }
}
