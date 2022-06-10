package ua.od.acros.zoningapp.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dagger.hilt.android.AndroidEntryPoint
import ua.od.acros.zoningapp.R
import ua.od.acros.zoningapp.vm.MainViewModel

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val sharedViewModel: MainViewModel by viewModels()

    val dirRequest = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let {
            contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val docId = DocumentsContract.getTreeDocumentId(it)
            val split = docId.split(":")
            val type = split[0]
            if ("primary".equals(type, ignoreCase = true)) {
                sharedViewModel.directorySelected.postValue(
                    "${Environment.getExternalStorageDirectory()}/${split[1]}")
            }
        }
    }

    private val storageResultLauncher: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()) { result ->
            var allAreGranted = true
            for(b in result.values) {
                allAreGranted = allAreGranted && b
            }

            if(allAreGranted) {
                sharedViewModel.storagePerm.postValue(true)
            } else {
                sharedViewModel.storagePerm.postValue(false)
            }
        }

    private val locationResultLauncher: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()) { result ->
            var allAreGranted = true
            for(b in result.values) {
                allAreGranted = allAreGranted && b
            }

            if(allAreGranted) {
                sharedViewModel.locationPerm.postValue(true)
            } else {
                sharedViewModel.locationPerm.postValue(false)
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun askForStoragePermission() {
        if (ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            sharedViewModel.storagePerm.postValue(true)
        } else {
            val appPerms = arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
            storageResultLauncher.launch(appPerms)
        }
    }

    fun askForLocationPermission() {
        if (ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            sharedViewModel.locationPerm.postValue(true)
        } else {
            val appPerms = arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
            locationResultLauncher.launch(appPerms)
        }
    }
}