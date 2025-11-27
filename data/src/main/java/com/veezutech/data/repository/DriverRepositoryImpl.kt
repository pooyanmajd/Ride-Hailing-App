package com.veezutech.data.repository

import com.veezutech.common.DispatchersProvider
import com.veezutech.data.directions.DirectionsApiDataSource
import com.veezutech.data.simulation.DriverRouteSimulator
import com.veezutech.data.simulation.NavigationMode
import com.veezutech.data.simulation.NavigationState
import com.veezutech.domain.models.Driver
import com.veezutech.domain.models.LocationPoint
import com.veezutech.domain.repository.DriverRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class DriverRepositoryImpl @Inject constructor(
    dispatchers: DispatchersProvider,
    private val directionsApi: DirectionsApiDataSource,
    private val simulator: DriverRouteSimulator,
) : DriverRepository {

    private val scope = CoroutineScope(SupervisorJob() + dispatchers.io)
    private val driversState = MutableStateFlow<List<Driver>>(emptyList())
    private val navigationStates = mutableMapOf<String, NavigationState>()

    // Holds the long-running simulation loop so screens can cancel it explicitly.
    private var simulationJob: Job? = null

    override fun observeDrivers(): Flow<List<Driver>> = driversState.asStateFlow()

    override suspend fun startSimulation(userLocation: LocationPoint) {
        if (simulationJob?.isActive == true) return

        simulationJob = scope.launch {
            val waypoints = simulator.generateWaypoints(userLocation, 5)
            val driversWithRoutes = simulator.generateDriversWithRoutes(userLocation, waypoints, ::fetchRoadAlignedRoute)
            driversState.value = driversWithRoutes.map { it.first }

            driversWithRoutes.forEach { (driver, route) ->
                val initialDirection = if (route.isNotEmpty()) {
                    simulator.calculateBearing(driver.location, route.first())
                } else {
                    Random.nextDouble(0.0, 360.0)
                }
                navigationStates[driver.id] = NavigationState(
                    route = route.toMutableList(),
                    currentDirection = simulator.getNearestRoadDirection(initialDirection),
                    mode = NavigationMode.PATROL
                )
            }
            while (isActive) {
                delay(1500L)
                val currentDrivers = driversState.value
                driversState.value = currentDrivers.map { driver ->
                    val navState = navigationStates[driver.id] ?: return@map driver
                    val route = navState.route

                    if (route.size < 2 || navState.routeSegmentIndex >= route.lastIndex) {
                        if (navState.mode == NavigationMode.PATROL) {
                            val newRoute = simulator.ensurePatrolRoute(driver.location, waypoints, ::fetchRoadAlignedRoute)
                            navState.route.clear()
                            navState.route.addAll(newRoute)
                            navState.routeSegmentIndex = 0
                        }
                        return@map driver
                    }

                    val currentPoint = route[navState.routeSegmentIndex]
                    val nextPoint = route[navState.routeSegmentIndex + 1]
                    val segmentDistance = simulator.calculateDistance(currentPoint, nextPoint)

                    val metersPerStep = when (navState.mode) {
                        NavigationMode.PATROL -> 14.0
                        NavigationMode.PICKUP -> 22.0
                    }
                    val stepRatio = (simulator.metersToDegrees(metersPerStep) / segmentDistance).coerceIn(0.0, 1.0)

                    val newLocation = simulator.interpolate(currentPoint, nextPoint, stepRatio)

                    if (stepRatio >= 1.0) {
                        navState.routeSegmentIndex++
                    } else {
                        navState.route[navState.routeSegmentIndex] = newLocation
                    }

                    val heading = simulator.calculateBearing(currentPoint, nextPoint).toFloat()
                    driver.copy(location = newLocation, headingDegrees = heading)
                }
            }
        }
    }

    override suspend fun stopSimulation() {
        simulationJob?.cancelAndJoin()
        simulationJob = null
        navigationStates.clear()
        driversState.value = emptyList()
    }

    override suspend fun assignDriverToPickup(driverId: String, pickupLocation: LocationPoint) {
        val driver = driversState.value.firstOrNull { it.id == driverId } ?: return
        val route = fetchRoadAlignedRoute(driver.location, pickupLocation)
        if (route.isEmpty()) return

        navigationStates[driverId]?.let { navState ->
            navState.route.clear()
            navState.route.addAll(route)
            navState.routeSegmentIndex = 0
            navState.currentDirection = simulator.getNearestRoadDirection(
                simulator.calculateBearing(driver.location, route.first())
            )
            navState.mode = NavigationMode.PICKUP
        }
    }

    private suspend fun fetchRoadAlignedRoute(start: LocationPoint, end: LocationPoint): List<LocationPoint> {
        return directionsApi.fetchRoute(start, end) ?: emptyList()
    }

    override suspend fun releaseDriver(driverId: String) {
        navigationStates[driverId]?.let { navState ->
            navState.mode = NavigationMode.PATROL
            navState.route.clear()
            navState.routeSegmentIndex = 0
        }
    }
}