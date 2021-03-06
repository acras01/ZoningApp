package ua.od.acros.zoningapp.ui.main

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.navigation.fragment.findNavController
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.jakewharton.rxbinding4.view.clicks
import ua.od.acros.zoningapp.R
import ua.od.acros.zoningapp.databinding.FragmentChooseActionBinding

class ChooseActionFragment : Fragment() {

    private var _binding: FragmentChooseActionBinding? = null

    private val binding get() = _binding!!

    @SuppressLint("MissingPermission")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChooseActionBinding.inflate(inflater, container, false)

        val sharedViewModel = (activity as MainActivity).getViewModel()

        binding.apply {
            activity?.let { MobileAds.initialize(it) }
            val adRequest = AdRequest.Builder().build()
            avActionFragmentBanner.loadAd(adRequest)

            val onCheckPlot: () -> Unit = {
                if (sharedViewModel.mLocationPerm.value == true) {
                    findNavController().navigate(R.id.action_chooseActionFragment_to_mapsFragment)
                } else
                    Toast.makeText(context, R.string.give_location_permission, Toast.LENGTH_LONG)
                        .show()
            }
            tvCheckMyPlot.clicks().subscribe { onCheckPlot() }

            val onRecommendPlot: () -> Unit = {
                findNavController().navigate(R.id.action_chooseActionFragment_to_chooseBuildingFragment)
            }
            tvRecommendPlot.clicks().subscribe { onRecommendPlot() }

            return root
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.onBackPressedDispatcher?.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                findNavController().navigate(R.id.action_global_selectFragment)
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}