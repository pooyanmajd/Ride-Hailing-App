package com.veezutech.domain.repository

import com.veezutech.domain.models.LocationPoint
import kotlinx.coroutines.flow.Flow

interface LocationRepository {
    fun observeCurrentLocation(): Flow<LocationPoint>
}