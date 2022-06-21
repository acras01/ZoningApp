package ua.od.acros.zoningapp.ui.main

import android.app.Dialog
import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import ua.od.acros.zoningapp.R
import ua.od.acros.zoningapp.vm.MainViewModel

class EnterAddressDialogFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val sharedViewModel: MainViewModel by activityViewModels()
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            val inflater = requireActivity().layoutInflater
            val dialogView = inflater.inflate(R.layout.fragment_enter_address_dialog, null)
            val et = dialogView.findViewById<EditText>(R.id.et_enterAddress)
            builder.apply {
                setView(dialogView)
                setPositiveButton(android.R.string.ok
                ) { _, _ ->
                    val address = "${sharedViewModel.mCity.value?.city} ${et.text}"
                    sharedViewModel.getLocationFromAddress(address)
                }
            }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}