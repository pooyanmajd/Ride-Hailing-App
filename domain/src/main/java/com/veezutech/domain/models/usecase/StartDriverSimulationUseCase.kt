package com.veezutech.domain.models.usecase

import com.veezutech.domain.models.LocationPoint
import com.veezutech.domain.repository.DriverRepository
import javax.inject.Inject

class StartDriverSimulationUseCase @Inject constructor(
    private val repository: DriverRepository,
) {
    suspend operator fun invoke(userLocation: LocationPoint) = repository.startSimulation(userLocation)
}