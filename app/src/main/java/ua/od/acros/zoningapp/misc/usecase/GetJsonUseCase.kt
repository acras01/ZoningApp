package ua.od.acros.zoningapp.misc.usecase

import ua.od.acros.zoningapp.misc.repository.JsonRepository
import java.lang.reflect.Type
import javax.inject.Inject

class GetJsonUseCase @Inject constructor(private val repository: JsonRepository) {
    suspend fun <T> execute(filename: String, type: Type): List<T> = repository.getT(filename, type)
}