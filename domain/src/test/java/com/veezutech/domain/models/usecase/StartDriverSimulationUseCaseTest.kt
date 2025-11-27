package com.veezutech.domain.models.usecase

import com.veezutech.domain.models.LocationPoint
import com.veezutech.domain.repository.DriverRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class StartDriverSimulationUseCaseTest {

    private val repository: DriverRepository = mockk(relaxed = true)
    private val useCase = StartDriverSimulationUseCase(repository)

    @Test
    fun `invoke starts simulation at user location`() = runTest {
        val location = LocationPoint(51.5, -0.1)
        coEvery { repository.startSimulation(location) } returns Unit

        useCase(location)

        coVerify(exactly = 1) { repository.startSimulation(location) }
    }
}

