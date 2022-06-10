package ua.od.acros.zoningapp.misc.usecase

import android.graphics.Bitmap
import ua.od.acros.zoningapp.misc.repository.SavePNGRepository
import javax.inject.Inject

class GetSavePNGUseCase @Inject constructor(private val repository: SavePNGRepository) {
    suspend fun execute(bitmap: Bitmap,
                        folderName: String,
                        fileName: String): Int = repository.saveImage(bitmap, folderName, fileName)
}