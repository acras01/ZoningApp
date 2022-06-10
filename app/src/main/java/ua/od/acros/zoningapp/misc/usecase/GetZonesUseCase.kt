package ua.od.acros.zoningapp.misc.usecase

import ua.od.acros.zoningapp.misc.utils.Zone
import ua.od.acros.zoningapp.misc.repository.ZonesRepository
import javax.inject.Inject

class GetZonesUseCase @Inject constructor(private val repository: ZonesRepository) {
    suspend fun execute(file: String): List<Zone>? = repository.getZones(file)
}