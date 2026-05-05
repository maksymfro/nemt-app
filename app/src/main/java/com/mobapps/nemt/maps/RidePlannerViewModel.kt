package com.mobapps.nemt.maps

import android.app.Application
import android.location.Geocoder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.firebase.auth.FirebaseAuth
import com.mobapps.nemt.BuildConfig
import com.mobapps.nemt.data.TripsFirestoreRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

data class RideStop(
    val placeId: String?,
    val title: String,
    val subtitle: String,
    val latLng: LatLng
)

class RidePlannerViewModel(application: Application) : AndroidViewModel(application) {

    private val placesClient: PlacesClient? =
        if (BuildConfig.MAPS_API_KEY.isNotBlank()) Places.createClient(application) else null

    private val fused: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(application)

    private var pickupSessionToken: AutocompleteSessionToken = AutocompleteSessionToken.newInstance()
    private var destSessionToken: AutocompleteSessionToken = AutocompleteSessionToken.newInstance()

    private val _deviceLocation = MutableStateFlow<LatLng?>(null)
    val deviceLocation: StateFlow<LatLng?> = _deviceLocation.asStateFlow()

    private val _pickupQuery = MutableStateFlow("")
    val pickupQuery: StateFlow<String> = _pickupQuery.asStateFlow()

    private val _destinationQuery = MutableStateFlow("")
    val destinationQuery: StateFlow<String> = _destinationQuery.asStateFlow()

    private val _pickupPredictions = MutableStateFlow<List<AutocompletePrediction>>(emptyList())
    val pickupPredictions: StateFlow<List<AutocompletePrediction>> = _pickupPredictions.asStateFlow()

    private val _destinationPredictions = MutableStateFlow<List<AutocompletePrediction>>(emptyList())
    val destinationPredictions: StateFlow<List<AutocompletePrediction>> =
        _destinationPredictions.asStateFlow()

    private val _pickupStop = MutableStateFlow<RideStop?>(null)
    val pickupStop: StateFlow<RideStop?> = _pickupStop.asStateFlow()

    private val _destinationStop = MutableStateFlow<RideStop?>(null)
    val destinationStop: StateFlow<RideStop?> = _destinationStop.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    /** Route polyline between pickup and destination. Null if either stop is missing. */
    private val _routePoints = MutableStateFlow<List<LatLng>?>(null)
    val routePoints: StateFlow<List<LatLng>?> = _routePoints.asStateFlow()

    /** ETA and distance estimate for the current planned trip. */
    private val _tripDirections = MutableStateFlow<TripDirections?>(null)
    val tripDirections: StateFlow<TripDirections?> = _tripDirections.asStateFlow()

    private val _selectedVehicle = MutableStateFlow<String?>(null)
    val selectedVehicle: StateFlow<String?> = _selectedVehicle.asStateFlow()

    private val _scheduledAtMillis = MutableStateFlow(0L)
    val scheduledAtMillis: StateFlow<Long> = _scheduledAtMillis.asStateFlow()

    private var pickupSearchJob: Job? = null
    private var destSearchJob: Job? = null
    private var routeJob: Job? = null

    fun clearMessage() {
        _userMessage.value = null
    }

    fun onPickupQueryChange(value: String) {
        _pickupQuery.value = value
        schedulePickupSearch(value)
    }

    fun onDestinationQueryChange(value: String) {
        _destinationQuery.value = value
        scheduleDestinationSearch(value)
    }

    fun refreshDeviceLocation() {
        fused.getCurrentLocation(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            CancellationTokenSource().token
        ).addOnSuccessListener { loc ->
            if (loc != null) {
                _deviceLocation.value = LatLng(loc.latitude, loc.longitude)
            }
        }.addOnFailureListener {
            _userMessage.value = "Could not read your location. Check location settings."
        }
    }

