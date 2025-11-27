package com.veezutech.data.repository

import com.veezutech.common.DispatchersProvider
import com.veezutech.data.directions.DirectionsApiDataSource
import com.veezutech.data.simulation.DriverRouteSimulator
import com.veezutech.domain.models.Driver
import com.veezutech.domain.models.DriverStatus
import com.veezutech.domain.models.LocationPoint
import com.veezutech.domain.repository.DriverRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class DriverRepositoryImplTest {

    private val dispatcher = StandardTestDispatcher()
    private val dispatchersProvider = object : DispatchersProvider {
        override val io: CoroutineDispatcher = dispatcher
        override val main: CoroutineDispatcher = dispatcher
        override val default: CoroutineDispatcher = dispatcher
    }

    private val directionsApi: DirectionsApiDataSource = mockk(relaxed = true)

    private val driver = Driver(
        id = "D0",
        location = LocationPoint(10.0, 10.0),
        headingDegrees = 0f,
        status = DriverStatus.AVAILABLE
    )
    private val seedRoute = listOf(
        LocationPoint(10.0, 10.0),
        LocationPoint(10.001, 10.001)
    )

    private lateinit var simulator: DriverRouteSimulator
    private lateinit var repository: DriverRepository

    @Before
    fun setup() {
        simulator = mockk(relaxed = true)
        coEvery { simulator.generateWaypoints(any(), any()) } returns listOf(LocationPoint(11.0, 11.0))
        coEvery { simulator.generateDriversWithRoutes(any(), any(), any()) } returns listOf(driver to seedRoute)
        coEvery { simulator.ensurePatrolRoute(any(), any(), any()) } returns seedRoute.toMutableList()
        every { simulator.calculateBearing(any(), any()) } returns 0.0
        every { simulator.getNearestRoadDirection(any()) } returns 0.0
        every { simulator.calculateDistance(any(), any()) } returns 1.0
        every { simulator.metersToDegrees(any()) } answers { (firstArg<Double>()) / 111_000.0 }
        every { simulator.interpolate(any(), any(), any()) } answers { secondArg() }
        coEvery { directionsApi.fetchRoute(any(), any()) } returns null
        repository = DriverRepositoryImpl(dispatchersProvider, directionsApi, simulator)
    }

    @Test
    fun `startSimulation seeds initial drivers`() = runTest {
        repository.startSimulation(LocationPoint(51.0, -0.1))
        dispatcher.scheduler.runCurrent()

        val drivers = repository.observeDrivers().first()
        assertEquals(listOf(driver), drivers)
    }

    @Test
    fun `startSimulation only seeds once`() = runTest {
        repository.startSimulation(LocationPoint(51.0, -0.1))
        dispatcher.scheduler.runCurrent()

        repository.startSimulation(LocationPoint(0.0, 0.0))
        dispatcher.scheduler.runCurrent()

        coVerify(exactly = 1) { simulator.generateWaypoints(any(), any()) }
    }

    @Test
    fun `assignDriverToPickup calls directions API for known driver`() = runTest {
        repository.startSimulation(LocationPoint(51.0, -0.1))
        dispatcher.scheduler.runCurrent()

        val pickup = LocationPoint(10.01, 10.01)
        coEvery { directionsApi.fetchRoute(driver.location, pickup) } returns seedRoute

        repository.assignDriverToPickup("D0", pickup)

        coVerify { directionsApi.fetchRoute(driver.location, pickup) }
    }

    @Test
    fun `assignDriverToPickup ignores unknown driver id`() = runTest {
        repository.startSimulation(LocationPoint(51.0, -0.1))
        dispatcher.scheduler.runCurrent()

        repository.assignDriverToPickup("unknown", LocationPoint(0.0, 0.0))

        coVerify(exactly = 0) { directionsApi.fetchRoute(any(), any()) }
    }
}

