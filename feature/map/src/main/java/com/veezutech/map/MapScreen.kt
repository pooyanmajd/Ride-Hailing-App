package com.veezutech.map

import android.Manifest
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.veezutech.domain.models.Driver
import com.veezutech.domain.models.DriverStatus
import com.veezutech.domain.models.LocationPoint
import com.veezutech.ui.R.drawable
import com.veezutech.ui.bitmapDescriptorFromVector
import com.veezutech.ui.hasLocationPermission

@Composable
fun MapScreen(
    modifier: Modifier = Modifier,
    state: MapUiState,
    onEvent: (MapEvent) -> Unit,
) {

    val context = LocalContext.current

    // Track whether we have already requested permissions to avoid spamming
    var hasRequestedPermission by rememberSaveable { mutableStateOf(false) }

    val locationPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    )

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted =
            result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        onEvent(MapEvent.OnLocationPermissionResult(granted))
    }

    LaunchedEffect(Unit) {
        val granted = context.hasLocationPermission()

        if (granted) {
            onEvent(MapEvent.OnLocationPermissionResult(true))
            return@LaunchedEffect
        }

        if (!hasRequestedPermission) {
            hasRequestedPermission = true
            permissionLauncher.launch(locationPermissions)
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val markerColors = rememberMarkerColors(context)

    // When an error appears in state, show it as a Snackbar
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val cameraPositionState = rememberCameraPositionState {
        // If we already know the user location at first composition, start centered on it.
        state.userLocation?.let { loc ->
            position = CameraPosition.fromLatLngZoom(loc.toLatLng(), 14f)
        }
    }

    LaunchedEffect(state.userLocation, state.hasCenteredOnUser) {
        val location = state.userLocation ?: return@LaunchedEffect
        if (!state.hasCenteredOnUser) {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(location.toLatLng(), 15f)
            )
            onEvent(MapEvent.UserCenteredOnLocation)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                isMyLocationEnabled = state.userLocation != null &&
                        !state.isLocationPermissionDenied
            ),
            uiSettings = MapUiSettings(
                myLocationButtonEnabled = false,
                zoomControlsEnabled = false
            ),
            onMapLoaded = {
                onEvent(MapEvent.OnMapReady)
            }
        ) {

            // User marker
            state.userLocation?.let { location ->
                Marker(
                    state = MarkerState(position = location.toLatLng()),
                    title = stringResource(R.string.you),
                )
            }

            // Driver markers
            state.drivers.forEach { driver ->
                DriverMarker(
                    driver = driver,
                    isAssigned = driver.id == state.assignedDriverId &&
                            state.bookingStatus in setOf(BookingStatus.DRIVER_EN_ROUTE, BookingStatus.DRIVER_ARRIVED),
                    colors = markerColors,
                )
            }
        }

        if (state.isLocationLoading || state.isDriversLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        if (state.isLocationPermissionDenied) {
            PermissionOverlay(
                message = stringResource(R.string.location_permission_required),
                onRetry = {
                    onEvent(MapEvent.OnRetryClicked)
                    permissionLauncher.launch(locationPermissions)
                }
            )
        }

        // Floating Action Buttons - positioned absolutely
        MapFabRow(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            onMyLocationClicked = { onEvent(MapEvent.OnMyLocationClicked) },
        )

        val isScreenReady = state.isMapReady &&
                !state.isLocationLoading &&
                !state.isDriversLoading &&
                state.userLocation != null &&
                state.drivers.isNotEmpty() &&
                state.hasCenteredOnUser

        if (isScreenReady) {
            BookingStatusPanel(
                status = state.bookingStatus,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                onBookClick = { onEvent(MapEvent.OnBookRideClicked) },
                onCancelClick = { onEvent(MapEvent.OnCancelBookingClicked) },
            )
        }

        // Snackbar for errors
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

}

@Composable
private fun MapFabRow(
    modifier: Modifier = Modifier,
    onMyLocationClicked: () -> Unit,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.BottomEnd
    ) {
        Column(
            horizontalAlignment = Alignment.End,
        ) {
            FloatingActionButton(
                onClick = onMyLocationClicked,
                modifier = Modifier.padding(bottom = 12.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "My location"
                )
            }

        }
    }
}

@Composable
private fun PermissionOverlay(
    message: String,
    onRetry: () -> Unit,
) {
    Surface(
        tonalElevation = 4.dp,
        shadowElevation = 4.dp,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize(),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                FloatingActionButton(onClick = onRetry) {
                    Text("Retry")
                }
            }
        }
    }
}

