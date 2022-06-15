package ua.od.acros.zoningapp.ui.main

import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.maps.android.PolyUtil
import com.google.maps.android.ui.IconGenerator
import com.google.maps.android.ui.IconGenerator.STYLE_WHITE
import com.jakewharton.rxbinding4.view.clicks
import dagger.hilt.android.AndroidEntryPoint
import ua.od.acros.zoningapp.misc.utils.Building
import ua.od.acros.zoningapp.misc.utils.Zone
import ua.od.acros.zoningapp.misc.utils.screenValue
import ua.od.acros.zoningapp.vm.MainViewModel
import ua.od.acros.zoningapp.R
import ua.od.acros.zoningapp.databinding.FragmentSelectZoneOnMapBinding

@AndroidEntryPoint
class SelectZoneOnMapFragment : Fragment() {

    private lateinit var googleMap: GoogleMap

    private var marker: Marker? = null

    private var zones: List<Zone>? = null

    private var buildings: List<Building>? = null

    private val sharedViewModel: MainViewModel by activityViewModels()

    private val onMapClickListener = GoogleMap.OnMapClickListener { latLng ->
        if (zones != null)
            addMarker(latLng)
    }

    private val callback = OnMapReadyCallback { map ->
        val city = sharedViewModel.mCity.value
        if (city != null) {
            googleMap = map
            googleMap.setOnMapClickListener(onMapClickListener)

            val coordinates = city.coordinates.split(",")
            moveCameraToBounds(
                LatLngBounds
                .builder()
                .include(LatLng(coordinates[0].toDouble(), coordinates[1].toDouble()))
                .build())
        }
    }

    private var _binding: FragmentSelectZoneOnMapBinding? = null

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentSelectZoneOnMapBinding.inflate(inflater, container, false)

        binding.etEnterAddress.isEnabled = false
        binding.btnCurrentLocation.isEnabled = false
        binding.btnShowSelectedZone.isEnabled = false

        sharedViewModel.mCityZones.observe(viewLifecycleOwner) { zones ->
            this.zones = zones
            binding.etEnterAddress.isEnabled = true
            binding.btnCurrentLocation.isEnabled = sharedViewModel.mLocationPerm.value == true
        }

        sharedViewModel.mBuildingList.observe(viewLifecycleOwner) { buildings ->
            this.buildings = buildings
        }

        binding.etEnterAddress.setOnEditorActionListener(TextView.OnEditorActionListener { view, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val address = "${sharedViewModel.mCity.value?.city} ${view.text}"
                sharedViewModel.getLocationFromAddress(address)
                //return@OnEditorActionListener true
            }
            return@OnEditorActionListener false
        })

        sharedViewModel.mLocation.observe(viewLifecycleOwner) { location ->
            if (::googleMap.isInitialized && location != null) {
                addMarker(location)
            }
        }

        binding.btnCurrentLocation.clicks().subscribe {
            sharedViewModel.requestCurrentLocation()
        }

        binding.btnShowSelectedZone.clicks().subscribe {
            val result = sharedViewModel.getInfoForZone(marker?.title)
            sharedViewModel.setSelectedZone(marker?.title to result)
            googleMap.snapshot { bitmap ->
                sharedViewModel.setBitmap(bitmap)
            }
            (activity as MainActivity).askForStoragePermission()
            findNavController().navigate(R.id.action_selectZoneOnMapFragment_to_zoneExportFragment)
        }

        this.context?.let { MobileAds.initialize(it) }
        val adRequest = AdRequest.Builder().build()
        binding.avMapsFragmentBanner.loadAd(adRequest)

        return binding.root
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

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(callback)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        (activity as MainActivity).askForLocationPermission()

        activity?.onBackPressedDispatcher?.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                findNavController().navigate(R.id.action_selectZoneOnMapFragment_to_chooseActionFragment)
            }
        })
    }
}