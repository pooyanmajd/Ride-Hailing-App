package com.veezutech.map

import com.veezutech.domain.models.LocationPoint
import com.veezutech.domain.models.usecase.AssignDriverToPickupUseCase
import com.veezutech.domain.models.usecase.ObserveCurrentLocationUseCase
import com.veezutech.domain.models.usecase.ObserveDriversUseCase
import com.veezutech.domain.models.usecase.ReleaseDriverUseCase
import com.veezutech.domain.models.usecase.StartDriverSimulationUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModelTest {

    private lateinit var dispatcher: TestDispatcher
    @Before
    fun setup() {
        dispatcher = StandardTestDispatcher()
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun bookingFlow_assignsNearestDriverAfterDelay() = runTest(dispatcher) {
        val driverRepository = FakeDriverRepository()
        val locationRepository = FakeLocationRepository()
        val telemetry = RecordingTelemetry()
        val viewModel = createViewModel(driverRepository, locationRepository, telemetry)

        locationRepository.emitLocation(USER_LOCATION)
        advanceUntilIdle()

        val nearDriver = driver(id = "near", latitude = 37.0001, longitude = -122.0001)
        val farDriver = driver(id = "far", latitude = 37.01, longitude = -122.01)
        driverRepository.emitDrivers(listOf(nearDriver, farDriver))
        advanceUntilIdle()

        viewModel.onEvent(MapEvent.OnMapReady)
        advanceUntilIdle()

        viewModel.onEvent(MapEvent.OnBookRideClicked)
        advanceTimeBy(1_000)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(BookingStatus.DRIVER_EN_ROUTE, state.bookingStatus)
        assertEquals("near", state.assignedDriverId)
        assertEquals("near", driverRepository.lastAssigned?.first)
        assertEquals(USER_LOCATION, driverRepository.lastAssigned?.second)
        assertEquals("near", telemetry.assignments.single().first)
    }

    @Test
    fun cancelBooking_releasesDriverAndResetsState() = runTest(dispatcher) {
        val driverRepository = FakeDriverRepository()
        val locationRepository = FakeLocationRepository()
        val telemetry = RecordingTelemetry()
        val viewModel = createViewModel(driverRepository, locationRepository, telemetry)

        locationRepository.emitLocation(USER_LOCATION)
        advanceUntilIdle()

        val nearDriver = driver(id = "near", latitude = 37.0001, longitude = -122.0001)
        driverRepository.emitDrivers(listOf(nearDriver))
        advanceUntilIdle()

        viewModel.onEvent(MapEvent.OnMapReady)
        advanceUntilIdle()

        viewModel.onEvent(MapEvent.OnBookRideClicked)
        advanceTimeBy(1_000)
        advanceUntilIdle()

        viewModel.onEvent(MapEvent.OnCancelBookingClicked)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(BookingStatus.IDLE, state.bookingStatus)
        assertNull(state.assignedDriverId)
        assertEquals("near", driverRepository.lastReleasedDriverId)
        assertEquals(listOf("near"), telemetry.cancellations)
    }

    private fun createViewModel(
        driverRepository: FakeDriverRepository,
        locationRepository: FakeLocationRepository,
        telemetry: BookingTelemetry,
    ): MapViewModel {
        val observeLocation = ObserveCurrentLocationUseCase(locationRepository)
        val observeDrivers = ObserveDriversUseCase(driverRepository)
        val startSimulation = StartDriverSimulationUseCase(driverRepository)
        val assignDriver = AssignDriverToPickupUseCase(driverRepository)
        val releaseDriver = ReleaseDriverUseCase(driverRepository)

        return MapViewModel(
            observeLocation,
            observeDrivers,
            startSimulation,
            assignDriver,
            releaseDriver,
            telemetry,
        )
    }

    companion object {
        private val USER_LOCATION = LocationPoint(37.0, -122.0)
    }

    private class RecordingTelemetry : BookingTelemetry {
        val assignments = mutableListOf<Pair<String, LocationPoint>>()
        val cancellations = mutableListOf<String?>()

        override fun onBookingAssigned(driverId: String, pickupLocation: LocationPoint) {
            assignments += driverId to pickupLocation
        }

        override fun onBookingCancelled(driverId: String?) {
            cancellations += driverId
        }
    }
}

