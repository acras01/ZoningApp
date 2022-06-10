package ua.od.acros.zoningapp.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.maps.android.ui.IconGenerator
import com.google.maps.android.ui.IconGenerator.*
import com.jakewharton.rxbinding4.view.clicks
import ua.od.acros.zoningapp.misc.data.SavePNGRepositoryImpl
import ua.od.acros.zoningapp.misc.utils.screenValue
import ua.od.acros.zoningapp.vm.MainViewModel
import ua.od.acros.zoningapp.R
import ua.od.acros.zoningapp.databinding.FragmentZonesMapBinding
import ua.od.acros.zoningapp.misc.utils.Building
import java.util.regex.Pattern
import kotlin.collections.ArrayList

class ZonesMapFragment : Fragment() {

    private lateinit var googleMap: GoogleMap

    private var _binding: FragmentZonesMapBinding? = null

    private val binding get() = _binding!!

    private val sharedViewModel: MainViewModel by activityViewModels()

    private val zonesList: ArrayList<Pair<MarkerOptions, AbstractSafeParcelable>> = arrayListOf()

    private var markerShown = -1

    companion object {
        const val FRAGMENT_ID = 1
    }

    private val callback = OnMapReadyCallback { map ->

        val city = sharedViewModel.city.value
        if (city != null) {
            googleMap = map

            val coordinates = city.coordinates.split(",")
            moveCameraToBounds(
                LatLngBounds
                    .builder()
                    .include(LatLng(coordinates[0].toDouble(), coordinates[1].toDouble()))
                    .build()
            )

            val selectedBuildings = sharedViewModel.selectedBuildingsList.value
            if (selectedBuildings != null) {
                googleMap = map

                var i = 1
                var builder: LatLngBounds.Builder
                var bounds: LatLngBounds?
                sharedViewModel.cityZones.value?.forEach { zone ->
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
                                googleMap.addMarker(mo)
                                val po = PolygonOptions()
                                    .addAll(polygon.points)
                                    .strokeColor(polygon.strokeColor)
                                    .strokeWidth(1f)
                                    .fillColor(polygon.fillColor)
                                googleMap.addPolygon(po)
                                zonesList.add(mo to po)
                            }
                            zone.circles.forEach { circle ->
                                val co = CircleOptions()
                                    .center(circle.center)
                                    .radius(circle.radius)
                                    .strokeColor(circle.strokeColor)
                                    .strokeWidth(1f)
                                    .fillColor(circle.fillColor)
                                googleMap.addCircle(co)
                                val mo =
                                    addMarker(circle.center, i++.toString(), style, building.group, zone.name)
                                googleMap.addMarker(mo)
                                zonesList.add(mo to co)
                            }
                        }
                    }
                }

                markerShown = 0
                showZoneWithMarker(zonesList[markerShown].first)

                if (zonesList.size > 1)
                    binding.btnForward.isEnabled = true
            }
        }
    }

    private fun addMarker(
        latLng: LatLng,
        tag: String,
        style: Int,
        group: String,
        name: String): MarkerOptions {

        val iconGen = IconGenerator(context)
        iconGen.setStyle(style)

        return MarkerOptions()
            .icon(BitmapDescriptorFactory.fromBitmap(iconGen.makeIcon(tag)))
            .title(name)
            .snippet(group)
            .position(latLng)
            .anchor(iconGen.anchorU, iconGen.anchorV)
    }

    private fun showZoneWithMarker(markerOptions: MarkerOptions) {
        googleMap.addMarker(markerOptions)
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(markerOptions.position))
        googleMap.animateCamera(CameraUpdateFactory.zoomTo(15f), 2000, null)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentZonesMapBinding.inflate(inflater, container, false)

        binding.btnExportResults.isEnabled = false

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

        binding.btnExportResults.clicks().subscribe {
            val list = sharedViewModel.selectedBuildingsList.value
                ?.map { building ->  mapBuilding(building) }
                ?.toTypedArray()
            sharedViewModel.selectedZone.postValue(zonesList[markerShown].first.title to list)
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
//                        val fileName: String =
//                            "${zonesList[markerShown].first.title}_" + SimpleDateFormat("yyyyMMddHHmm").format(
//                                Date()
//                            )
//                        context?.let { it ->
//                            sharedViewModel.savePNG(
//                                bitmap,
//                                it.getString(R.string.app_name),
//                                fileName
//                            )
//                        }
                    sharedViewModel.mapBitmap.postValue(bitmap)
                    val args = Bundle()
                    args.putInt("id", FRAGMENT_ID)
                    findNavController().navigate(
                        R.id.action_zonesMapFragment_to_HTMLPrintFragment,
                        args
                    )
                }
            }
        }

        sharedViewModel.storagePerm.observe(viewLifecycleOwner) {
            if (it)
                binding.btnExportResults.isEnabled = true
            else
                Toast.makeText(context, R.string.give_storage_permission, Toast.LENGTH_LONG).show()
        }

        sharedViewModel.savePNG.observe(viewLifecycleOwner) { result ->
            var text = ""
            when (result) {
                SavePNGRepositoryImpl.OK -> text = resources.getString(R.string.saved)
                SavePNGRepositoryImpl.ERR -> text = resources.getString(R.string.not_saved)
            }
            Toast.makeText(context, text, Toast.LENGTH_LONG).show()
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
        showZoneWithMarker(zonesList[markerShown].first)
    }

    private fun showPreviousMarker() {
        binding.btnForward.isEnabled = true
        markerShown--
        if (markerShown == 0)
            binding.btnBack.isEnabled = false
        showZoneWithMarker(zonesList[markerShown].first)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(callback)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.onBackPressedDispatcher?.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                findNavController().navigate(R.id.action_zonesMapsFragment_to_chooseBuildingFragment)
            }
        })
        (activity as MainActivity).askForLocationPermission()
        (activity as MainActivity).askForStoragePermission()
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