package ua.od.acros.zoningapp.ui.main

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.preference.PreferenceManager
import dagger.hilt.android.AndroidEntryPoint
import ua.od.acros.zoningapp.R
import ua.od.acros.zoningapp.vm.MainViewModel

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val sharedViewModel: MainViewModel by viewModels()

    private val locationResultLauncher: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()) { result ->
            var allAreGranted = true
            for (b in result.values) {
                allAreGranted = allAreGranted && b
            }
            sharedViewModel.setLocationPermission(allAreGranted)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        checkForLocationPermission(prefs.getBoolean("show_dialog", false))
    }

    private fun checkForLocationPermission(check: Boolean) {
        if (ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            sharedViewModel.setLocationPermission(true)
        } else if (!check) {
            val dialog = AskPermissionDialogFragment()
            dialog.show(supportFragmentManager, "AskPermissionDialogFragment")
        }
    }

    fun askForLocationPermission() {
        val appPerms = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        locationResultLauncher.launch(appPerms)
    }
}