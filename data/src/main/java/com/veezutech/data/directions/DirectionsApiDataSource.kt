package com.veezutech.data.directions

import com.veezutech.common.DispatchersProvider
import com.veezutech.data.BuildConfig
import com.veezutech.domain.models.LocationPoint
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Singleton
class DirectionsApiDataSource @Inject constructor(
    private val dispatchers: DispatchersProvider,
) {

    // Minimal Ktor client tuned for Google Routes API v2.
    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                }
            )
        }
    }

    // Execute a POST to /directions/v2:computeRoutes and decode the polyline points.
    suspend fun fetchRoute(
        start: LocationPoint,
        end: LocationPoint,
    ): List<LocationPoint>? = withContext(dispatchers.io) {
        if (BuildConfig.MAPS_API_KEY.isBlank()) return@withContext null

        runCatching {
            val response: DirectionsResponse = client.post(BASE_URL) {
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                header("X-Goog-Api-Key", BuildConfig.MAPS_API_KEY)
                header("X-Goog-FieldMask", FIELD_MASK)
                setBody(
                    DirectionsRequest(
                        origin = Waypoint(
                            location = Location(
                                latLng = LatLngLiteral(start.latitude, start.longitude)
                            )
                        ),
                        destination = Waypoint(
                            location = Location(
                                latLng = LatLngLiteral(end.latitude, end.longitude)
                            )
                        ),
                        travelMode = "DRIVE"
                    )
                )
            }.body()

            val encoded = response.routes.firstOrNull()
                ?.polyline
                ?.encodedPolyline
                .orEmpty()
            if (encoded.isBlank()) return@withContext null
            decodePolyline(encoded)
        }.getOrNull()
    }

    // Standard Google polyline decoder (borrowed from Maps utils).
    private fun decodePolyline(encoded: String): List<LocationPoint> {
        val polyline = mutableListOf<LocationPoint>()
        var index = 0
        val length = encoded.length
        var lat = 0
        var lng = 0

        while (index < length) {
            var result = 0
            var shift = 0
            var b: Int
            do {
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)
            val dLat = if ((result and 1) != 0) (result shr 1).inv() else result shr 1
            lat += dLat

            result = 0
            shift = 0
            do {
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)
            val dLng = if ((result and 1) != 0) (result shr 1).inv() else result shr 1
            lng += dLng

            val point = LocationPoint(
                latitude = lat / 1E5,
                longitude = lng / 1E5
            )
            polyline.add(point)
        }

        return polyline
    }

    @Serializable
    private data class DirectionsRequest(
        val origin: Waypoint,
        val destination: Waypoint,
        val travelMode: String,
    )

    @Serializable
    private data class Waypoint(
        val location: Location,
    )

    @Serializable
    private data class Location(
        val latLng: LatLngLiteral,
    )

    @Serializable
    private data class LatLngLiteral(
        val latitude: Double,
        val longitude: Double,
    )

    @Serializable
    private data class DirectionsResponse(
        val routes: List<Route> = emptyList(),
    )

    @Serializable
    private data class Route(
        val polyline: Polyline? = null,
    )

    @Serializable
    private data class Polyline(
        val encodedPolyline: String = "",
    )

    private companion object {
        private const val BASE_URL = "https://routes.googleapis.com/directions/v2:computeRoutes"
        private const val FIELD_MASK = "routes.polyline.encodedPolyline"
    }
}

