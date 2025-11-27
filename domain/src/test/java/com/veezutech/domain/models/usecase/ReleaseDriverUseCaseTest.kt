package com.veezutech.domain.models.usecase

import com.veezutech.domain.repository.DriverRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ReleaseDriverUseCaseTest {

    private val repository: DriverRepository = mockk(relaxed = true)
    private val useCase = ReleaseDriverUseCase(repository)

    @Test
    fun `invoke releases driver through repository`() = runTest {
        coEvery { repository.releaseDriver("driver-123") } returns Unit

        useCase("driver-123")

        coVerify(exactly = 1) { repository.releaseDriver("driver-123") }
    }
}

