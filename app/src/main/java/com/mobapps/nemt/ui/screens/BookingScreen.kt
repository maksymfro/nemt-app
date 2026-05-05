package com.mobapps.nemt.ui.screens

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.outlined.Accessibility
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.maps.android.SphericalUtil
import com.mobapps.nemt.BuildConfig
import com.mobapps.nemt.R
import com.mobapps.nemt.maps.RideStop
import com.mobapps.nemt.ui.rememberRidePlannerViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private data class VehicleOption(
    val label: String,
    val details: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun BookingScreen(
    onBack: () -> Unit,
    onTripConfirmed: () -> Unit,
    contentPadding: PaddingValues
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current
    val keyboardOpen = WindowInsets.ime.getBottom(density) > 0
    val scope = rememberCoroutineScope()
    val pickupFocusRequester = remember { FocusRequester() }
    val destinationFocusRequester = remember { FocusRequester() }
    val pickupCellInteraction = remember { MutableInteractionSource() }
    val destinationCellInteraction = remember { MutableInteractionSource() }
    var scheduledAtMillis by remember { mutableLongStateOf(0L) }

    val ridePlanner = rememberRidePlannerViewModel()
    val pickupStop by ridePlanner.pickupStop.collectAsState()
    val destinationStop by ridePlanner.destinationStop.collectAsState()
    val pickupQuery by ridePlanner.pickupQuery.collectAsState()
    val destinationQuery by ridePlanner.destinationQuery.collectAsState()
    val pickupPredictions by ridePlanner.pickupPredictions.collectAsState()
    val destinationPredictions by ridePlanner.destinationPredictions.collectAsState()
    val selectedVehicle by ridePlanner.selectedVehicle.collectAsState()
    val userMessage by ridePlanner.userMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val hasMapsKey = BuildConfig.MAPS_API_KEY.isNotBlank()
    val scrollState = rememberScrollState()
    val dateTimeText = remember(scheduledAtMillis) {
        if (scheduledAtMillis == 0L) "" else {
            SimpleDateFormat("EEE, MMM d, yyyy • h:mm a", Locale.getDefault())
                .format(scheduledAtMillis)
        }
    }
    val openDatePicker: () -> Unit = {
        keyboardController?.hide()
        focusManager.clearFocus(force = true)
        val base = Calendar.getInstance().apply {
            if (scheduledAtMillis != 0L) timeInMillis = scheduledAtMillis
        }
        DatePickerDialog(
            context,
            { _, year, month, day ->
                val updated = Calendar.getInstance().apply {
                    timeInMillis = if (scheduledAtMillis != 0L) scheduledAtMillis else System.currentTimeMillis()
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, day)
                }
                scheduledAtMillis = updated.timeInMillis
            },
            base.get(Calendar.YEAR),
            base.get(Calendar.MONTH),
            base.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
    val openTimePicker: () -> Unit = {
        keyboardController?.hide()
        focusManager.clearFocus(force = true)
        val base = Calendar.getInstance().apply {
            if (scheduledAtMillis != 0L) timeInMillis = scheduledAtMillis
        }
        TimePickerDialog(
            context,
            { _, selectedHour, selectedMinute ->
                val updated = Calendar.getInstance().apply {
                    timeInMillis = if (scheduledAtMillis != 0L) scheduledAtMillis else System.currentTimeMillis()
                    set(Calendar.HOUR_OF_DAY, selectedHour)
                    set(Calendar.MINUTE, selectedMinute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                scheduledAtMillis = updated.timeInMillis
            },
            base.get(Calendar.HOUR_OF_DAY),
            base.get(Calendar.MINUTE),
            false
        ).show()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            granted[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            ridePlanner.refreshDeviceLocation()
        }
    }

    LaunchedEffect(hasMapsKey) {
        if (!hasMapsKey) return@LaunchedEffect
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (fine || coarse) {
            ridePlanner.refreshDeviceLocation()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    LaunchedEffect(userMessage) {
        val msg = userMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        ridePlanner.clearMessage()
    }

    LaunchedEffect(scheduledAtMillis) {
        ridePlanner.setScheduledAtMillis(scheduledAtMillis)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (!keyboardOpen) {
                TopAppBar(
                    title = { Text("Plan ride") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Spacer(modifier = Modifier.height(4.dp))

                if (keyboardOpen) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                        Text(
                            text = "Plan ride",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.size(48.dp))
                    }
                }

                if (!hasMapsKey) {
                    Text(
                        text = "Add MAPS_API_KEY in local.properties for address suggestions and current location pickup.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (hasMapsKey && (pickupStop == null || destinationStop == null)) {
                    Text(
                        text = "Pick each address from the suggestions (or use current location for pickup). The map needs both stops to draw your driving route.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                val distanceText = if (pickupStop != null && destinationStop != null) {
                    val meters = SphericalUtil.computeDistanceBetween(
                        pickupStop!!.latLng,
                        destinationStop!!.latLng
                    )
                    val km = meters / 1000.0
                    "Approximate distance: ${"%.1f".format(km)} km"
                } else null
                if (distanceText != null) {
                    Text(
                        text = distanceText,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = pickupCellInteraction,
                            indication = null
                        ) {
                            pickupFocusRequester.requestFocus()
                            keyboardController?.show()
                        }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Pickup",
                            style = MaterialTheme.typography.titleMedium
                        )
                        TextButton(
                            onClick = { ridePlanner.useDeviceLocationForPickup() },
                            enabled = hasMapsKey
                        ) {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.size(6.dp))
                            Text("Use current location")
                        }
                    }
                    OutlinedTextField(
                        value = pickupQuery,
                        onValueChange = { ridePlanner.onPickupQueryChange(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(pickupFocusRequester)
                            .onFocusChanged { state ->
                                if (state.isFocused) keyboardController?.show()
                            },
                        label = { Text("Where should we pick you up?") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default)
                    )
                }

                if (pickupPredictions.isNotEmpty()) {
                    PredictionList(
                        predictions = pickupPredictions,
                        onPick = { ridePlanner.selectPickupPrediction(it) }
                    )
                }
                pickupStop?.let { StopSummary(it, onClear = { ridePlanner.clearPickupSelection() }) }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = destinationCellInteraction,
                            indication = null
                        ) {
                            destinationFocusRequester.requestFocus()
                            keyboardController?.show()
                        }
                ) {
                    Text(
                        text = "Destination",
                        style = MaterialTheme.typography.titleMedium
                    )
                    OutlinedTextField(
                        value = destinationQuery,
                        onValueChange = { ridePlanner.onDestinationQueryChange(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(destinationFocusRequester)
                            .onFocusChanged { state ->
                                if (state.isFocused) keyboardController?.show()
                            },
                        label = { Text("Where do you need to go?") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default)
                    )
                }

                if (destinationPredictions.isNotEmpty()) {
                    PredictionList(
                        predictions = destinationPredictions,
                        onPick = { ridePlanner.selectDestinationPrediction(it) }
                    )
                }
                destinationStop?.let {
                    StopSummary(it, onClear = { ridePlanner.clearDestinationSelection() })
                }

                Text(
                    text = "Date and time",
                    style = MaterialTheme.typography.titleMedium
                )
                if (dateTimeText.isNotBlank()) {
                    Text(
                        text = dateTimeText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = openDatePicker) {
                        Text("Choose date")
                    }
                    TextButton(onClick = openTimePicker) {
                        Text("Choose time")
                    }
                    TextButton(
                        onClick = { scheduledAtMillis = 0L },
                        enabled = scheduledAtMillis != 0L
                    ) {
                        Text("Clear")
                    }
                }

                val vehicleOptions = remember {
                    listOf(
                        VehicleOption(
                            label = "Wheelchair Van",
                            details = "Ramp + accessibility support",
                            icon = Icons.Outlined.Accessibility
                        ),
                        VehicleOption(
                            label = "Van with Oxygen",
                            details = "Oxygen tank mount + medical assist",
                            icon = Icons.Outlined.LocalHospital
                        ),
                        VehicleOption(
                            label = "Standard Accessible Sedan",
                            details = "Comfort ride with assistance",
                            icon = Icons.Outlined.DirectionsBus
                        )
                    )
                }

                Text(
                    text = "Vehicle type",
                    style = MaterialTheme.typography.titleMedium
                )
                vehicleOptions.forEach { option ->
                    val isSelected = selectedVehicle == option.label
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { ridePlanner.setVehicleChoice(option.label) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = option.icon,
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.size(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = option.label,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = option.details,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                val canAttemptConfirm =
                    pickupQuery.isNotBlank() &&
                        destinationQuery.isNotBlank() &&
                        scheduledAtMillis != 0L &&
                        selectedVehicle != null
                Button(
                    onClick = {
                        scope.launch {
                            keyboardController?.hide()
                            focusManager.clearFocus(force = true)
                            val err = ridePlanner.tryFinalizeBooking()
                            if (err != null) {
                                snackbarHostState.showSnackbar(err)
                            } else {
                                onTripConfirmed()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = canAttemptConfirm
                ) {
                    Text("Confirm booking")
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun PredictionList(
    predictions: List<AutocompletePrediction>,
    onPick: (AutocompletePrediction) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            predictions.take(6).forEach { prediction ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPick(prediction) }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = prediction.getPrimaryText(null).toString(),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = prediction.getSecondaryText(null).toString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun StopSummary(stop: RideStop, onClear: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = stop.title, style = MaterialTheme.typography.bodyMedium)
            if (stop.subtitle.isNotBlank()) {
                Text(
                    text = stop.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        TextButton(onClick = onClear) {
            Text("Clear")
        }
    }
}
