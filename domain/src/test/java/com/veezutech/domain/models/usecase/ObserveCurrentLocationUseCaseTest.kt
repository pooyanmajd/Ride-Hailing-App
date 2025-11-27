package com.veezutech.domain.models.usecase

import com.veezutech.domain.models.LocationPoint
import com.veezutech.domain.repository.LocationRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertSame
import org.junit.Test

class ObserveCurrentLocationUseCaseTest {

    private val repository: LocationRepository = mockk()
    private val useCase = ObserveCurrentLocationUseCase(repository)

    @Test
    fun `invoke returns repository location flow`() {
        val expectedFlow: Flow<LocationPoint> = flowOf(LocationPoint(1.0, 2.0))
        every { repository.observeCurrentLocation() } returns expectedFlow

        val actual = useCase()

        assertSame(expectedFlow, actual)
    }
}

