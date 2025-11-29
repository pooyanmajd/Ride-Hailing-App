package com.veezutech.map

import com.veezutech.domain.models.Driver
import com.veezutech.domain.models.DriverStatus
import com.veezutech.domain.models.LocationPoint
import com.veezutech.domain.repository.DriverRepository
import com.veezutech.domain.repository.LocationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeDriverRepository : DriverRepository {

    private val drivers = MutableStateFlow<List<Driver>>(emptyList())
    var lastAssigned: Pair<String, LocationPoint>? = null
        private set
    var lastReleasedDriverId: String? = null
        private set
    var simulationStartLocation: LocationPoint? = null
        private set

    override fun observeDrivers(): Flow<List<Driver>> = drivers.asStateFlow()

    override suspend fun startSimulation(userLocation: LocationPoint) {
        simulationStartLocation = userLocation
    }

    fun emitDrivers(list: List<Driver>) {
        drivers.value = list
    }

    override suspend fun assignDriverToPickup(driverId: String, pickupLocation: LocationPoint) {
        lastAssigned = driverId to pickupLocation
    }

    override suspend fun releaseDriver(driverId: String) {
        lastReleasedDriverId = driverId
    }

    override suspend fun stopSimulation() = Unit
}

class FakeLocationRepository : LocationRepository {

    private val locations = MutableSharedFlow<LocationPoint>(replay = 1)

    override fun observeCurrentLocation(): Flow<LocationPoint> = locations.asSharedFlow()

    suspend fun emitLocation(location: LocationPoint) {
        locations.emit(location)
    }
}

fun driver(
    id: String,
    latitude: Double = 0.0,
    longitude: Double = 0.0,
    status: DriverStatus = DriverStatus.AVAILABLE,
) = Driver(
    id = id,
    location = LocationPoint(latitude, longitude),
    headingDegrees = 0f,
    status = status,
)

