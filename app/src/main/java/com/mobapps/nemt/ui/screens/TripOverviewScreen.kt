package com.mobapps.nemt.ui.screens

import android.Manifest
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.AltRoute
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Dash
import com.google.android.gms.maps.model.Gap
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.PatternItem
import com.google.android.gms.maps.model.RoundCap
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.mobapps.nemt.BuildConfig
import com.mobapps.nemt.R
import com.mobapps.nemt.data.TripsRepository
import com.mobapps.nemt.notifications.NemtNotificationType
import com.mobapps.nemt.notifications.NemtNotifications
import com.mobapps.nemt.ui.rememberRidePlannerViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripOverviewScreen(
    onClose: () -> Unit,
    contentPadding: PaddingValues
) {
    val ridePlanner = rememberRidePlannerViewModel()
    val pickupStop by ridePlanner.pickupStop.collectAsState()
    val destinationStop by ridePlanner.destinationStop.collectAsState()
    val routePoints by ridePlanner.routePoints.collectAsState()
    val tripDirections by ridePlanner.tripDirections.collectAsState()
    val deviceLocation by ridePlanner.deviceLocation.collectAsState()
    val selectedVehicle by ridePlanner.selectedVehicle.collectAsState()
    val scheduledAtMillis by ridePlanner.scheduledAtMillis.collectAsState()

    val context = LocalContext.current
    val hasMapsKey = BuildConfig.MAPS_API_KEY.isNotBlank()
    val scope = rememberCoroutineScope()
    val pickupDotIcon = remember { createBlueDotMarkerDescriptor() }

    var locationGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        locationGranted = granted[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            granted[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (locationGranted) {
            ridePlanner.refreshDeviceLocation()
        }
    }

    LaunchedEffect(Unit) {
        ridePlanner.refreshDeviceLocation()
        if (!locationGranted && hasMapsKey) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    fun finishTrip() {
        ridePlanner.clearTripPlanningSession()
        onClose()
    }

    val pickup = pickupStop?.latLng
    val destination = destinationStop?.latLng
    var showDetailsSheet by remember { mutableStateOf(true) }
    var showTripConfirmedDialog by remember { mutableStateOf(false) }
    var confirmedFrom by remember { mutableStateOf("") }
    var confirmedTo by remember { mutableStateOf("") }
    var confirmedDateTime by remember { mutableStateOf("") }
    var confirmedVehicle by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            pickup ?: deviceLocation ?: destination ?: LatLng(25.7617, -80.1918),
            12f
        )
    }

    LaunchedEffect(routePoints, pickup, destination) {
        val pts = routePoints ?: return@LaunchedEffect
        if (pts.size < 2) return@LaunchedEffect
        val builder = LatLngBounds.Builder()
        pts.forEach { builder.include(it) }
        runCatching {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngBounds(builder.build(), 100),
                durationMs = 500
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        if (hasMapsKey && pickup != null && destination != null) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    isMyLocationEnabled = locationGranted
                ),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    compassEnabled = true,
                    myLocationButtonEnabled = false,
                    mapToolbarEnabled = false
                )
            ) {
                Marker(
                    state = MarkerState(position = pickup),
                    title = context.getString(R.string.trip_marker_pickup),
                    snippet = pickupStop?.title,
                    icon = pickupDotIcon
                )
                Marker(
                    state = MarkerState(position = destination),
                    title = context.getString(R.string.trip_marker_destination),
                    snippet = destinationStop?.title
                )
                routePoints?.takeIf { it.size >= 2 }?.let { pts ->
                    Polyline(
                        points = pts,
                        color = Color(0xB2151A2E),
                        width = 18f,
                        geodesic = false,
                        jointType = JointType.ROUND,
                        startCap = RoundCap(),
                        endCap = RoundCap()
                    )
                    Polyline(
                        points = pts,
                        color = Color(0xFF2D8CFF),
                        width = 11f,
                        geodesic = false,
                        jointType = JointType.ROUND,
                        startCap = RoundCap(),
                        endCap = RoundCap()
                    )
                    Polyline(
                        points = pts,
                        color = Color(0xE6FFFFFF),
                        width = 3.5f,
                        geodesic = false,
                        jointType = JointType.ROUND,
                        startCap = RoundCap(),
                        endCap = RoundCap(),
                        pattern = listOf<PatternItem>(Dash(22f), Gap(16f))
                    )
                }
                tripDirections?.turns?.take(6)?.forEach { turn ->
                    Marker(
                        state = MarkerState(position = turn.position),
                        title = turn.maneuver ?: "Turn",
                        snippet = turn.instruction,
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = context.getString(R.string.trip_map_unavailable),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        if (hasMapsKey && pickup != null && destination != null) {
            MapScaleIndicator(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 84.dp, bottom = 20.dp),
                cameraPosition = cameraPositionState.position
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xCC101820),
                            Color.Transparent
                        )
                    )
                )
                .statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = {
                    ridePlanner.clearTripPlanningSession()
                    onClose()
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = context.getString(R.string.cd_back),
                        tint = Color.White
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = context.getString(R.string.trip_title),
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = context.getString(R.string.trip_subtitle),
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 74.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FloatingActionButton(
                onClick = {
                    val nextZoom = (cameraPositionState.position.zoom + 1f).coerceAtMost(21f)
                    scope.launch {
                        runCatching {
                            cameraPositionState.animate(
                                CameraUpdateFactory.zoomTo(nextZoom),
                                durationMs = 220
                            )
                        }
                    }
                },
                modifier = Modifier.size(46.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = "Zoom in"
                )
            }

            FloatingActionButton(
                onClick = {
                    val nextZoom = (cameraPositionState.position.zoom - 1f).coerceAtLeast(3f)
                    scope.launch {
                        runCatching {
                            cameraPositionState.animate(
                                CameraUpdateFactory.zoomTo(nextZoom),
                                durationMs = 220
                            )
                        }
                    }
                },
                modifier = Modifier.size(46.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Remove,
                    contentDescription = "Zoom out"
                )
            }
        }

        if (!showDetailsSheet) {
            FloatingActionButton(
                onClick = { showDetailsSheet = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Navigation,
                    contentDescription = "Show trip details"
                )
            }
        }

        if (locationGranted) {
            FloatingActionButton(
                onClick = { deviceLocation?.let { loc ->
                    scope.launch {
                        runCatching {
                            cameraPositionState.animate(
                                CameraUpdateFactory.newLatLngZoom(loc, 15f),
                                durationMs = 450
                            )
                        }
                    }
                } },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.MyLocation,
                    contentDescription = "Center on origin"
                )
            }
        }
    }

    if (showDetailsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showDetailsSheet = false },
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            dragHandle = {}
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (tripDirections == null && routePoints == null) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(36.dp))
                    }
                }

                TransportPreviewCard(
                    selectedVehicle = selectedVehicle,
                    etaText = tripDirections?.durationText,
                    tripDetailsText = buildString {
                        pickupStop?.title?.takeIf { it.isNotBlank() }?.let { append(it) }
                        destinationStop?.title?.takeIf { it.isNotBlank() }?.let {
                            if (isNotEmpty()) append(" -> ")
                            append(it)
                        }
                    }.ifBlank { "Trip details pending" }
                )

                tripDirections?.let { dir ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        TripStatChip(
                            icon = Icons.Outlined.AccessTime,
                            label = context.getString(R.string.trip_eta_label),
                            value = dir.durationText
                        )
                        TripStatChip(
                            icon = Icons.Outlined.AltRoute,
                            label = context.getString(R.string.trip_distance_label),
                            value = dir.distanceText
                        )
                    }
                    dir.nextStepHint?.takeIf { it.isNotBlank() }?.let { hint ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Navigation,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.size(10.dp))
                                Column {
                                    Text(
                                        text = context.getString(R.string.trip_next_step),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = hint,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                TripAddressRow(
                    icon = Icons.Outlined.MyLocation,
                    label = context.getString(R.string.trip_pickup_label),
                    title = pickupStop?.title.orEmpty(),
                    subtitle = pickupStop?.subtitle.orEmpty()
                )
                TripAddressRow(
                    icon = Icons.Outlined.Flag,
                    label = context.getString(R.string.trip_destination_label),
                    title = destinationStop?.title.orEmpty(),
                    subtitle = destinationStop?.subtitle.orEmpty()
                )

                if (deviceLocation != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Place,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = context.getString(R.string.trip_you_are_here),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            ridePlanner.clearTripPlanningSession()
                            onClose()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = context.getString(R.string.trip_cancel),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    Button(
                        onClick = {
                            val fromLabel = pickupStop?.title?.takeIf { it.isNotBlank() } ?: "Pickup location"
                            val toLabel = destinationStop?.title?.takeIf { it.isNotBlank() } ?: "Destination"
                            val dateTimeLabel = SimpleDateFormat(
                                "EEE, MMM d, yyyy • h:mm a",
                                Locale.getDefault()
                            ).format(Date(scheduledAtMillis.takeIf { it > 0L } ?: System.currentTimeMillis()))
                            val vehicleLabel = selectedVehicle ?: "Unit A12 · Wheelchair Van"
                            TripsRepository.addConfirmedUpcomingTrip(
                                from = fromLabel,
                                to = toLabel,
                                patientName = "For: You",
                                vehicle = vehicleLabel,
                                dateTime = dateTimeLabel
                            )
                            NemtNotifications.notifyNow(
                                context = context,
                                type = NemtNotificationType.TRIP_CONFIRMED,
                                title = "Trip confirmed",
                                body = "Your ride to $toLabel is confirmed for $dateTimeLabel."
                            )
                            NemtNotifications.scheduleTripReminder(
                                context = context,
                                requestKey = "${fromLabel}_${toLabel}_${dateTimeLabel}".replace(" ", "_"),
                                scheduledAtMillis = scheduledAtMillis.takeIf { it > 0L } ?: System.currentTimeMillis(),
                                destination = toLabel
                            )
                            confirmedFrom = fromLabel
                            confirmedTo = toLabel
                            confirmedDateTime = dateTimeLabel
                            confirmedVehicle = vehicleLabel
                            showTripConfirmedDialog = true
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(context.getString(R.string.trip_confirm))
                    }
                }
            }
        }
    }

    if (showTripConfirmedDialog) {
        Dialog(onDismissRequest = { }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                tonalElevation = 8.dp,
                shadowElevation = 14.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                                    MaterialTheme.colorScheme.surface
                                )
                            )
                        )
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Column {
                            Text(
                                text = "Trip Confirmed",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Your trip has been added to Upcoming.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        tonalElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ConfirmedTripDetailRow(label = "Date & time", value = confirmedDateTime)
                            ConfirmedTripDetailRow(label = "Pickup", value = confirmedFrom)
                            ConfirmedTripDetailRow(label = "Destination", value = confirmedTo)
                            ConfirmedTripDetailRow(label = "Vehicle", value = confirmedVehicle)
                        }
                    }

                    Button(
                        onClick = {
                            showTripConfirmedDialog = false
                            ridePlanner.clearTripPlanningSession()
                            onClose()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Done")
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfirmedTripDetailRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun TransportPreviewCard(
    selectedVehicle: String?,
    etaText: String?,
    tripDetailsText: String
) {
    val vehicleName = selectedVehicle ?: "Selected vehicle"
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.DirectionsCar,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = vehicleName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Preview: NEMT Vehicle #A12 • Ramp-equipped",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = etaText ?: "ETA --",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = tripDetailsText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MapScaleIndicator(
    modifier: Modifier = Modifier,
    cameraPosition: CameraPosition
) {
    val lat = cameraPosition.target.latitude
    val zoom = cameraPosition.zoom.toDouble()
    val metersPerPx = 156543.03392 * cos(Math.toRadians(lat)) / 2.0.pow(zoom)
    val rawMeters = (metersPerPx * 120.0).coerceAtLeast(1.0)
    val niceMeters = niceScaleDistance(rawMeters)
    val label = if (niceMeters >= 1000.0) "${(niceMeters / 1000.0).format1()} km" else "${niceMeters.toInt()} m"

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xCC101820))
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(
                text = label,
                color = Color.White,
                style = MaterialTheme.typography.labelSmall
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .width(96.dp)
                    .height(2.dp)
                    .border(1.dp, Color.White, RoundedCornerShape(2.dp))
            )
        }
    }
}

private fun niceScaleDistance(raw: Double): Double {
    val exp = floor(ln(raw) / ln(10.0))
    val base = 10.0.pow(exp)
    val n = raw / base
    val nice = when {
        n < 1.5 -> 1.0
        n < 3.5 -> 2.0
        n < 7.5 -> 5.0
        else -> 10.0
    }
    return nice * base
}

private fun Double.format1(): String = String.format("%.1f", this)

private fun createBlueDotMarkerDescriptor(): BitmapDescriptor {
    val sizePx = 56
    val center = sizePx / 2f
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x553B82F6
        style = Paint.Style.FILL
    }
    val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF2D8CFF.toInt()
        style = Paint.Style.FILL
    }
    val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    canvas.drawCircle(center, center, 22f, glowPaint)
    canvas.drawCircle(center, center, 10f, corePaint)
    canvas.drawCircle(center, center, 10f, ringPaint)

    return BitmapDescriptorFactory.fromBitmap(bitmap)
}

@Composable
private fun TripStatChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun TripAddressRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(22.dp)
        )
        Spacer(modifier = Modifier.size(12.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            if (subtitle.isNotBlank()) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
