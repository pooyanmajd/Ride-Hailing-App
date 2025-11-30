package com.veezutech.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.veezutech.domain.models.Driver
import com.veezutech.domain.models.DriverStatus
import com.veezutech.domain.models.LocationPoint
import com.veezutech.domain.models.usecase.AssignDriverToPickupUseCase
import com.veezutech.domain.models.usecase.ObserveCurrentLocationUseCase
import com.veezutech.domain.models.usecase.ObserveDriversUseCase
import com.veezutech.domain.models.usecase.ReleaseDriverUseCase
import com.veezutech.domain.models.usecase.StartDriverSimulationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val observeCurrentLocation: ObserveCurrentLocationUseCase,
    private val observeDrivers: ObserveDriversUseCase,
    private val startDriverSimulation: StartDriverSimulationUseCase,
    private val assignDriverToPickup: AssignDriverToPickupUseCase,
    private val releaseDriver: ReleaseDriverUseCase,
    private val bookingTelemetry: BookingTelemetry,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()
    private var bookingJob: Job? = null

    init {
        observeLocation()
        observeDriversInternal()
    }

    fun onEvent(event: MapEvent) {
        when (event) {
            MapEvent.OnMapReady -> onMapReady()
            is MapEvent.OnLocationPermissionResult -> onLocationPermissionResult(event.granted)
            MapEvent.OnRetryClicked -> retry()
            MapEvent.OnMyLocationClicked -> recenterRequested()
            MapEvent.UserCenteredOnLocation -> onUserCentered()
            MapEvent.OnBookRideClicked -> onBookRideRequested()
            MapEvent.OnCancelBookingClicked -> onCancelBooking()
        }
    }

    private var hasStartedSimulation = false

    private fun onUserLocationUpdated(latLng: LocationPoint) {
        val isFirstLocation = _uiState.value.userLocation == null

        if (isFirstLocation) {
            _uiState.update {
                it.copy(
                    isLocationLoading = false,
                    userLocation = latLng,
                    hasCenteredOnUser = false
                )
            }
            startDriverSimulationIfReady(latLng)
        } else {
            _uiState.update { it.copy(userLocation = latLng) }
        }
    }

    private fun startDriverSimulationIfReady(userLocation: LocationPoint) {
        if (!hasStartedSimulation) {
            hasStartedSimulation = true
            viewModelScope.launch {
                try {
                    startDriverSimulation(userLocation)
                } catch (t: Throwable) {
                    _uiState.update {
                        it.copy(errorMessage = t.message)
                    }
                    hasStartedSimulation = false
                }
            }
        }
    }

    private fun onUserCentered() = viewModelScope.launch {
        _uiState.update { it.copy(hasCenteredOnUser = true) }
    }

    private fun observeLocation() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLocationLoading = true,
                    errorMessage = null,
                    isLocationPermissionRequired = false,
                    isLocationPermissionDenied = false,
                )
            }

            observeCurrentLocation()
                .onEach(::onUserLocationUpdated)
                .catch { throwable ->
                    // In a real app you’d branch by exception type (permission vs generic error)
                    _uiState.update {
                        it.copy(
                            isLocationLoading = false,
                            errorMessage = throwable.message,
                        )
                    }
                }
                .collect()
        }
    }

    private fun observeDriversInternal() {
        viewModelScope.launch {
            _uiState.update { it.copy(isDriversLoading = true) }

            observeDrivers()
                .onEach(::handleDriversUpdate)
                .catch { throwable ->
                    _uiState.update {
                        it.copy(
                            isDriversLoading = false,
                            errorMessage = throwable.message,
                        )
                    }
                }
                .collect()
        }
    }

    private fun handleDriversUpdate(drivers: List<Driver>) {
        val bookingStatus = resolveBookingStatus(drivers)
        _uiState.update {
            it.copy(
                isDriversLoading = false,
                drivers = drivers,
                bookingStatus = bookingStatus,
            )
        }
    }

    private fun resolveBookingStatus(drivers: List<Driver>): BookingStatus {
        val state = _uiState.value
        val assignedDriverId = state.assignedDriverId ?: return state.bookingStatus
        if (state.bookingStatus != BookingStatus.DRIVER_EN_ROUTE) return state.bookingStatus

        val userLocation = state.userLocation ?: return state.bookingStatus
        val assignedDriver = drivers.firstOrNull { it.id == assignedDriverId } ?: return state.bookingStatus
        val distance = calculateDistance(assignedDriver.location, userLocation)
        return if (distance < ARRIVAL_DISTANCE_THRESHOLD) BookingStatus.DRIVER_ARRIVED else state.bookingStatus
    }

    private fun onMapReady() {
        _uiState.update { it.copy(isMapReady = true) }
        _uiState.value.userLocation?.let { location ->
            startDriverSimulationIfReady(location)
        }
    }

    private fun onLocationPermissionResult(granted: Boolean) {
        if (granted) {
            _uiState.update {
                it.copy(
                    isLocationPermissionRequired = false,
                    isLocationPermissionDenied = false,
                    errorMessage = null,
                )
            }
            observeLocation()
        } else {
            _uiState.update {
                it.copy(
                    isLocationPermissionRequired = false,
                    isLocationPermissionDenied = true,
                    isLocationLoading = false,
                )
            }
        }
    }

    private fun retry() {
        _uiState.update { it.copy(errorMessage = null) }
        observeLocation()
        observeDriversInternal()
    }

    private fun recenterRequested() {
        _uiState.update { it.copy(hasCenteredOnUser = false) }
    }

    private fun onBookRideRequested() {
        val state = _uiState.value
        if (state.bookingStatus != BookingStatus.IDLE) return

        val userLocation = state.userLocation ?: return showError("Need your location first")
        val driver = selectNearestAvailableDriver(userLocation) ?: return showError("No drivers available nearby")

        _uiState.update {
            it.copy(
                bookingStatus = BookingStatus.REQUESTING,
                hasCenteredOnUser = false,
            )
        }

        bookingJob?.cancel()
        bookingJob = viewModelScope.launch {
            delay(1_000L)
            runCatching { assignDriverToPickup(driver.id, userLocation) }
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            bookingStatus = BookingStatus.DRIVER_EN_ROUTE,
                            assignedDriverId = driver.id,
                        )
                    }
                    bookingTelemetry.onBookingAssigned(driver.id, userLocation)
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            bookingStatus = BookingStatus.IDLE,
                            errorMessage = throwable.message,
                        )
                    }
                }
        }
    }

    private fun onCancelBooking() {
        val driverId = _uiState.value.assignedDriverId
        bookingJob?.cancel()
        if (driverId != null) {
            viewModelScope.launch { releaseDriver(driverId) }
        }
        bookingTelemetry.onBookingCancelled(driverId)
        _uiState.update {
            it.copy(
                bookingStatus = BookingStatus.IDLE,
                assignedDriverId = null,
            )
        }
    }

    private fun selectNearestAvailableDriver(userLocation: LocationPoint): Driver? {
        return _uiState.value.drivers
            .filter { it.status == DriverStatus.AVAILABLE }
            .minByOrNull { calculateDistance(it.location, userLocation) }
    }

    private fun calculateDistance(a: LocationPoint, b: LocationPoint): Double {
        val latDiff = a.latitude - b.latitude
        val lngDiff = a.longitude - b.longitude
        return kotlin.math.sqrt(latDiff * latDiff + lngDiff * lngDiff)
    }

    private fun showError(message: String) {
        _uiState.update { it.copy(errorMessage = message) }
    }

    override fun onCleared() {
        super.onCleared()
        bookingJob?.cancel()
    }

    companion object {
        private const val ARRIVAL_DISTANCE_THRESHOLD = 0.00025 // ~25m
    }
}

data class MapUiState(
    val isLocationLoading: Boolean = true,
    val isDriversLoading: Boolean = true,
    val userLocation: LocationPoint? = null,
    val hasCenteredOnUser: Boolean = false,
    val drivers: List<Driver> = emptyList(),
    val errorMessage: String? = null,
    val isLocationPermissionRequired: Boolean = false,
    val isLocationPermissionDenied: Boolean = false,
    val isMapReady: Boolean = false,
    val bookingStatus: BookingStatus = BookingStatus.IDLE,
    val assignedDriverId: String? = null,
)

sealed interface MapEvent {
    data object OnMapReady : MapEvent
    data class OnLocationPermissionResult(val granted: Boolean) : MapEvent
    data object OnRetryClicked : MapEvent
    data object OnMyLocationClicked : MapEvent
    data object UserCenteredOnLocation : MapEvent
    data object OnBookRideClicked : MapEvent
    data object OnCancelBookingClicked : MapEvent
}

enum class BookingStatus {
    IDLE,
    REQUESTING,
    DRIVER_EN_ROUTE,
    DRIVER_ARRIVED,
}