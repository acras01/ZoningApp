package ua.od.acros.zoningapp.misc.repository

import com.google.android.gms.maps.model.LatLng

interface LocationRepository {
    suspend fun getCurrentLocation(): LatLng?
    suspend fun getLocationFromAddress(address: String): LatLng?
}