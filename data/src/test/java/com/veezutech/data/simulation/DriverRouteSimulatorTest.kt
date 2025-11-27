package com.veezutech.data.simulation

import com.veezutech.domain.models.LocationPoint
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DriverRouteSimulatorTest {

    private val simulator = DriverRouteSimulator()

    @Test
    fun `ensurePatrolRoute returns provider route`() = runTest {
        val start = LocationPoint(51.0, -0.1)
        val waypoints = listOf(LocationPoint(51.01, -0.12))
        val expectedRoute = listOf(start, waypoints.first())

        val result = simulator.ensurePatrolRoute(
            driverLocation = start,
            waypoints = waypoints,
            routeProvider = { _, _ -> expectedRoute }
        )

        assertEquals(expectedRoute, result)
    }

    @Test
    fun `ensurePatrolRoute returns empty when provider fails`() = runTest {
        val start = LocationPoint(51.0, -0.1)
        val waypoints = listOf(LocationPoint(51.01, -0.12))

        val result = simulator.ensurePatrolRoute(
            driverLocation = start,
            waypoints = waypoints,
            routeProvider = { _, _ -> throw IllegalStateException("boom") }
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun `meters to degrees conversion is proportional`() {
        val result = simulator.metersToDegrees(111_000.0)

        assertEquals(1.0, result, 1e-6)
    }
}

