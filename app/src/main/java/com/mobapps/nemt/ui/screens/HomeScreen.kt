package com.mobapps.nemt.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.mobapps.nemt.BuildConfig
import com.mobapps.nemt.R
import com.mobapps.nemt.ui.rememberRidePlannerViewModel

private val BackgroundColor = Color(0xFFF3F4F7)
private val CardColor = Color(0xFFFFFFFF)
private val CardHighlight = Color(0xFFF8F8FB)
private val BorderSubtle = Color(0xFFE7E8EE)
private val TextPrimary = Color(0xFF111318)
private val TextSecondary = Color(0xFF7A7F8C)
private val BrandBlue = Color(0xFF2F8FFF)
private val DarkChip = Color(0xFF1F222A)
private val DefaultMapCenter = LatLng(25.7617, -80.1918)

@Composable
fun HomeScreen(
    userName: String,
    onGoToTrips: () -> Unit,
    onGoToProfile: () -> Unit,
    onGoToBooking: () -> Unit,
    onNeedAssistanceClick: () -> Unit,
    contentPadding: PaddingValues
) {
    val ridePlanner = rememberRidePlannerViewModel()
    val deviceLocation by ridePlanner.deviceLocation.collectAsState()
    val hasMapsKey = BuildConfig.MAPS_API_KEY.isNotBlank()
    val context = LocalContext.current

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

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = BackgroundColor
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            verticalArrangement = Arrangement.Top
        ) {
            item {
                TopMapSection(
                    hasMapsKey = hasMapsKey,
                    deviceLocation = deviceLocation,
                    onNeedAssistanceClick = onNeedAssistanceClick
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                Column(
                    modifier = Modifier.padding(horizontal = 18.dp)
                ) {
                    GreetingCard(
                        userName = userName,
                        onNewRideClick = onGoToBooking,
                        onUpcomingClick = onGoToTrips
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    DestinationSearchSection(
                        onClick = onGoToBooking
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    RecentDestinationsSection(
                        onDestinationClick = onGoToBooking
                    )

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun TopMapSection(
    hasMapsKey: Boolean,
    deviceLocation: LatLng?,
    onNeedAssistanceClick: () -> Unit
) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            deviceLocation ?: DefaultMapCenter,
            12f
        )
    }
    LaunchedEffect(deviceLocation) {
        deviceLocation?.let {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(it, 14f),
                durationMs = 500
            )
        }
    }

    val mapStatusLabel = when {
        !hasMapsKey -> "Map preview"
        deviceLocation == null -> "Finding your location"
        else -> "You are here"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(
                RoundedCornerShape(
                    bottomStart = 24.dp,
                    bottomEnd = 24.dp
                )
            )
            .background(Color(0xFF151821))
    ) {
        if (hasMapsKey) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    compassEnabled = false,
                    mapToolbarEnabled = false,
                    myLocationButtonEnabled = false
                )
            ) {
                deviceLocation?.let { ll ->
                    Marker(
                        state = MarkerState(position = ll),
                        title = "You are here"
                    )
                }
            }
        } else {
            Image(
                painter = painterResource(id = R.drawable.map_placeholder),
                contentDescription = "Map background",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(CardColor)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo_placeholder),
                    contentDescription = "Logo",
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Fit
                )
            }

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0x33000000))
                    .clickable { onNeedAssistanceClick() }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.HelpOutline,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = "Need assistance?",
                    color = Color.White,
                    fontSize = 14.sp
                )
            }
        }

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!hasMapsKey || deviceLocation == null) {
                Icon(
                    imageVector = Icons.Outlined.LocationOn,
                    contentDescription = null,
                    tint = BrandBlue,
                    modifier = Modifier.size(30.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0x66000000))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = mapStatusLabel,
                    color = Color.White,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun GreetingCard(
    userName: String,
    onNewRideClick: () -> Unit,
    onUpcomingClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = CardHighlight),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderSubtle, RoundedCornerShape(22.dp))
                .padding(18.dp)
        ) {
            Text(
                text = "Good afternoon, $userName",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Safe, non-emergency transport\nfor you, door-to-door.",
                fontSize = 14.sp,
                lineHeight = 19.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row {
                ActionTile(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.DirectionsCar,
                    title = "New ride",
                    subtitle = "Request transport",
                    onClick = onNewRideClick
                )

                Spacer(modifier = Modifier.width(12.dp))

                ActionTile(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.CalendarToday,
                    title = "Upcoming",
                    subtitle = "Schedule transport",
                    onClick = onUpcomingClick
                )
            }
        }
    }
}

@Composable
private fun ActionTile(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(CardColor)
            .border(1.dp, BorderSubtle, RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x332F8FFF))
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = BrandBlue,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = subtitle,
            fontSize = 13.sp,
            color = TextSecondary
        )
    }
}

@Composable
private fun DestinationSearchSection(
    onClick: () -> Unit
) {
    Column {
        Text(
            text = "Plan your next ride",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(CardColor)
                .border(1.dp, BorderSubtle, RoundedCornerShape(18.dp))
                .clickable { onClick() }
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(18.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "Where do you need to go?",
                fontSize = 15.sp,
                color = TextSecondary,
                modifier = Modifier.weight(1f)
            )

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(DarkChip)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.AccessTime,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = "Later",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun RecentDestinationsSection(
    onDestinationClick: () -> Unit
) {
    Column {
        Text(
            text = "Recent destinations",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(10.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(CardColor)
                .border(1.dp, BorderSubtle, RoundedCornerShape(18.dp))
        ) {
            DestinationItem(
                icon = Icons.Outlined.AccessTime,
                title = "Central Medical Center",
                subtitle = "Downtown, Miami",
                onClick = onDestinationClick
            )

            DividerLine()

            DestinationItem(
                icon = Icons.Outlined.AccessTime,
                title = "North Rehabilitation Clinic",
                subtitle = "Uptown, Miami",
                onClick = onDestinationClick
            )

            DividerLine()

            DestinationItem(
                icon = Icons.Outlined.DirectionsBus,
                title = "Dialysis Center",
                subtitle = "Westside, Miami",
                onClick = onDestinationClick
            )
        }
    }
}

@Composable
private fun DestinationItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(CardHighlight),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun DividerLine() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .height(1.dp)
            .background(BorderSubtle)
    )
}
