package com.veezutech.domain.models.usecase

import com.veezutech.domain.models.Driver
import com.veezutech.domain.repository.DriverRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertSame
import org.junit.Test

class ObserveDriversUseCaseTest {

    private val repository: DriverRepository = mockk()
    private val useCase = ObserveDriversUseCase(repository)

    @Test
    fun `invoke returns repository flow`() {
        val expectedFlow: Flow<List<Driver>> = flowOf(emptyList())
        every { repository.observeDrivers() } returns expectedFlow

        val actual = useCase()

        assertSame(expectedFlow, actual)
    }
}

