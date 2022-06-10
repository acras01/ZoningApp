package ua.od.acros.zoningapp.misc.data

import android.app.Application
import com.google.gson.Gson
import com.google.gson.stream.JsonReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ua.od.acros.zoningapp.misc.repository.JsonRepository
import java.lang.reflect.Type
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JsonRepositoryImpl @Inject constructor(
    private val application: Application
): JsonRepository {

    override suspend fun <T> getT(filename: String, type: Type): List<T> {
        var list = listOf<T>()
        withContext(Dispatchers.IO) {
            try {
                val stream = application.assets.open(filename)
                val reader = JsonReader(stream.reader())
                list = Gson().fromJson(reader, type)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return list
    }
}