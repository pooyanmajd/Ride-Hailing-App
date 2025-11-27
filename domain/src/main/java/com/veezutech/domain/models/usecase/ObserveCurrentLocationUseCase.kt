package com.veezutech.domain.models.usecase

import com.veezutech.domain.models.LocationPoint
import com.veezutech.domain.repository.LocationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveCurrentLocationUseCase @Inject constructor(
    private val repository: LocationRepository,
) {
    operator fun invoke(): Flow<LocationPoint> = repository.observeCurrentLocation()
}