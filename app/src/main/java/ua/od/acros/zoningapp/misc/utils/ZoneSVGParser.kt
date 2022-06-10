package ua.od.acros.zoningapp.misc.utils

import android.graphics.Color
import android.util.Xml
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.geometry.Point
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream


class ZoneSVGParser {

    // We don't use namespaces
    private val ns: String? = null

    private val mercator = EllipticalMercator()

    private var offsetX: Double = 0.0
    private var offsetY: Double = 0.0
    private var scaleX: Double = 1.0
    private var scaleY: Double = 1.0

    fun parse(inputStream: InputStream): List<Zone> {
        val parser: XmlPullParser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(inputStream, null)
        parser.nextTag()
        return parseZonesFromSVG(parser)
    }

    private fun parseZonesFromSVG(parser: XmlPullParser): List<Zone> {
        val zones = mutableListOf<Zone>()
        parser.require(XmlPullParser.START_TAG, ns, "svg")
        offsetX = parser.getAttributeValue(null, "offsetX").toDouble()
        offsetY = parser.getAttributeValue(null, "offsetY").toDouble()
        scaleX = parser.getAttributeValue(null, "scaleX").toDouble()
        scaleY = parser.getAttributeValue(null, "scaleY").toDouble()
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }

            // Starts by looking for the entry tag
            if (parser.name == "g") {
                zones.add(readZone(parser))
            } else {
                skip(parser)
            }
        }
        return zones
    }

    private fun skip(parser: XmlPullParser) {
        if (parser.eventType != XmlPullParser.START_TAG) {
            throw IllegalStateException()
        }
        var depth = 1
        while (depth != 0) {
            when (parser.next()) {
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.START_TAG -> depth++
            }
        }
    }

    private fun readZone(parser: XmlPullParser): Zone {
        parser.require(XmlPullParser.START_TAG, ns, "g")
        val name = parser.getAttributeValue(null, "id")
        val polygons: MutableList<Zone.Polygon> = mutableListOf()
        val circles: MutableList<Zone.Circle> = mutableListOf()
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            when (parser.name) {
                "polygon" ->  {
                    polygons.add(readPolygon(parser))
                }
                "poligon" ->  {
                    polygons.add(readPoligon(parser))
                }
                "circle" ->  {
                    circles.add(readCircle(parser))
                }
                else -> skip(parser)
            }
        }
        return Zone(name, polygons, circles)
    }

    private fun readPolygon(parser: XmlPullParser): Zone.Polygon {
        parser.require(XmlPullParser.START_TAG, ns, "polygon")
        val fill = hex2aRGB(parser.getAttributeValue(null, "fill"))
        val stroke = hex2aRGB(parser.getAttributeValue(null, "stroke"))
        val pointsStr = parser.getAttributeValue(null, "points")
        val points = mutableListOf<LatLng>()
        pointsStr.split(" ").forEach { point ->
            val coords = point.split(",")
            if (coords.size == 2) {
                val x = coords[0].toDouble() / scaleX + offsetX
                val y = offsetY - coords[1].toDouble() / scaleY
                points.add(mercator.revMerc(Point(x, y)))
            }
        }
        parser.next()
        parser.require(XmlPullParser.END_TAG, ns, "polygon")
        return Zone.Polygon(points, fill, stroke)
    }

    private fun readPoligon(parser: XmlPullParser): Zone.Polygon {
        parser.require(XmlPullParser.START_TAG, ns, "poligon")
        val fill = hex2aRGB(parser.getAttributeValue(null, "fill"))
        val stroke = hex2aRGB(parser.getAttributeValue(null, "stroke"))
        val pointsStr = parser.getAttributeValue(null, "points")
        val points = mutableListOf<LatLng>()
        pointsStr.split(", ").forEach { point ->
            val coords = point.split(" ")
            if (coords.size == 2) {
                val x = coords[0].toDouble() / scaleX + offsetX
                val y = offsetY - coords[1].toDouble() / scaleY
                points.add(mercator.revMerc(Point(y, x)))
            }
        }
        parser.next()
        parser.require(XmlPullParser.END_TAG, ns, "poligon")
        return Zone.Polygon(points, fill, stroke)
    }

    private fun readCircle(parser: XmlPullParser): Zone.Circle {
        parser.require(XmlPullParser.START_TAG, ns, "circle")
        val fill = hex2aRGB(parser.getAttributeValue(null, "fill"))
        val stroke = hex2aRGB(parser.getAttributeValue(null, "stroke"))
        val xStr = parser.getAttributeValue(null, "cx")
        val yStr = parser.getAttributeValue(null, "cx")
        val x = xStr.toDouble() / scaleX + offsetX
        val y = offsetY - yStr.toDouble() / scaleY
        val center = mercator.revMerc(Point(x, y))
        val radius = parser.getAttributeValue(null, "r").toDouble()
        parser.next()
        parser.require(XmlPullParser.END_TAG, ns, "circle")
        return Zone.Circle(center, radius, fill, stroke)
    }

    /**
     *
     * @param colorStr e.g. "#FFFFFF"
     * @return
     */
    private fun hex2aRGB(colorStr: String): Int {
        return Color.argb(
            Integer.valueOf("7F", 16),
            Integer.valueOf(colorStr.substring(1, 3), 16),
            Integer.valueOf(colorStr.substring(3, 5), 16),
            Integer.valueOf(colorStr.substring(5, 7), 16)
        )
    }
}