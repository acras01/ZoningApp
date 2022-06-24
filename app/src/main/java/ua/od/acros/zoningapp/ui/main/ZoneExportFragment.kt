package ua.od.acros.zoningapp.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.jakewharton.rxbinding4.view.clicks
import ua.od.acros.zoningapp.R
import ua.od.acros.zoningapp.databinding.FragmentZoneExportBinding

class ZoneExportFragment : Fragment() {

    private var _binding: FragmentZoneExportBinding? = null

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_zone_export,
            container, false)

        val sharedViewModel = (activity as MainActivity).getViewModel()

        binding.viewModel = sharedViewModel

        this.context?.let { MobileAds.initialize(it) }
        val adRequest = AdRequest.Builder().build()
        binding.avZoneExportFragmentBanner.loadAd(adRequest)

        binding.btnNewSearch.clicks().subscribe {
            findNavController().navigate(R.id.action_global_selectFragment)
        }

        binding.btnExportResults.clicks().subscribe {
            val args = Bundle()
            args.putInt("fragment_id", HTMLPrintFragment.ZONE_ON_MAP)
            findNavController().navigate(
                R.id.action_global_HTMLPrintFragment,
                args
            )
        }

        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.onBackPressedDispatcher?.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                findNavController().navigate(R.id.action_zoneExportFragment_to_selectZoneOnMapFragment)
            }
        })
    }
}