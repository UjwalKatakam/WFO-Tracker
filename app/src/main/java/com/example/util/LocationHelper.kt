package com.example.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class GeofenceStatus(
    val distanceKm: Double,
    val isWithinRadius: Boolean,
    val userLat: Double,
    val userLon: Double,
    val message: String
)

object LocationHelper {

    fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return (results[0] / 1000.0)
    }

    @SuppressLint("MissingPermission")
    suspend fun checkGeofence(
        context: Context,
        officeLat: Double,
        officeLon: Double,
        radiusKm: Double
    ): GeofenceStatus? {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        val cts = CancellationTokenSource()

        return suspendCancellableCoroutine { continuation ->
            try {
                fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                    .addOnSuccessListener { location: Location? ->
                        if (location != null) {
                            val dist = calculateDistanceKm(
                                location.latitude,
                                location.longitude,
                                officeLat,
                                officeLon
                            )
                            val isWithin = dist <= radiusKm
                            val msg = if (isWithin) {
                                String.format("Verified at Office! (%.2f km from coordinates)", dist)
                            } else {
                                String.format("Outside Office Geofence: %.2f km away (Allowed: %.1f km)", dist, radiusKm)
                            }
                            continuation.resume(
                                GeofenceStatus(
                                    distanceKm = dist,
                                    isWithinRadius = isWithin,
                                    userLat = location.latitude,
                                    userLon = location.longitude,
                                    message = msg
                                )
                            )
                        } else {
                            continuation.resume(null)
                        }
                    }
                    .addOnFailureListener {
                        continuation.resume(null)
                    }
            } catch (e: SecurityException) {
                continuation.resume(null)
            } catch (e: Exception) {
                continuation.resume(null)
            }
        }
    }
}
