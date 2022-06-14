package ua.od.acros.zoningapp.misc.repository

import ua.od.acros.zoningapp.misc.utils.Zone

interface ZonesRepository {
    suspend fun getZones(cityName: String): List<Zone>?
}