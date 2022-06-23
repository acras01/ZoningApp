package ua.od.acros.zoningapp.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.maps.android.ktx.awaitMap
import com.google.maps.android.ui.IconGenerator
import com.google.maps.android.ui.IconGenerator.*
import com.jakewharton.rxbinding4.view.clicks
import ua.od.acros.zoningapp.misc.utils.screenValue
import ua.od.acros.zoningapp.R
import ua.od.acros.zoningapp.databinding.FragmentZonesMapBinding
import ua.od.acros.zoningapp.misc.utils.Building
import ua.od.acros.zoningapp.vm.MainViewModel
import java.util.regex.Pattern
import kotlin.collections.ArrayList

class ZonesMapFragment : Fragment() {

    private lateinit var googleMap: GoogleMap

    private var _binding: FragmentZonesMapBinding? = null

    private val binding get() = _binding!!

    private lateinit var sharedViewModel: MainViewModel

    private val zonesList: ArrayList<Pair<MarkerOptions, AbstractSafeParcelable>> = arrayListOf()

    private var markerShown = -1

    private fun addMarker(
        latLng: LatLng,
        tag: String,
        style: Int,
        group: String,
        name: String): MarkerOptions {

        val iconGen = IconGenerator(context)
        iconGen.setStyle(style)

        return MarkerOptions().apply {
            icon(BitmapDescriptorFactory.fromBitmap(iconGen.makeIcon(tag)))
            title(name)
            snippet(group)
            position(latLng)
            anchor(iconGen.anchorU, iconGen.anchorV)
        }
    }

