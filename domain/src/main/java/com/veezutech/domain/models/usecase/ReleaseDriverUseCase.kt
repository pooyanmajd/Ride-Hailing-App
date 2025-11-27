package com.veezutech.domain.models.usecase

import com.veezutech.domain.repository.DriverRepository
import javax.inject.Inject

class ReleaseDriverUseCase @Inject constructor(
    private val repository: DriverRepository,
) {
    suspend operator fun invoke(driverId: String) {
        repository.releaseDriver(driverId)
    }
}

