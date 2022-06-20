package ua.od.acros.zoningapp.ui.main

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.jakewharton.rxbinding4.view.clicks
import ua.od.acros.zoningapp.misc.utils.Building
import ua.od.acros.zoningapp.misc.utils.CustomAdapter
import ua.od.acros.zoningapp.vm.MainViewModel
import ua.od.acros.zoningapp.R
import ua.od.acros.zoningapp.databinding.FragmentChooseBuildingBinding

class ChooseBuildingFragment : Fragment(), AdapterView.OnItemSelectedListener {

    private var _binding: FragmentChooseBuildingBinding? = null

    private val binding get() = _binding!!

    private val sharedViewModel: MainViewModel by activityViewModels()

    private var typeSelectionMade = false
    private var groupSelectionMade = false

    private var typeSelected = ""
    private var groupSelected = ""

    private lateinit var buildings: List<Building>

    @SuppressLint("MissingPermission")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentChooseBuildingBinding.inflate(inflater, container, false)

        binding.btnFindZone.isEnabled = false
        binding.spBuildingType.isEnabled = false

        this.context?.let { MobileAds.initialize(it) }
        val adRequest = AdRequest.Builder().build()
        binding.avSelectBuildingFragmentBanner.loadAd(adRequest)

        sharedViewModel.mBuildingList.observe(viewLifecycleOwner) { buildingList ->
            if (buildingList != null) {
                buildings = buildingList
                val groupList =
                    context?.resources?.getStringArray(R.array.building_groups)?.toMutableList()

                if (groupList != null) {

                    sharedViewModel.getGroupList()?.let { groupList.addAll(it) }

                    val spinnerAdapter: CustomAdapter<String>? = context?.let { context ->
                        CustomAdapter(
                            context,
                            R.layout.spinner_item,
                            groupList
                        )
                    }

                    spinnerAdapter?.setDropDownViewResource(R.layout.spinner_dropdown_item)
                    binding.spBuildingGroup.adapter = spinnerAdapter
                    binding.spBuildingGroup.onItemSelectedListener = this
                }
            }
        }

        binding.btnFindZone.clicks().subscribe() {
            sharedViewModel.setSelectedBuildingsList(groupSelected, typeSelected)
            findNavController().navigate(R.id.action_chooseBuildingFragment_to_zonesMapsFragment)
        }

        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.onBackPressedDispatcher?.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                findNavController().navigate(R.id.action_chooseBuildingFragment_to_chooseActionFragment)
            }
        })
    }

    override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
        if (p0 != null && p1 != null) {
            val textView = p1 as TextView
            if (p2 == 0) {
                textView.setTextColor(R.color.button_text_color_disabled)
            }
            when (p0) {
                binding.spBuildingGroup -> {
                    groupSelectionMade = p2 > 0
                    binding.spBuildingType.isEnabled = groupSelectionMade
                    if (groupSelectionMade) {
                        groupSelected = textView.text.toString()

                        val typeList =
                            context?.resources?.getStringArray(R.array.building_types)?.toMutableList()

                        if (typeList != null) {
                            sharedViewModel.getTypeListForGroup(groupSelected)
                                ?.let { typeList.addAll(it) }
                            val spinnerAdapter: CustomAdapter<String>? = context?.let { context ->
                                CustomAdapter<String>(
                                    context,
                                    R.layout.spinner_item,
                                    typeList
                                )
                            }
                            spinnerAdapter?.setDropDownViewResource(R.layout.spinner_dropdown_item)
                            binding.spBuildingType.adapter = spinnerAdapter
                            binding.spBuildingType.onItemSelectedListener = this
                        }
                    }
                }
                binding.spBuildingType -> {
                    typeSelectionMade = p2 > 0
                    if (typeSelectionMade)
                        typeSelected = textView.text.toString()
                }
            }
            if (sharedViewModel.mLocationPerm.value == true) {
                binding.btnFindZone.isEnabled = typeSelectionMade && groupSelectionMade
            } else
                Toast.makeText(context, R.string.give_location_permission, Toast.LENGTH_LONG).show()
        }
    }

    override fun onNothingSelected(p0: AdapterView<*>?) {
        typeSelectionMade = false
        groupSelectionMade = false
        binding.btnFindZone.isEnabled = false
    }
}