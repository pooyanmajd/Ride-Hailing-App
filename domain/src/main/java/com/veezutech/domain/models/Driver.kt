package com.veezutech.domain.models

data class Driver(
    val id: String,
    val location: LocationPoint,
    val headingDegrees: Float,
    val status: DriverStatus,
)

enum class DriverStatus { AVAILABLE, BUSY, EN_ROUTE }