    fun useDeviceLocationForPickup() {
        val client = placesClient
        if (client == null) {
            _userMessage.value = "Add MAPS_API_KEY in local.properties to use maps and search."
            return
        }
        fused.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            CancellationTokenSource().token
        ).addOnSuccessListener { loc ->
            if (loc == null) {
                _userMessage.value = "Location unavailable. Try again or enter pickup manually."
                return@addOnSuccessListener
            }
            val latLng = LatLng(loc.latitude, loc.longitude)
            _deviceLocation.value = latLng
            viewModelScope.launch {
                val label = withContext(Dispatchers.IO) { reverseGeocodeLabel(latLng) }
                _pickupStop.value = RideStop(
                    placeId = null,
                    title = label.first,
                    subtitle = label.second,
                    latLng = latLng
                )
                _pickupQuery.value = label.first
                _pickupPredictions.value = emptyList()
                pickupSessionToken = AutocompleteSessionToken.newInstance()
                refreshRoutePolyline()
            }
        }.addOnFailureListener {
            _userMessage.value = "Could not use current location for pickup."
        }
    }

    fun selectPickupPrediction(prediction: AutocompletePrediction) {
        fetchPlace(prediction) { stop ->
            _pickupStop.value = stop
            _pickupQuery.value = stop.title
            _pickupPredictions.value = emptyList()
            pickupSessionToken = AutocompleteSessionToken.newInstance()
            refreshRoutePolyline()
        }
    }

    fun selectDestinationPrediction(prediction: AutocompletePrediction) {
        fetchPlace(prediction) { stop ->
            _destinationStop.value = stop
            _destinationQuery.value = stop.title
            _destinationPredictions.value = emptyList()
            destSessionToken = AutocompleteSessionToken.newInstance()
            refreshRoutePolyline()
        }
    }

    fun clearPickupSelection() {
        _pickupStop.value = null
        refreshRoutePolyline()
    }

    fun clearDestinationSelection() {
        _destinationStop.value = null
        refreshRoutePolyline()
    }

    fun setVehicleChoice(vehicle: String) {
        _selectedVehicle.value = vehicle
    }

    fun setScheduledAtMillis(millis: Long) {
        _scheduledAtMillis.value = millis
    }

    private suspend fun loadRouteFromCurrentStops() {
        val p = _pickupStop.value?.latLng ?: return
        val d = _destinationStop.value?.latLng ?: return
        val estimate = DirectionsClient.straightLineFallback(p, d)
        _routePoints.value = estimate.polylinePoints
        _tripDirections.value = estimate
    }

    private fun refreshRoutePolyline() {
        routeJob?.cancel()
        val p = _pickupStop.value?.latLng
        val d = _destinationStop.value?.latLng
        if (p == null || d == null) {
            _routePoints.value = null
            _tripDirections.value = null
            return
        }
        routeJob = viewModelScope.launch {
            loadRouteFromCurrentStops()
        }
    }

    /** Clears booking UI state after a trip is finished or cancelled. */
    fun clearTripPlanningSession() {
        routeJob?.cancel()
        _pickupStop.value = null
        _destinationStop.value = null
        _pickupQuery.value = ""
        _destinationQuery.value = ""
        _pickupPredictions.value = emptyList()
        _destinationPredictions.value = emptyList()
        _routePoints.value = null
        _tripDirections.value = null
        _selectedVehicle.value = null
        _scheduledAtMillis.value = 0L
        pickupSessionToken = AutocompleteSessionToken.newInstance()
        destSessionToken = AutocompleteSessionToken.newInstance()
    }

    /**
     * Ensures pickup and destination have coordinates (uses existing selections or Geocoder on typed text).
     * @return null on success, or a short error message.
     */
    suspend fun tryFinalizeBooking(): String? {
        if (FirebaseAuth.getInstance().currentUser == null) {
            return "Please sign in to book a ride."
        }
        if (_selectedVehicle.value.isNullOrBlank()) {
            return "Select a vehicle type."
        }
        val sched = _scheduledAtMillis.value
        if (sched == 0L) {
            return "Choose a date and time for your ride."
        }
        val now = System.currentTimeMillis()
        if (sched < now - 60_000L) {
            return "Pick a time in the future (at least 1 minute from now)."
        }
        val pq = _pickupQuery.value.trim()
        val dq = _destinationQuery.value.trim()
        if (pq.isEmpty() || dq.isEmpty()) {
            return "Enter pickup and destination."
        }
        var p = _pickupStop.value
        var d = _destinationStop.value
        if (p == null) {
            p = withContext(Dispatchers.IO) { geocodeQueryToStop(pq) }
                ?: return "Could not find pickup. Add detail or pick from suggestions."
            _pickupStop.value = p
        }
        if (d == null) {
            d = withContext(Dispatchers.IO) { geocodeQueryToStop(dq) }
                ?: return "Could not find destination. Add detail or pick from suggestions."
            _destinationStop.value = d
        }
        if (TripsFirestoreRepository.areStopsEffectivelyDuplicate(
                pickupTitle = p.title,
                pickupSubtitle = p.subtitle,
                pickupLatLng = p.latLng,
                destTitle = d.title,
                destSubtitle = d.subtitle,
                destLatLng = d.latLng
            )
        ) {
            return "Pickup and destination must be different places."
        }
        loadRouteFromCurrentStops()
        return null
    }

    private fun geocodeQueryToStop(query: String): RideStop? {
        if (!Geocoder.isPresent()) return null
        val geocoder = Geocoder(getApplication(), Locale.getDefault())
        return try {
            @Suppress("DEPRECATION")
            val list = geocoder.getFromLocationName(query, 5) ?: return null
            val a = list.firstOrNull() ?: return null
            val line0 = a.getAddressLine(0).orEmpty()
            val title = a.featureName?.takeIf { it.isNotBlank() }
                ?: a.thoroughfare?.takeIf { it.isNotBlank() }
                ?: query
            RideStop(
                placeId = null,
                title = title,
                subtitle = line0,
                latLng = LatLng(a.latitude, a.longitude)
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun schedulePickupSearch(raw: String) {
        pickupSearchJob?.cancel()
        if (raw.length < 2) {
            _pickupPredictions.value = emptyList()
            return
        }
        pickupSearchJob = viewModelScope.launch {
            delay(280)
            runAutocomplete(raw, pickupSessionToken) { _pickupPredictions.value = it }
        }
    }

    private fun scheduleDestinationSearch(raw: String) {
        destSearchJob?.cancel()
        if (raw.length < 2) {
            _destinationPredictions.value = emptyList()
            return
        }
        destSearchJob = viewModelScope.launch {
            delay(280)
            runAutocomplete(raw, destSessionToken) { _destinationPredictions.value = it }
        }
    }

    private fun runAutocomplete(
        query: String,
        token: AutocompleteSessionToken,
        onResult: (List<AutocompletePrediction>) -> Unit
    ) {
        val client = placesClient
        if (client == null) {
            onResult(emptyList())
            return
        }
        val request = FindAutocompletePredictionsRequest.builder()
            .setSessionToken(token)
            .setQuery(query)
            .build()
        client.findAutocompletePredictions(request)
            .addOnSuccessListener { response ->
                onResult(response.autocompletePredictions)
            }
            .addOnFailureListener {
                onResult(emptyList())
            }
    }

    private fun fetchPlace(prediction: AutocompletePrediction, onReady: (RideStop) -> Unit) {
        val client = placesClient ?: return
        val fields = listOf(
            Place.Field.ID,
            Place.Field.NAME,
            Place.Field.ADDRESS,
            Place.Field.LAT_LNG
        )
        val request = FetchPlaceRequest.builder(prediction.placeId, fields).build()
        client.fetchPlace(request)
            .addOnSuccessListener { response ->
                val place = response.place
                val loc = place.latLng
                if (loc == null) {
                    _userMessage.value = "This place has no map location. Try another result."
                    return@addOnSuccessListener
                }
                val title = place.name?.takeIf { it.isNotBlank() }
                    ?: prediction.getPrimaryText(null).toString()
                val subtitle = place.address
                    ?: prediction.getSecondaryText(null)?.toString().orEmpty()
                onReady(
                    RideStop(
                        placeId = place.id,
                        title = title,
                        subtitle = subtitle,
                        latLng = loc
                    )
                )
            }
            .addOnFailureListener {
                _userMessage.value = "Could not load place details."
            }
    }

    private fun reverseGeocodeLabel(latLng: LatLng): Pair<String, String> {
        val geocoder = Geocoder(getApplication(), Locale.getDefault())
        if (!Geocoder.isPresent()) {
            return "Current location" to "${latLng.latitude.format6()}, ${latLng.longitude.format6()}"
        }
        return try {
            @Suppress("DEPRECATION")
            val list = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            val a = list?.firstOrNull()
            if (a == null) {
                "Current location" to "${latLng.latitude.format6()}, ${latLng.longitude.format6()}"
            } else {
                val title = a.featureName?.takeIf { it.isNotBlank() }
                    ?: a.thoroughfare?.takeIf { it.isNotBlank() }
                    ?: "Current location"
                val subtitle = buildList {
                    a.getAddressLine(0)?.let { add(it) }
                }.firstOrNull().orEmpty()
                if (subtitle.isBlank()) title to "" else title to subtitle
            }
        } catch (_: Exception) {
            "Current location" to "${latLng.latitude.format6()}, ${latLng.longitude.format6()}"
        }
    }

    private fun Double.format6(): String = String.format(Locale.US, "%.6f", this)
}
