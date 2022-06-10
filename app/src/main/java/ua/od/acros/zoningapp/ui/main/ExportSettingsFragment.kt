package ua.od.acros.zoningapp.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.jakewharton.rxbinding4.view.clicks
import ua.od.acros.zoningapp.R
import ua.od.acros.zoningapp.databinding.FragmentExportSettingsBinding
import ua.od.acros.zoningapp.misc.utils.CustomAdapter
import ua.od.acros.zoningapp.vm.MainViewModel

//Unused
class ExportSettingsFragment : Fragment(), AdapterView.OnItemSelectedListener {

    private var _binding: FragmentExportSettingsBinding? = null

    private val binding get() = _binding!!

    private var fileType = ""

    private var directoryPath = ""

    private var fileName = ""

    private val sharedViewModel: MainViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activity?.onBackPressedDispatcher?.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                findNavController().navigate(R.id.action_exportSettingsFragment_to_zoneExportFragment)
            }
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExportSettingsBinding.inflate(inflater, container, false)

        this.context?.let { MobileAds.initialize(it) }
        val adRequest = AdRequest.Builder().build()
        binding.avExportSettingsFragmentBanner.loadAd(adRequest)

        binding.btnExport.isEnabled = false

        val spTypeAdapter: CustomAdapter<String>? = context?.let {
            CustomAdapter(
                it,
                R.layout.spinner_item,
                it.resources.getStringArray(R.array.file_types).toList()
            )
        }

        binding.spFileType.adapter = spTypeAdapter
        binding.spFileType.onItemSelectedListener = this

        val spFileAdapter: CustomAdapter<String>? = context?.let {
            CustomAdapter(
                it,
                R.layout.spinner_item,
                it.resources.getStringArray(R.array.file_paths).toList()
            )
        }
        spFileAdapter?.setNotifyOnChange(true)
        binding.spFilePath.adapter = spFileAdapter
        binding.spFilePath.onItemSelectedListener = this
        sharedViewModel.directorySelected.observe(viewLifecycleOwner) {
            directoryPath = it
            spFileAdapter?.clear()
            val newList = listOf(directoryPath,getString(R.string.select_directory_path))
            spFileAdapter?.addAll(newList)
            binding.spFilePath.setSelection(0)
        }

        binding.etEnterFileName.setOnEditorActionListener(TextView.OnEditorActionListener { view, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                fileName = view.text.toString()
                if (fileType == "")
                    binding.btnExport.isEnabled = false
                //return@OnEditorActionListener true
            }
            return@OnEditorActionListener false
        })

        binding.btnExport.clicks().subscribe {
            findNavController().navigate(R.id.action_exportSettingsFragment_to_HTMLPrintFragment)
        }

        return binding.root
    }

    override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
        if (p0 != null && p1 != null) {
            val textView = p1 as TextView
            if (p2 != 0) {
                when (p0) {
                    binding.spFileType -> {
                        fileType = textView.text.toString()
                        if (directoryPath != "" && fileName != "")
                            binding.btnExport.isEnabled = true
                    }
                    binding.spFilePath -> {
                        (activity as MainActivity).dirRequest.launch(null)
                        if (fileType != "" && fileName != "")
                            if (sharedViewModel.storagePerm.value == true) {
                                binding.btnExport.isEnabled = true
                            } else
                                Toast.makeText(context, R.string.give_storage_permission, Toast.LENGTH_LONG).show()
                    }
                }
            } else
                textView.setTextColor(R.color.button_text_color_disabled)
        }
    }

    override fun onNothingSelected(p0: AdapterView<*>?) {
        binding.btnExport.isEnabled = false
    }
}