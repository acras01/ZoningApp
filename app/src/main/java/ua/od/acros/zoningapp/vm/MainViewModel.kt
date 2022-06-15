package ua.od.acros.zoningapp.vm

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import androidx.lifecycle.LiveData
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

    private val directorySelected: MutableLiveData<String> = MutableLiveData()
    val mDirectorySelected: LiveData<String> get() = directorySelected
    fun setDirectory(uri: Uri) {
        val docId = DocumentsContract.getTreeDocumentId(uri)
        val split = docId.split(":")
        val type = split[0]
        if ("primary".equals(type, ignoreCase = true)) {
            directorySelected.value =
                "${Environment.getExternalStorageDirectory()}/${split[1]}"
        }
    }

    private val selectedZone: MutableLiveData<Pair<String?, Array<String>?>> = MutableLiveData()
    val mSelectedZone: LiveData<Pair<String?, Array<String>?>> get() = selectedZone
    fun setSelectedZone(pair: Pair<String?, Array<String>?>) {
        selectedZone.value = pair
    }

    private val selectedBuildingsList: MutableLiveData<List<Building>?> = MutableLiveData()
    val mSelectedBuildingsList: LiveData<List<Building>?> get() = selectedBuildingsList
    fun setSelectedBuildingsList(groupSelected: String, typeSelected: String) {
        selectedBuildingsList.value = getListForGroupAndType(groupSelected, typeSelected)
    }

    private val cityList: MutableLiveData<List<City>?> = MutableLiveData()
    val mCityList: LiveData<List<City>?> get() = cityList

    private val buildingList: MutableLiveData<List<Building>?> = MutableLiveData()
    val mBuildingList: LiveData<List<Building>?> get() = buildingList

    private var city: MutableLiveData<City> = MutableLiveData()
    val mCity: LiveData<City> get() = city
    fun setCity(city: City) {
        this.city.value = city
    }

    private val cityZones: MutableLiveData<List<Zone>?> = MutableLiveData()
    val mCityZones: LiveData<List<Zone>?> get() = cityZones

    private val locationPerm: MutableLiveData<Boolean> = MutableLiveData(false)
    val mLocationPerm: LiveData<Boolean> get() = locationPerm
    fun setLocationPermission(permission: Boolean) {
        locationPerm.value = permission
    }

    private val storagePerm: MutableLiveData<Boolean> = MutableLiveData(false)
    val mStoragePerm: LiveData<Boolean> get() = storagePerm
    fun setStoragePermission(permission: Boolean) {
        storagePerm.value = permission
    }

    private val location: MutableLiveData<LatLng?> = MutableLiveData()
    val mLocation: LiveData<LatLng?> get() = location

    private val savePNG: MutableLiveData<Int> = MutableLiveData()
    val mSavePNG: LiveData<Int> get() = savePNG

    private val mapBitmap: MutableLiveData<Bitmap> = MutableLiveData()
    val mMapBitmap: LiveData<Bitmap> get() = mapBitmap
    fun setBitmap(bitmap: Bitmap?) {
        mapBitmap.value = bitmap
    }

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
            val fileName = city.city
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

    private fun getListForGroupAndType(group: String, type: String): List<Building>? {
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
