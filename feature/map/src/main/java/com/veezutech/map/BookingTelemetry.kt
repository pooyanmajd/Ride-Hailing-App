package com.veezutech.map

import android.util.Log
import com.veezutech.domain.models.LocationPoint
import javax.inject.Inject

interface BookingTelemetry {
    fun onBookingAssigned(driverId: String, pickupLocation: LocationPoint)
    fun onBookingCancelled(driverId: String?)
}

class LogcatBookingTelemetry @Inject constructor() : BookingTelemetry {

    override fun onBookingAssigned(driverId: String, pickupLocation: LocationPoint) {
        Log.d(TAG, "Assigned $driverId to pickup at ${pickupLocation.latitude},${pickupLocation.longitude}")
    }

    override fun onBookingCancelled(driverId: String?) {
        Log.d(TAG, "Cancelled booking for driver=${driverId ?: "none"}")
    }

    private companion object {
        private const val TAG = "MapBooking"
    }
}

