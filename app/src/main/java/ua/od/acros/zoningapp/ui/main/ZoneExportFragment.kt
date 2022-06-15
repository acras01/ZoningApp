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
import com.jakewharton.rxbinding4.view.clicks
import ua.od.acros.zoningapp.R
import ua.od.acros.zoningapp.databinding.FragmentZoneExportBinding
import ua.od.acros.zoningapp.vm.MainViewModel

class ZoneExportFragment : Fragment() {

    private val sharedViewModel: MainViewModel by activityViewModels()

    private var _binding: FragmentZoneExportBinding? = null

    private val binding get() = _binding!!

    companion object {
        const val FRAGMENT_ID = 2
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentZoneExportBinding.inflate(inflater, container, false)

        binding.btnExportResults.isEnabled = false

        this.context?.let { MobileAds.initialize(it) }
        val adRequest = AdRequest.Builder().build()
        binding.avZoneExportFragmentBanner.loadAd(adRequest)

        sharedViewModel.mSelectedZone.observe(viewLifecycleOwner) { zone ->
            if(zone.first != null && zone.second != null) {
                binding.tvSelectedZone.text = getString(R.string.selected_zones, zone.first)
                binding.tvZoneDesc2.text = zone.second!![0]
                binding.tvZoneDesc4.text = zone.second!![1]
                binding.tvZoneDesc6.text = zone.second!![2]
            }
        }

        sharedViewModel.mStoragePerm.observe(viewLifecycleOwner) {
            if (it == true)
                binding.btnExportResults.isEnabled = true
            else
                Toast.makeText(context, R.string.give_storage_permission, Toast.LENGTH_LONG).show()
        }

        binding.btnNewSearch.clicks().subscribe {
            findNavController().navigate(R.id.action_zonesMapsFragment_to_selectFragment)
        }

        binding.btnExportResults.clicks().subscribe {
            //findNavController().navigate(R.id.action_zoneExportFragment_to_exportSettingsFragment)
            val args = Bundle()
            args.putInt("id", FRAGMENT_ID)
            findNavController().navigate(
                R.id.action_zoneExportFragment_to_HTMLPrintFragment,
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

        (activity as MainActivity).askForStoragePermission()
    }
}