    private fun showZoneWithMarker(zone: Pair<MarkerOptions, AbstractSafeParcelable>) {
        googleMap.apply {
            clear()
            addMarker(zone.first)
            if (zone.second is PolygonOptions)
                addPolygon(zone.second as PolygonOptions)
            else if (zone.second is CircleOptions)
                addCircle(zone.second as CircleOptions)
            moveCamera(CameraUpdateFactory.newLatLng(zone.first.position))
            animateCamera(CameraUpdateFactory.zoomTo(15f), 2000, null)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentZonesMapBinding.inflate(inflater, container, false)

        binding.btnNewSearch.clicks().subscribe {
            findNavController().navigate(R.id.action_zonesMapsFragment_to_selectFragment)
        }

        binding.btnBack.isEnabled = false
        binding.btnBack.clicks().subscribe {
            showPreviousMarker()
        }
        binding.btnForward.isEnabled = false
        binding.btnForward.clicks().subscribe {
            showNextMarker()
        }

        sharedViewModel = (activity as MainActivity).getViewModel()

        binding.btnExportResults.clicks().subscribe {
            val list = sharedViewModel.mSelectedBuildingsList.value
                ?.map { building ->  mapBuilding(building) }
                ?.toTypedArray()
            sharedViewModel.setSelectedZone(zonesList[markerShown].first.title to list)
            val area = zonesList[markerShown].second
            if (area is PolygonOptions) {
                area.fillColor(0x7F00FF00)
                googleMap.addPolygon(area)
            } else if (area is CircleOptions) {
                area.fillColor(0x7F00FF00)
                googleMap.addCircle(area)
            }
            googleMap.snapshot { bitmap ->
                if (bitmap != null) {
                    sharedViewModel.setBitmap(bitmap)
                    val args = Bundle()
                    args.putInt("fragment_id", HTMLPrintFragment.ZONE_FOR_PURPOSE)
                    findNavController().navigate(
                        R.id.action_zonesMapFragment_to_HTMLPrintFragment,
                        args
                    )
                }
            }
        }

        this.context?.let { MobileAds.initialize(it) }
        val adRequest = AdRequest.Builder().build()
        binding.avZonesMapFragmentBanner.loadAd(adRequest)

        return binding.root
    }

    private fun mapBuilding(building: Building): String {
        val addition = if (building.misc != "") " (${building.misc})" else ""
        return "${building.group}$addition"
    }

    private fun showNextMarker() {
        binding.btnBack.isEnabled = true
        markerShown++
        if (markerShown == zonesList.size - 1)
            binding.btnForward.isEnabled = false
        showZoneWithMarker(zonesList[markerShown])
    }

    private fun showPreviousMarker() {
        binding.btnForward.isEnabled = true
        markerShown--
        if (markerShown == 0)
            binding.btnBack.isEnabled = false
        showZoneWithMarker(zonesList[markerShown])
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycleScope.launchWhenCreated {
            val mapFragment =
                childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
            val map = mapFragment?.awaitMap()
            if (map != null) {
                googleMap = map
                val city = sharedViewModel.mCity.value
                if (city != null) {
                    val coordinates = city.coordinates.split(",")
                    moveCameraToBounds(
                        LatLngBounds
                            .builder()
                            .include(
                                LatLng(
                                    coordinates[0].toDouble(),
                                    coordinates[1].toDouble()
                                )
                            )
                            .build()
                    )
                }
            }

            val selectedBuildings = sharedViewModel.mSelectedBuildingsList.value
            if (selectedBuildings != null) {
                var i = 1
                var builder: LatLngBounds.Builder
                var bounds: LatLngBounds?
                sharedViewModel.mCityZones.value?.forEach { zone ->
                    selectedBuildings.forEach { building ->
                        val code: StringBuilder = StringBuilder(building.toString())
                        val regex = Regex("code_[0-9]*=")
                        val codeStr = code.delete(0, 17)
                            .delete(code.indexOf("group"), code.length)
                            .replace(regex, "")
                            .split(", ")
                            .toSet()
                        val pattern = Pattern.compile("${zone.name}[А-Я]")
                        if (codeStr.any{ e -> pattern.matcher(e).matches() })  {
                            var style = STYLE_DEFAULT
                            var name = zone.name
                            if (codeStr.contains("${name}П")) {
                                style = STYLE_GREEN
                                name += "П"
                            } else if (codeStr.contains("${name}С")) {
                                style = STYLE_ORANGE
                                name += "С"
                            } else if (codeStr.contains("${name}Д")) {
                                style = STYLE_BLUE
                                name += "Д"
                            }

                            zone.polygons.forEach { polygon ->
                                builder = LatLngBounds.builder()
                                polygon.points.forEach { point ->
                                    builder.include(point)
                                }
                                bounds = builder.build()
                                val mo =
                                    addMarker(bounds!!.center, i++.toString(), style, building.group, name)
                                val po = PolygonOptions().apply {
                                    addAll(polygon.points)
                                    strokeColor(polygon.strokeColor)
                                    strokeWidth(1f)
                                    fillColor(polygon.fillColor)
                                }
                                zonesList.add(mo to po)
                            }
                            zone.circles.forEach { circle ->
                                val co = CircleOptions().apply {
                                    center(circle.center)
                                    radius(circle.radius)
                                    strokeColor(circle.strokeColor)
                                    strokeWidth(1f)
                                    fillColor(circle.fillColor)
                                }
                                val mo =
                                    addMarker(circle.center, i++.toString(), style, building.group, zone.name)
                                zonesList.add(mo to co)
                            }
                        }
                    }
                }

                markerShown = 0
                showZoneWithMarker(zonesList[markerShown])

                if (zonesList.size > 1)
                    binding.btnForward.isEnabled = true
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.onBackPressedDispatcher?.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                findNavController().navigate(R.id.action_zonesMapsFragment_to_chooseBuildingFragment)
            }
        })
    }

    private fun moveCameraToBounds(bounds: LatLngBounds) {
        val metrics = this.activity?.let { screenValue(it) }
        val cu = CameraUpdateFactory.newLatLngBounds(
            bounds, metrics?.get(0) ?: 720,
            metrics?.get(1) ?: 1280, 200
        )
        googleMap.moveCamera(cu)
        googleMap.animateCamera(CameraUpdateFactory.zoomTo(10f), 2000, null)
    }
}