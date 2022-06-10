package ua.od.acros.zoningapp.misc.utils

import com.google.android.gms.maps.model.LatLng

data class Zone(
    val name: String,
    val polygons: List<Polygon>,
    val circles: List<Circle>
) {
    class Circle(
        val center: LatLng,
        val radius: Double,
        val fillColor: Int,
        val strokeColor: Int
    )

    class Polygon(
        val points: List<LatLng>,
        val fillColor: Int,
        val strokeColor: Int
    )
}


