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
import androidx.navigation.fragment.findNavController
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.jakewharton.rxbinding4.view.clicks
import ua.od.acros.zoningapp.misc.utils.CustomAdapter
import ua.od.acros.zoningapp.R
import ua.od.acros.zoningapp.databinding.FragmentChooseBuildingBinding
import ua.od.acros.zoningapp.misc.utils.Building
import ua.od.acros.zoningapp.vm.MainViewModel

class ChooseBuildingFragment : Fragment(), AdapterView.OnItemSelectedListener {

    private lateinit var sharedViewModel: MainViewModel

    private var _binding: FragmentChooseBuildingBinding? = null

    private val binding get() = _binding!!

    private var typeSelectionMade = false
    private var groupSelectionMade = false

    private var typeSelected = ""
    private var groupSelected = ""
    private var typePosition = 0
    private var groupPosition = 0


    private lateinit var buildings: List<Building>

    @SuppressLint("MissingPermission")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentChooseBuildingBinding.inflate(inflater, container, false)

        sharedViewModel = (activity as MainActivity).getViewModel()

        binding.apply {
            btnFindZone.isEnabled = false
            spBuildingType.isEnabled = false

            activity?.let { MobileAds.initialize(it) }
            val adRequest = AdRequest.Builder().build()
            avSelectBuildingFragmentBanner.loadAd(adRequest)

            sharedViewModel.mBuildingList.observe(viewLifecycleOwner) { buildingList ->
                if (buildingList != null) {
                    buildings = buildingList
                    val groupList = ArrayList<String>()
                    context?.resources?.getStringArray(R.array.building_groups)?.toList()
                        ?.let { groupList.addAll(it) }

                    sharedViewModel.getGroupList()?.let { groupList.addAll(it) }
                    val spinnerAdapter: CustomAdapter<String>? = context?.let { context ->
                        CustomAdapter(
                            context,
                            R.layout.spinner_item,
                            groupList
                        )
                    }

                    spinnerAdapter?.setDropDownViewResource(R.layout.spinner_dropdown_item)
                    spBuildingGroup.apply {
                        adapter = spinnerAdapter
                        spBuildingGroup.onItemSelectedListener = this@ChooseBuildingFragment
                        setSelection(groupPosition)
                    }
                }
            }

            btnFindZone.clicks().subscribe {
                sharedViewModel.setSelectedBuildingsList(groupSelected, typeSelected)
                findNavController().navigate(R.id.action_chooseBuildingFragment_to_zonesMapsFragment)
            }

            return root
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            typePosition = savedInstanceState.getInt("type")
            groupPosition = savedInstanceState.getInt("group")
        }
        activity?.onBackPressedDispatcher?.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                findNavController().navigate(R.id.action_global_chooseActionFragment)
            }
        })
    }


    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("type", typePosition)
        outState.putInt("group", groupPosition)
        super.onSaveInstanceState(outState)
    }

    override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
        if (p1 != null) {
            val textView = p1 as TextView
            if (p2 == 0) {
                textView.setTextColor(R.color.button_text_color_disabled)
            }
            binding.apply {
                when (p0) {
                    spBuildingGroup -> {
                        groupPosition = p2
                        groupSelectionMade = p2 > 0
                        spBuildingType.isEnabled = groupSelectionMade
                        spBuildingType.setSelection(0)
                        if (groupSelectionMade) {
                            groupSelected = textView.text.toString()

                            val typeList = ArrayList<String>()
                            context?.resources?.getStringArray(R.array.building_types)?.toList()
                                ?.let { typeList.addAll(it) }

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
                            binding.apply {
                                spBuildingType.apply {
                                    adapter = spinnerAdapter
                                    onItemSelectedListener =
                                        this@ChooseBuildingFragment
                                    setSelection(typePosition)
                                }
                            }
                        }
                    }
                    spBuildingType -> {
                        typePosition = p2
                        typeSelectionMade = p2 > 0
                        if (typeSelectionMade)
                            typeSelected = textView.text.toString()
                    }
                }
                if (sharedViewModel.mLocationPerm.value == true) {
                    btnFindZone.isEnabled = typeSelectionMade && groupSelectionMade
                } else
                    Toast.makeText(context, R.string.give_location_permission, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onNothingSelected(p0: AdapterView<*>?) {
        typeSelectionMade = false
        groupSelectionMade = false
        binding.btnFindZone.isEnabled = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}