// --- Mapping helpers (UI-only) ---
@Composable
private fun BookingStatusPanel(
    status: BookingStatus,
    modifier: Modifier = Modifier,
    onBookClick: () -> Unit,
    onCancelClick: () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 6.dp,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = when (status) {
                    BookingStatus.IDLE -> stringResource(R.string.ready_to_book_a_ride)
                    BookingStatus.REQUESTING -> stringResource(R.string.finding_the_best_driver)
                    BookingStatus.DRIVER_EN_ROUTE -> stringResource(R.string.driver_is_on_the_way)
                    BookingStatus.DRIVER_ARRIVED -> stringResource(R.string.your_driver_has_arrived)
                },
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (status == BookingStatus.IDLE) {
                Button(onClick = onBookClick) {
                    Text(stringResource(R.string.book_ride))
                }
            } else {
                Button(onClick = onCancelClick) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    }
}

@Composable
private fun DriverMarker(
    driver: Driver,
    isAssigned: Boolean,
    colors: MarkerColors,
) {
    val snippet = when {
        isAssigned || driver.status == DriverStatus.EN_ROUTE -> stringResource(R.string.en_route_to_you)
        driver.status == DriverStatus.BUSY -> stringResource(R.string.on_trip)
        else -> stringResource(R.string.available)
    }
    val icon = when {
        isAssigned || driver.status == DriverStatus.EN_ROUTE -> colors.enRoute
        driver.status == DriverStatus.BUSY -> colors.busy
        else -> colors.available
    }

    Marker(
        state = MarkerState(position = driver.location.toLatLng()),
        title = "Driver ${driver.id}",
        rotation = driver.headingDegrees,
        icon = icon,
        anchor = Offset(0.5f, 0.5f),
        flat = true,
        snippet = snippet,
    )
}

private fun LocationPoint.toLatLng(): LatLng =
    LatLng(latitude, longitude)

private data class MarkerColors(
    val available: com.google.android.gms.maps.model.BitmapDescriptor,
    val busy: com.google.android.gms.maps.model.BitmapDescriptor,
    val enRoute: com.google.android.gms.maps.model.BitmapDescriptor,
)

@Composable
private fun rememberMarkerColors(context: Context): MarkerColors {
    val available = remember(context) {
        bitmapDescriptorFromVector(
            context = context,
            vectorResId = drawable.ic_car_available
        )
    }
    val busy = remember(context) {
        bitmapDescriptorFromVector(
            context = context,
            vectorResId = drawable.ic_car_busy,
        )
    }
    val enRoute = remember(context) {
        bitmapDescriptorFromVector(
            context = context,
            vectorResId = drawable.ic_car_en_route
        )
    }
    return MarkerColors(available, busy, enRoute)
}

@Composable
private fun PermissionDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            Button(onClick = onConfirm) { Text(confirmLabel) }
        },
        dismissButton = {}
    )
}

@Preview(showBackground = true)
@Composable
private fun MapScreenPreview_Loading() {
    MaterialTheme {
        MapScreen(
            state = MapUiState(
                isLocationLoading = true,
                isDriversLoading = true
            ),
            onEvent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MapScreenPreview_WithContent() {
    val sampleUserLocation = LocationPoint(37.7749, -122.4194) // San Francisco
    val sampleDrivers = listOf(
        Driver(
            id = "1",
            location = LocationPoint(37.7755, -122.4190),
            headingDegrees = 45f,
            status = DriverStatus.AVAILABLE
        ),
        Driver(
            id = "2",
            location = LocationPoint(37.7740, -122.4220),
            headingDegrees = 180f,
            status = DriverStatus.BUSY
        ),
        Driver(
            id = "3",
            location = LocationPoint(37.7760, -122.4170),
            headingDegrees = 90f,
            status = DriverStatus.EN_ROUTE
        )
    )

    MaterialTheme {
        MapScreen(
            state = MapUiState(
                userLocation = sampleUserLocation,
                drivers = sampleDrivers,
                isLocationLoading = false,
                isDriversLoading = false,
                assignedDriverId = "3",
                bookingStatus = BookingStatus.DRIVER_EN_ROUTE,
            ),
            onEvent = {},
        )
    }
}


@Preview(showBackground = true)
@Composable
private fun MapScreenPreview_PermissionDenied() {
    MaterialTheme {
        MapScreen(
            state = MapUiState(
                isLocationPermissionDenied = true,
                isLocationLoading = false,
                isDriversLoading = false,
            ),
            onEvent = {},
        )
    }
}
