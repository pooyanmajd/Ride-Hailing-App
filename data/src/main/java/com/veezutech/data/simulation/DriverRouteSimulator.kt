package com.veezutech.data.simulation

import com.veezutech.domain.models.Driver
import com.veezutech.domain.models.DriverStatus
import com.veezutech.domain.models.LocationPoint
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

@Singleton
class DriverRouteSimulator @Inject constructor() {

    // Scatter evenly spaced waypoints around the rider, then nudge each toward a "road-aligned" heading.
    fun generateWaypoints(center: LocationPoint, count: Int): List<LocationPoint> {
        return List(count) { index ->
            val angle = (360.0 / count) * index + Random.nextDouble(-15.0, 15.0)
            val radius = Random.nextDouble(0.003, 0.008)
            val roadAngle = getNearestRoadDirection(angle)
            val roadAngleRad = Math.toRadians(roadAngle)
            LocationPoint(
                latitude = center.latitude + radius * cos(roadAngleRad),
                longitude = center.longitude + radius * sin(roadAngleRad)
            )
        }
    }

    // Seed synthetic drivers and attach a precomputed patrol route to each.
    suspend fun generateDriversWithRoutes(
        userLocation: LocationPoint,
        waypoints: List<LocationPoint>,
        routeProvider: suspend (LocationPoint, LocationPoint) -> List<LocationPoint>
    ): List<Pair<Driver, List<LocationPoint>>> {
        return List(10) { index ->
            val startPoint = if (Random.nextBoolean() && waypoints.isNotEmpty()) {
                waypoints[Random.nextInt(waypoints.size)]
            } else {
                val radius = Random.nextDouble(0.002, 0.005)
                val angle = Random.nextDouble(0.0, 360.0)
                val angleRad = Math.toRadians(angle)
                LocationPoint(
                    latitude = userLocation.latitude + radius * cos(angleRad),
                    longitude = userLocation.longitude + radius * sin(angleRad)
                )
            }
            val targetWaypoint = waypoints[index % waypoints.size]
            val route = runCatching { routeProvider(startPoint, targetWaypoint) }.getOrDefault(emptyList())
            val driver = Driver(
                id = "D$index",
                location = startPoint,
                headingDegrees = if (route.isNotEmpty()) {
                    calculateBearing(startPoint, route.first()).toFloat()
                } else {
                    Random.nextFloat() * 360f
                },
                status = if (index % 3 == 0) DriverStatus.BUSY else DriverStatus.AVAILABLE
            )
            Pair(driver, route)
        }
    }

    // Rebuild a patrol route once a driver finishes its current path.
    suspend fun ensurePatrolRoute(
        driverLocation: LocationPoint,
        waypoints: List<LocationPoint>,
        routeProvider: suspend (LocationPoint, LocationPoint) -> List<LocationPoint>
    ): MutableList<LocationPoint> {
        val nextWaypointIndex = Random.nextInt(waypoints.size)
        // Fallback to empty list
        val newRoute = runCatching { routeProvider(driverLocation, waypoints[nextWaypointIndex]) }
            .getOrElse { emptyList() }
        return newRoute.toMutableList()
    }

    fun calculateDistance(point1: LocationPoint, point2: LocationPoint): Double {
        val latDiff = point1.latitude - point2.latitude
        val lngDiff = point1.longitude - point2.longitude
        return sqrt(latDiff * latDiff + lngDiff * lngDiff)
    }

    fun calculateBearing(point1: LocationPoint, point2: LocationPoint): Double {
        val lat1 = Math.toRadians(point1.latitude)
        val lat2 = Math.toRadians(point2.latitude)
        val deltaLng = Math.toRadians(point2.longitude - point1.longitude)
        val y = sin(deltaLng) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(deltaLng)
        val bearing = Math.toDegrees(kotlin.math.atan2(y, x))
        return normalizeAngle(bearing)
    }

    fun getNearestRoadDirection(direction: Double): Double {
        val roadDirections = listOf(0.0, 45.0, 90.0, 135.0, 180.0, 225.0, 270.0, 315.0)
        return roadDirections.minByOrNull { abs(normalizeAngle(it - direction)) } ?: direction
    }

    private fun normalizeAngle(angle: Double): Double {
        var normalized = angle % 360.0
        if (normalized < 0) normalized += 360.0
        return normalized
    }

    fun metersToDegrees(meters: Double): Double = meters / 111_000.0

    fun interpolate(start: LocationPoint, end: LocationPoint, ratio: Double): LocationPoint {
        val lat = (end.latitude - start.latitude) * ratio + start.latitude
        val lng = (end.longitude - start.longitude) * ratio + start.longitude
        return LocationPoint(lat, lng)
    }
}

data class NavigationState(
    val route: MutableList<LocationPoint>,
    var routeSegmentIndex: Int = 0,
    var currentDirection: Double,
    var mode: NavigationMode,
)

enum class NavigationMode {
    PATROL,
    PICKUP,
}

