package ua.od.acros.zoningapp.ui.main

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.jakewharton.rxbinding4.view.clicks
import dagger.hilt.android.AndroidEntryPoint
import ua.od.acros.zoningapp.misc.utils.CustomAdapter
import ua.od.acros.zoningapp.R
import ua.od.acros.zoningapp.databinding.FragmentCitySelectBinding

@AndroidEntryPoint
class CitySelectFragment : Fragment(), AdapterView.OnItemSelectedListener {

    private var _binding: FragmentCitySelectBinding? = null

    private val binding get() = _binding!!

    @SuppressLint("MissingPermission")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_city_select,
            container, false)

        val sharedViewModel = (activity as MainActivity).getViewModel()

        binding.apply {
            activity?.let { MobileAds.initialize(it) }
            val adRequest = AdRequest.Builder().build()
            avSelectFragmentBanner.loadAd(adRequest)

            viewModel = sharedViewModel

            context?.let {
                sharedViewModel.setCityNameList(
                    it.resources.getStringArray(R.array.city_names).toList()
                )
                val spinnerAdapter: CustomAdapter<String> = CustomAdapter(
                    it,
                    R.layout.spinner_item,
                    mutableListOf()
                )

                spinnerAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
                spCities?.adapter = spinnerAdapter
                spCities?.onItemSelectedListener = this@CitySelectFragment
            }

            btnSelect?.isEnabled = false
            btnSelect?.clicks()?.subscribe {
                val id = (spCities?.selectedItemId?.minus(1))?.toInt()
                val city = sharedViewModel.getCityList()[id!!]
                sharedViewModel.setCity(city)
                sharedViewModel.parseBuildings(city)
                sharedViewModel.prepareMapForCity(city)
                findNavController().navigate(R.id.action_selectFragment_to_chooseActionFragment)
            }

            return root
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.onBackPressedDispatcher?.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                activity?.finish()
            }
        })
    }

    @SuppressLint("ResourceAsColor")
    override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
        if (p1 != null) {
            val textView = p1 as TextView
            if (p2 != 1) {
                binding.btnSelect?.isEnabled = false
                textView.setTextColor(R.color.button_text_color_disabled)
            } else {
                binding.btnSelect?.isEnabled = true
            }
        }
    }

    override fun onNothingSelected(p0: AdapterView<*>?) {
        binding.btnSelect?.isEnabled = false
    }
}