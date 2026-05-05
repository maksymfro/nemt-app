package com.mobapps.nemt.maps

import com.google.android.gms.maps.model.LatLng

data class TripTurn(
    val position: LatLng,
    val maneuver: String?,
    val instruction: String
)

/**
 * Route details shown in the trip overview.
 */
data class TripDirections(
    val polylinePoints: List<LatLng>,
    val durationSeconds: Int,
    val durationText: String,
    val distanceMeters: Int,
    val distanceText: String,
    /** Optional next step hint text. */
    val nextStepHint: String?,
    /** Optional maneuver points to display on the map. */
    val turns: List<TripTurn>
)
