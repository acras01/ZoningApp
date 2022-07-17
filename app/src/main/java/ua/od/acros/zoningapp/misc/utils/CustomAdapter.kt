package ua.od.acros.zoningapp.misc.utils

import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

class CustomAdapter<String>(
    context: Context,
    layoutID: Int,
    dataSource: ArrayList<String>
): ArrayAdapter<String>(context, layoutID, dataSource) {

    override fun isEnabled(position: Int): Boolean {
        return position != 0
    }

    override fun getDropDownView(
        position: Int,
        convertView: View?,
        parent: ViewGroup
    ): View {
        val view: TextView = super.getDropDownView(position, convertView, parent) as TextView
        if (position == 0) {
            view.setTextColor(Color.GRAY)
        }
        return view
    }
}