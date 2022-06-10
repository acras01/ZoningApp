package ua.od.acros.zoningapp.misc.data

import android.annotation.SuppressLint
import android.app.Application
import android.location.Address
import android.location.Geocoder
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ua.od.acros.zoningapp.misc.repository.LocationRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.suspendCoroutine

@Singleton
class LocationRepositoryImpl @Inject constructor(
    private val application: Application
): LocationRepository {

    private val fusedLocationClient: FusedLocationProviderClient? by lazy {
        LocationServices.getFusedLocationProviderClient(application)
    }

    private var cancellationTokenSource = CancellationTokenSource()

    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocation(): LatLng? {
        var result: LatLng? = null
        suspendCoroutine<LatLng?> { cont ->
            fusedLocationClient?.getCurrentLocation(
                LocationRequest.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.token
            )?.addOnCompleteListener { task: Task<Location> ->
                if (task.isSuccessful) {
                    result = LatLng(task.result.latitude, task.result.longitude)
                    cont.resumeWith(Result.success((result)))
                } else {
                    cont.resumeWith(Result.success(null))
                }
            }
        }
        return result
    }

    override suspend fun getLocationFromAddress(address: String): LatLng? {
        var result: LatLng? = null
        withContext(Dispatchers.IO) {
            val coder = Geocoder(application)
            val addressList: List<Address>?
            try {
                addressList = coder.getFromLocationName(address, 5)
                if (addressList != null) {
                    val location: Address = addressList[0]
                    result = LatLng(location.latitude, location.longitude)
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
        return result
    }
}