package com.veezutech.domain.repository

import com.veezutech.domain.models.Driver
import com.veezutech.domain.models.LocationPoint
import kotlinx.coroutines.flow.Flow

interface DriverRepository {
    fun observeDrivers(): Flow<List<Driver>>
    suspend fun startSimulation(userLocation: LocationPoint)
    suspend fun assignDriverToPickup(driverId: String, pickupLocation: LocationPoint)
    suspend fun releaseDriver(driverId: String)
    suspend fun stopSimulation()
}