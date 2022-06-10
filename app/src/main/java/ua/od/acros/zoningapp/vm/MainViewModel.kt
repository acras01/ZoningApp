package ua.od.acros.zoningapp.vm

import android.graphics.Bitmap
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import ua.od.acros.zoningapp.misc.utils.Building
import ua.od.acros.zoningapp.misc.utils.City
import ua.od.acros.zoningapp.misc.utils.Zone
import ua.od.acros.zoningapp.misc.usecase.*
import java.lang.reflect.Type
import java.util.regex.Pattern
import javax.inject.Inject


@HiltViewModel
class MainViewModel @Inject constructor(
    private val getZonesUseCase: GetZonesUseCase,
    private val getJsonUseCase: GetJsonUseCase,
    private val getCurrentLocationUseCase: GetCurrentLocationUseCase,
    private val getLocationFromAddressUseCase: GetLocationFromAddressUseCase,
    private val getSavePNGUseCase: GetSavePNGUseCase
) : ViewModel() {

    val directorySelected: MutableLiveData<String> = MutableLiveData()
    val selectedZone: MutableLiveData<Pair<String?, Array<String>?>> = MutableLiveData()
    val selectedBuildingsList: MutableLiveData<List<Building>?> = MutableLiveData()
    val cityList: MutableLiveData<List<City>?> = MutableLiveData()
    val buildingList: MutableLiveData<List<Building>?> = MutableLiveData()
    var city: MutableLiveData<City> = MutableLiveData()
    val cityZones: MutableLiveData<List<Zone>?> = MutableLiveData()
    val locationPerm: MutableLiveData<Boolean> = MutableLiveData(false)
    val storagePerm: MutableLiveData<Boolean> = MutableLiveData(false)
    val location: MutableLiveData<LatLng?> = MutableLiveData()
    val savePNG: MutableLiveData<Int> = MutableLiveData()
    val mapBitmap: MutableLiveData<Bitmap> = MutableLiveData()

    private var jobSVG: Job? = null
    private var jobCities: Job? = null
    private var jobBuildings: Job? = null
    private var jobLocation: Job? = null
    private var jobSavePNG: Job? = null

    init {
        parseCities()
    }

    fun prepareMapForCity(city: City) {
        jobSVG = viewModelScope.launch {
            val fileName = city.zones
            val zones = getZonesUseCase.execute(fileName)
            cityZones.postValue(zones)
        }
    }

    private fun parseCities() {
        jobCities = viewModelScope.launch {
            val cityListType: Type = object : TypeToken<MutableList<City>>() {}.type
            val list = getJsonUseCase.execute<City>("cities.json",cityListType)
            cityList.postValue(list)
        }
    }

    fun parseBuildings(city: City) {
        val name = city.city
        jobBuildings = viewModelScope.launch {
            val buildingListType: Type = object : TypeToken<MutableList<Building>>() {}.type
            val bList = getJsonUseCase.execute<Building>("buildings_$name.json",buildingListType)
            buildingList.postValue(bList)
        }
    }

    fun requestCurrentLocation() {
        jobLocation = viewModelScope.launch {
            val result = getCurrentLocationUseCase.execute()
            location.postValue(result)
        }
    }

    fun getLocationFromAddress(address: String) {
        jobLocation = viewModelScope.launch {
            val result = getLocationFromAddressUseCase.execute(address)
            location.postValue(result)
        }
    }

    fun savePNG(bitmap: Bitmap, folderName: String, fileName: String) {
        jobSavePNG = viewModelScope.launch {
            val result = getSavePNGUseCase.execute(bitmap, folderName, fileName)
            savePNG.postValue(result)
        }
    }

    override fun onCleared() {
        super.onCleared()
        jobSVG?.cancel()
        jobCities?.cancel()
        jobBuildings?.cancel()
        jobLocation?.cancel()
        jobSavePNG?.cancel()
    }

    fun getInfoForZone(title: String?): Array<String>? {
        if (title != null) {

            val list1 = mutableListOf<String>()
            val list2 = mutableListOf<String>()
            val list3 = mutableListOf<String>()
            buildingList.value?.forEach { building ->
                val code: StringBuilder = StringBuilder(building.toString())
                val regex = Regex("code_[0-9]*=")
                val codeStr = code.delete(0, 17)
                    .delete(code.indexOf("group"), code.length)
                    .replace(regex, "")
                    .split(", ")
                    .toSet()
                val pattern = Pattern.compile("$title[А-Я]")
                if (codeStr.any { e -> pattern.matcher(e).matches() }) {
                    val addition = if (building.misc != "") " (${building.misc})" else ""
                    if (codeStr.contains("${title}П"))
                        list1.add("  - ${building.group}$addition\n")
                    else if (codeStr.contains("${title}С"))
                        list2.add("  - ${building.group}$addition\n")
                    else if (codeStr.contains("${title}Д"))
                        list3.add("  - ${building.group}$addition\n")
                }
            }
            return arrayOf(list1.toSet().sorted().toContinuousString(),
                list2.toSet().sorted().toContinuousString(),
                list3.toSet().sorted().toContinuousString())
        }
        return null
    }

    fun getGroupList(): Collection<String>? {
        if (buildingList.value != null) {
            return buildingList.value!!
                .asSequence()
                .map { building -> building.group }
                .filterNot { it == "" }
                .toSet()
                .sorted()
                .toList()
        }
        return null
    }

    fun getTypeListForGroup(group: String): Collection<String>? {
        if (buildingList.value != null) {
            return buildingList.value!!
                .asSequence()
                .filter { building -> building.group == group }
                .map { building -> building.type }
                .filterNot { it == "" }
                .toSet()
                .sorted()
                .toList()
        }
        return null
    }

    fun getListForGroupAndType(group: String, type: String): List<Building>? {
        if (buildingList.value != null) {
            return buildingList.value!!
                .filter { building -> building.group == group }
                .filter { building -> building.type == type }
                .toList()
        }
        return null
    }
}

private fun <String> Collection<String>.toContinuousString(): kotlin.String {
    var result = ""
    this.forEach { element ->
        result += element
    }
    return result
}
