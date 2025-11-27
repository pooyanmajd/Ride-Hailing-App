package com.veezutech.domain.models.usecase

import com.veezutech.domain.models.LocationPoint
import com.veezutech.domain.repository.DriverRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AssignDriverToPickupUseCaseTest {

    private val repository: DriverRepository = mockk(relaxed = true)
    private val useCase = AssignDriverToPickupUseCase(repository)

    @Test
    fun `invoke delegates to repository`() = runTest {
        val location = LocationPoint(10.0, 20.0)
        coEvery { repository.assignDriverToPickup("driver-123", location) } returns Unit

        useCase(driverId = "driver-123", pickupLocation = location)

        coVerify(exactly = 1) { repository.assignDriverToPickup("driver-123", location) }
    }
}

