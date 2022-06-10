package ua.od.acros.zoningapp.misc.usecase

import com.google.android.gms.maps.model.LatLng
import ua.od.acros.zoningapp.misc.repository.LocationRepository
import javax.inject.Inject

class GetCurrentLocationUseCase @Inject constructor(private val repository: LocationRepository) {
    suspend fun execute(): LatLng? = repository.getCurrentLocation()
}