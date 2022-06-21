package ua.od.acros.zoningapp.ui.main

import android.app.Dialog
import android.os.Bundle
import android.widget.CheckBox
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.preference.PreferenceManager
import ua.od.acros.zoningapp.R

class AskPermissionDialogFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            val inflater = requireActivity().layoutInflater
            val dialogView = inflater.inflate(R.layout.fragment_ask_permission_dialog, null)
            val cb = dialogView.findViewById<CheckBox>(R.id.cb_doNotAsk)
            val prefs = PreferenceManager.getDefaultSharedPreferences(it)
            val editor = prefs.edit()
            builder.apply {
                setView(dialogView)
                setPositiveButton(android.R.string.ok
                ) { _, _ ->
                    editor.putBoolean("show_dialog", cb.isChecked)
                    (it as MainActivity).apply {
                        askForLocationPermission()
                    }
                }
                setNegativeButton(android.R.string.cancel
                ) { _, _ ->
                    val check = cb.isChecked
                    editor.putBoolean("show_dialog", check)
                    if (check)
                        Toast.makeText(it, R.string.not_functional_check, Toast.LENGTH_LONG).show()
                    else
                        Toast.makeText(it, R.string.not_functional, Toast.LENGTH_LONG).show()
                }
            }
            editor.apply()
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}