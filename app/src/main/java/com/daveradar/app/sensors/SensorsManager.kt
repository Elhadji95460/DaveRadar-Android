package com.daveradar.app.sensors

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat

/**
 * Fournit le cap boussole (en degrés, 0 = Nord) et la position GPS de l'utilisateur.
 * Le cap sert à orienter la flèche « Localiser » ; la position sert à placer les
 * appareils autour de l'utilisateur sur la carte.
 *
 * Les deux fournisseurs sont écoutés : le réseau donne un premier point en quelques
 * secondes, le GPS prend plus de temps mais descend à quelques mètres. Toute position
 * reçue passe par [estMeilleure] avant d'être retenue, sinon le réseau — bien plus
 * bavard — écraserait en permanence les points GPS et la position affichée resterait
 * à plusieurs centaines de mètres de la réalité.
 */
class SensorsManager(private val context: Context) {

    var onHeading: ((Float) -> Unit)? = null
    var onLocation: ((Location) -> Unit)? = null

    var lastLocation: Location? = null
        private set
    var heading: Float = 0f
        private set

    /** Vrai dès qu'un point GPS récent est retenu (sert à l'affichage de l'état). */
    val surGps: Boolean
        get() = lastLocation?.provider == LocationManager.GPS_PROVIDER

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationSensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private val rotationMatrix = FloatArray(9)
    private val orientation = FloatArray(3)

    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type != Sensor.TYPE_ROTATION_VECTOR) return
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            SensorManager.getOrientation(rotationMatrix, orientation)
            var azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
            if (azimuth < 0f) azimuth += 360f
            heading = azimuth
            onHeading?.invoke(azimuth)
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    private val locationListener = LocationListener { location ->
        if (estMeilleure(location, lastLocation)) {
            lastLocation = location
            onLocation?.invoke(location)
        }
    }

    fun start() {
        rotationSensor?.let {
            sensorManager.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_UI)
        }
        startLocation()
    }

    fun stop() {
        sensorManager.unregisterListener(sensorListener)
        try {
            locationManager.removeUpdates(locationListener)
        } catch (_: Exception) {
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocation() {
        if (!hasLocationPermission(context)) return
        // GPS demandé en premier et plus souvent : c'est lui qu'on veut voir gagner.
        val demandes = listOf(
            LocationManager.GPS_PROVIDER to 1000L,
            LocationManager.NETWORK_PROVIDER to 2000L
        )
        try {
            for ((provider, intervalle) in demandes) {
                if (!locationManager.isProviderEnabled(provider)) continue
                locationManager.requestLocationUpdates(
                    provider, intervalle, 1f, locationListener, Looper.getMainLooper()
                )
                locationManager.getLastKnownLocation(provider)?.let { connue ->
                    if (estMeilleure(connue, lastLocation)) {
                        lastLocation = connue
                        onLocation?.invoke(connue)
                    }
                }
            }
        } catch (_: SecurityException) {
        } catch (_: IllegalArgumentException) {
        }
    }

    /**
     * Décide si [candidate] doit remplacer [actuelle].
     * Règle principale : à fraîcheur comparable, le GPS l'emporte toujours sur le réseau.
     */
    private fun estMeilleure(candidate: Location, actuelle: Location?): Boolean {
        if (actuelle == null) return true

        val ecartMs = candidate.time - actuelle.time
        // Le candidat est bien plus vieux que ce qu'on a déjà : sans intérêt.
        if (ecartMs < -PEREMPTION_MS) return false

        val candidatGps = candidate.provider == LocationManager.GPS_PROVIDER
        val actuelleGps = actuelle.provider == LocationManager.GPS_PROVIDER

        // La préférence GPS passe AVANT toute règle de fraîcheur : un point réseau
        // à peine plus récent ne doit jamais chasser un point GPS, sans quoi on
        // remplace 3 m de précision par 100 m.
        if (candidatGps && !actuelleGps) return true
        if (!candidatGps && actuelleGps) {
            // Repli sur le réseau uniquement si le point GPS est franchement périmé.
            return ecartMs > PEREMPTION_GPS_MS
        }

        // Même source : on tranche à la précision, en tolérant un léger recul
        // pour ne pas figer la position sur un vieux point exceptionnellement bon.
        val ecartPrecision = candidate.accuracy - actuelle.accuracy
        return when {
            ecartPrecision < 0f -> true
            ecartPrecision > TOLERANCE_PRECISION_M -> false
            else -> ecartMs > 0
        }
    }

    companion object {
        /** Au-delà, un candidat plus ancien que la position retenue est ignoré. */
        private const val PEREMPTION_MS = 20_000L

        /** Durée après laquelle un point GPS est jugé périmé et le réseau reprend la main. */
        private const val PEREMPTION_GPS_MS = 60_000L

        /** Recul de précision toléré entre deux points de même source. */
        private const val TOLERANCE_PRECISION_M = 50f

        fun hasLocationPermission(context: Context): Boolean =
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
    }
}
