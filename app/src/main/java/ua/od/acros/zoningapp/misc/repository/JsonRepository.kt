package ua.od.acros.zoningapp.misc.repository

import java.lang.reflect.Type

interface JsonRepository {
    suspend fun <T> getT(filename: String, type: Type): List<T>
}