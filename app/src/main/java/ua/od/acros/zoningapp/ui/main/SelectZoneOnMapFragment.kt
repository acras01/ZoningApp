package ua.od.acros.zoningapp.ui.main

import android.location.Location
import android.location.LocationManager
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
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.maps.android.PolyUtil
import com.google.maps.android.ktx.awaitMap
import com.google.maps.android.ui.IconGenerator
import com.google.maps.android.ui.IconGenerator.STYLE_WHITE
import com.jakewharton.rxbinding4.view.clicks
import dagger.hilt.android.AndroidEntryPoint
import ua.od.acros.zoningapp.misc.utils.Building
import ua.od.acros.zoningapp.misc.utils.Zone
import ua.od.acros.zoningapp.misc.utils.screenValue
import ua.od.acros.zoningapp.R
import ua.od.acros.zoningapp.databinding.FragmentSelectZoneOnMapBinding
import ua.od.acros.zoningapp.vm.MainViewModel

@AndroidEntryPoint
class SelectZoneOnMapFragment : Fragment() {

    private lateinit var sharedViewModel: MainViewModel

    private lateinit var googleMap: GoogleMap

    private var marker: Marker? = null

    private var zones: List<Zone>? = null

    private var buildings: List<Building>? = null

    private val onMapClickListener = GoogleMap.OnMapClickListener { latLng ->
        if (zones != null)
            addMarker(latLng)
    }

    private var _binding: FragmentSelectZoneOnMapBinding? = null

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentSelectZoneOnMapBinding.inflate(inflater, container, false)

        binding.apply {
            btnEnterAddress.isEnabled = false
            btnCurrentLocation.isEnabled = false
            btnShowSelectedZone.isEnabled = false

            sharedViewModel = (activity as MainActivity).getViewModel()

            sharedViewModel.mCityZones.observe(viewLifecycleOwner) { zones ->
                this@SelectZoneOnMapFragment.zones = zones
                btnEnterAddress.isEnabled = true
                btnCurrentLocation.isEnabled = sharedViewModel.mLocationPerm.value == true
            }

            sharedViewModel.mBuildingList.observe(viewLifecycleOwner) { buildings ->
                this@SelectZoneOnMapFragment.buildings = buildings
            }

            sharedViewModel.mLocation.observe(viewLifecycleOwner) { location ->
                if (::googleMap.isInitialized && location != null) {
                    addMarker(location)
                }
            }

            btnEnterAddress.clicks().subscribe {
                val dialog = EnterAddressDialogFragment()
                activity?.let { a -> dialog.show(a.supportFragmentManager, "EnterAddressDialogFragment") }
            }

            btnCurrentLocation.clicks().subscribe {
                sharedViewModel.requestCurrentLocation()
            }

            btnShowSelectedZone.clicks().subscribe {
                val result = sharedViewModel.getInfoForZone(marker?.title)
                sharedViewModel.setSelectedZone(marker?.title to result)
                googleMap.snapshot { bitmap ->
                    sharedViewModel.setBitmap(bitmap)
                }
                findNavController().navigate(R.id.action_selectZoneOnMapFragment_to_zoneExportFragment)
            }

            activity?.let { MobileAds.initialize(it) }
            val adRequest = AdRequest.Builder().build()
            avMapsFragmentBanner.loadAd(adRequest)

            return root
        }
    }

    private fun addMarker(latLng: LatLng) {
        googleMap.clear()
        val tag = drawZone(latLng)
        if (tag != "") {
            val iconGen = IconGenerator(context)
            iconGen.setStyle(STYLE_WHITE)
            marker = googleMap.addMarker(
                MarkerOptions()
                    .icon(BitmapDescriptorFactory.fromBitmap(iconGen.makeIcon(tag)))
                    .title(tag)
                    .position(latLng)
                    .anchor(iconGen.anchorU, iconGen.anchorV)
            )
            binding.btnShowSelectedZone.isEnabled = true
        }
    }

    private fun drawZone(latLng: LatLng): String {
        var title = ""
        zones?.forEach { zone ->
            zone.polygons.forEach Polygons@{ polygon ->
                if (PolyUtil.containsLocation(latLng, polygon.points, false)) {
                    googleMap.addPolygon(
                        PolygonOptions()
                            .addAll(polygon.points)
                            .strokeColor(polygon.strokeColor)
                        //.fillColor(polygon.fillColor)
                    )
                    title = zone.name
                    return@Polygons
                }
            }
            zone.circles.forEach Circles@{ circle ->
                val centerLocation = Location(LocationManager.GPS_PROVIDER)
                    .apply {
                        latitude = circle.center.latitude
                        longitude = circle.center.longitude
                    }
                val currentLocation = Location(LocationManager.GPS_PROVIDER)
                    .apply {
                    latitude = latLng.latitude
                    longitude = latLng.longitude
                    }
                if (currentLocation.distanceTo(centerLocation) <= circle.radius) {
                    googleMap.addCircle(
                        CircleOptions()
                            .center(circle.center)
                            .radius(circle.radius)
                            .strokeColor(circle.strokeColor)
                        //.fillColor(circle.fillColor)
                    )
                    title = zone.name
                    return@Circles
                }
            }
        }
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng))
        googleMap.animateCamera(CameraUpdateFactory.zoomTo(15f), 2000, null)
        return title
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycleScope.launchWhenCreated {
            val mapFragment =
                childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
            val map = mapFragment?.awaitMap()
            if (map != null) {
                googleMap = map
                googleMap.setOnMapClickListener(onMapClickListener)

                val city = sharedViewModel.mCity.value
                if (city != null) {
                    val coordinates = city.coordinates.split(",")
                    moveCameraToBounds(
                        LatLngBounds
                            .builder()
                            .include(LatLng(coordinates[0].toDouble(), coordinates[1].toDouble()))
                            .build()
                    )
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.onBackPressedDispatcher?.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                findNavController().navigate(R.id.action_global_chooseActionFragment)
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}