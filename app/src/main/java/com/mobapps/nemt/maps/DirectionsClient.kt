package com.mobapps.nemt.maps

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import java.util.Locale

/**
 * Local route estimator used by the trip UI.
 *
 * This intentionally avoids external Directions API calls and produces a direct
 * pickup-to-destination polyline with rough distance/time estimates.
 */
object DirectionsClient {
    fun straightLineFallback(origin: LatLng, destination: LatLng): TripDirections {
        val meters = SphericalUtil.computeDistanceBetween(origin, destination)
        val distanceM = meters.toInt().coerceAtLeast(1)
        val km = meters / 1000.0
        val distanceText = String.format(Locale.US, "%.1f km", km)
        val avgMetersPerSec = 8.0
        val durationSec = (meters / avgMetersPerSec).toInt().coerceAtLeast(60)
        val minutes = (durationSec + 59) / 60
        return TripDirections(
            polylinePoints = listOf(origin, destination),
            durationSeconds = durationSec,
            durationText = "~$minutes min",
            distanceMeters = distanceM,
            distanceText = distanceText,
            nextStepHint = null,
            turns = emptyList()
        )
    }
}
