package ua.od.acros.zoningapp.misc.utils

import android.app.Activity
import android.os.Build
import android.util.DisplayMetrics

fun screenValue(activity: Activity): Array<Int> {
    val height: Int
    val width: Int

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        width = activity.resources.displayMetrics.widthPixels
        height = activity.resources.displayMetrics.heightPixels
    } else {
        val displayMetrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        activity.windowManager.defaultDisplay.getMetrics(displayMetrics)
        height = displayMetrics.heightPixels
        width = displayMetrics.widthPixels
    }
    return arrayOf(height, width)
}

fun convertToPixels(activity: Activity, nDP: Int): Float {
    val conversionScale: Float = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        activity.resources.displayMetrics.density
    } else {
        val displayMetrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        activity.windowManager.defaultDisplay.getMetrics(displayMetrics)
        displayMetrics.density
    }
    return (nDP * conversionScale + 0.5f)
}


