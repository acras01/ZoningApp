package ua.od.acros.zoningapp.misc.utils

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.geometry.Point
import kotlin.math.*

class EllipticalMercator {
    fun merc(latLng: LatLng): Point {
        return Point(mercX(latLng.longitude), mercY(latLng.latitude))
    }

    fun revMerc(point: Point): LatLng {
        return LatLng(revMercY(point.y), revMercX(point.x))
    }

    private fun revMercY(y: Double): Double {
        val ts = exp(-y / R_MAJOR)
        var phi = Math.PI / 2 - 2 * atan(ts)
        var dphi = 1.0
        var i = 0
        while (abs(dphi) > 0.000000001 && i < 15) {
            val con = ECCENTRIC * sin(phi)
            dphi = Math.PI / 2 - 2 * atan(ts * ((1.0 - con) / (1.0 + con)).pow(COM)) - phi;
            phi += dphi
            i++
        }
        return Math.toDegrees(phi)
    }

    private fun revMercX(x: Double): Double {
        return Math.toDegrees(x) / R_MAJOR
    }

    private fun mercX(lon: Double): Double {
        return R_MAJOR * Math.toRadians(lon)
    }

    private fun mercY(lat: Double): Double {
        val newLat = min(89.5, max(-89.5, lat))
        val phi = Math.toRadians(newLat)
        val sinphi = sin(phi)
        var con = ECCENTRIC * sinphi
        val com = 0.5 * ECCENTRIC
        con = ((1.0 - con) / (1.0 + con)).pow(com)
        val ts = tan(0.5 * (Math.PI * 0.5 - phi)) / con
        return 0 - R_MAJOR * ln(ts)
    }

    companion object {
        private const val R_MAJOR = 6378137.0
        private const val R_MINOR = 6356752.3142
        private const val RATIO = R_MINOR / R_MAJOR
        private val ECCENTRIC = sqrt(1.0 - RATIO * RATIO)
        private val COM = ECCENTRIC / 2
    }
}