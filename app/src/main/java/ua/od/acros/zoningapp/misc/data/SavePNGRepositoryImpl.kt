package ua.od.acros.zoningapp.misc.data

import android.app.Application
import android.content.ContentValues
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ua.od.acros.zoningapp.misc.repository.SavePNGRepository
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.OutputStream
import javax.inject.Inject

class SavePNGRepositoryImpl @Inject constructor(
    private val application: Application
): SavePNGRepository {

    companion object Result {
        val OK: Int = 0
        val ERR: Int = -1
    }

    @Throws(FileNotFoundException::class)
    override suspend fun saveImage(
        bitmap: Bitmap,
        folderName: String,
        fileName: String
    ): Int {
        var result = ERR
        if (Build.VERSION.SDK_INT >= 29) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/$folderName")
                put(MediaStore.Images.Media.IS_PENDING, true)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            }

            val uri: Uri? = application.contentResolver
                .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

            if (uri != null) {
                withContext(Dispatchers.IO) {
                    result = saveImageToStream(bitmap, application.contentResolver.openOutputStream(uri))
                    values.put(MediaStore.Images.Media.IS_PENDING, false)
                    application.contentResolver.update(uri, values, null, null)

                }
                return result
            }
        } else {
            val dir = File(
                application.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                ""
            )

            if (!dir.exists()) {
                dir.mkdirs()
            }

            val imageFile = File(
                dir.absolutePath
                    .toString() + File.separator
                        + fileName + ".png"
            )
            withContext(Dispatchers.IO) {
                result = saveImageToStream(bitmap, FileOutputStream(imageFile))
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DATA, imageFile.absolutePath)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                }
                application.contentResolver
                    .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            }
            return result
        }
        return result
    }

    private fun saveImageToStream(bitmap: Bitmap, outputStream: OutputStream?): Int {
        if (outputStream != null) {
            return try {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                outputStream.flush()
                outputStream.close()
                OK
            } catch (e: Exception) {
                e.printStackTrace()
                ERR
            }
        }
        return ERR
    }
}