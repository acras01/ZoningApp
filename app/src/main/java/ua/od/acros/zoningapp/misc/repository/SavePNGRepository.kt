package ua.od.acros.zoningapp.misc.repository

import android.graphics.Bitmap

interface SavePNGRepository {
    suspend fun saveImage(bitmap: Bitmap, folderName: String, fileName: String): Int
}