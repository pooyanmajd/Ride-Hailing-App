package com.veezutech.domain.models.usecase

import com.veezutech.domain.models.Driver
import com.veezutech.domain.repository.DriverRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveDriversUseCase @Inject constructor(
    private val repository: DriverRepository,
) {
    operator fun invoke(): Flow<List<Driver>> = repository.observeDrivers()
}

