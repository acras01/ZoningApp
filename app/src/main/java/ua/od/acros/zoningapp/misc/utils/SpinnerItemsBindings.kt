package ua.od.acros.zoningapp.misc.utils

import android.widget.Spinner
import androidx.databinding.BindingAdapter

@BindingAdapter("app:items")
fun setItems(spinner: Spinner, items: List<String>) {
    val adapter: CustomAdapter<String> = spinner.adapter as CustomAdapter<String>
    adapter.replaceData(items)
}