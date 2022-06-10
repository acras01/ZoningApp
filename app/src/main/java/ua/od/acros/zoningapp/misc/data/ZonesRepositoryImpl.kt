package ua.od.acros.zoningapp.misc.data

import android.app.Application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ua.od.acros.zoningapp.misc.utils.ZoneSVGParser
import ua.od.acros.zoningapp.misc.repository.ZonesRepository
import ua.od.acros.zoningapp.misc.utils.Zone
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ZonesRepositoryImpl @Inject constructor(
    private val application: Application
): ZonesRepository {

    override suspend fun getZones(filename: String): List<Zone>? {
        var zones: List<Zone>? = listOf<Zone>()
        withContext(Dispatchers.IO) {
            try {
                val stream = application.assets.open(filename)
                zones = stream.let { inputStream ->  ZoneSVGParser().parse(inputStream) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return zones
    }
}