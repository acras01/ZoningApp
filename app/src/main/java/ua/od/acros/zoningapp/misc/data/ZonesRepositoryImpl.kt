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

    override suspend fun getZones(cityName: String): List<Zone> {
        val zones: MutableList<Zone> = mutableListOf<Zone>()
        withContext(Dispatchers.IO) {
            val fileList: Array<String>?
            try {
                fileList = application.assets.list("")
                if (fileList != null && fileList.isNotEmpty()) {
                    fileList.forEach { file ->
                        if (file.contains(cityName) && file.contains("svg")) {
                            val stream = application.assets.open(file)
                            val curZones = stream.let { inputStream -> ZoneSVGParser().parse(inputStream) }
                            zones.addAll(curZones)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return zones
    }
}