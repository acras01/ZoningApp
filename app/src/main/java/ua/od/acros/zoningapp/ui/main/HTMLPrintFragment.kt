package ua.od.acros.zoningapp.ui.main

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.print.PrintAttributes
import android.print.PrintManager
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.jakewharton.rxbinding4.view.clicks
import ua.od.acros.zoningapp.R
import ua.od.acros.zoningapp.databinding.FragmentHtmlPrintBinding
import ua.od.acros.zoningapp.vm.MainViewModel
import java.io.ByteArrayOutputStream

class HTMLPrintFragment : Fragment() {

    private var fragmentId: Int = 0

    private var _binding: FragmentHtmlPrintBinding? = null

    private val binding get() = _binding!!

    private lateinit var sharedViewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedViewModel = (activity as MainActivity).getViewModel()

        fragmentId = arguments?.getInt("fragment_id")!!

        activity?.onBackPressedDispatcher?.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when (fragmentId) {
                    ZONE_FOR_PURPOSE -> findNavController().navigate(R.id.action_HTMLPrintFragment_to_zonesMapFragment)
                    ZONE_ON_MAP -> findNavController().navigate(R.id.action_HTMLPrintFragment_to_zoneExportFragment)
                }
            }
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHtmlPrintBinding.inflate(inflater, container, false)
        binding.fabSaveResults.isEnabled = false
        binding.fabSaveResults.clicks().subscribe {
            createWebPrintJob(binding.webview)
        }
        printWebView(fragmentId)
        return binding.root
    }

    private fun printWebView(fragmentId: Int) {
        binding.webview.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                return true
            }

            override fun onPageFinished(view: WebView, url: String) {
                binding.fabSaveResults.isEnabled = true
            }
        }

        var html = ""
        when (fragmentId) {
            ZONE_ON_MAP -> html = prepareHtmlForSelectedZone()
            ZONE_FOR_PURPOSE -> html = prepareHtmlForSelectedPurposes()
        }

        binding.webview.loadDataWithBaseURL(
            null, html,
            "text/HTML", "UTF-8", null
        )
    }

    private fun prepareHtmlForSelectedZone(): String {
        var html = "<h1>Зона {ZONE_PLACEHOLDER}</h1>\n" +
                "<p><img src=\"{IMAGE_PLACEHOLDER}\"  style=\"height:320px; width:240px\" /></p>\n" +
                "<h2><strong>Вид використання території (земельної ділянки)</strong></h2>\n" +
                FIRST_GROUP +
                "<h3>{ZONE1_PLACEHOLDER}</h3>\n" +
                SECOND_GROUP +
                "<h3>{ZONE2_PLACEHOLDER}</h3>\n" +
                THIRD_GROUP +
                "<h3>{ZONE3_PLACEHOLDER}</h3>\n"

        var zoneDesc = sharedViewModel.getSelectedZone().get()?.first
        html = html.replace("{ZONE_PLACEHOLDER}", zoneDesc!!)
        zoneDesc = sharedViewModel.getSelectedZone().get()?.second!![0]
        html = html.replace("{ZONE1_PLACEHOLDER}", zoneDesc)
        zoneDesc = sharedViewModel.getSelectedZone().get()?.second!![1]
        html = html.replace("{ZONE2_PLACEHOLDER}", zoneDesc)
        zoneDesc = sharedViewModel.getSelectedZone().get()?.second!![2]
        html = html.replace("{ZONE3_PLACEHOLDER}", zoneDesc)
        html = html.replace("\n", "<br>")

        val bitmap: Bitmap = sharedViewModel.mMapBitmap.value!!
        html = html.replace("{IMAGE_PLACEHOLDER}", prepareImage(bitmap))

        return html
    }

    private fun prepareHtmlForSelectedPurposes(): String {
        var html = "<h1>Зона {ZONE_PLACEHOLDER}</h1>\n" +
                "<p><img src=\"{IMAGE_PLACEHOLDER}\"  style=\"height:320px; width:240px\" /></p>\n" +
                "<h2><strong>Вибрана група будівель - {GROUP_PLACEHOLDER}</strong></h2>\n" +
                "<h2>&nbsp;</h2>\n" +
                "<h2><strong>Для означеної зони вибрана група будівель належить до наступного виду використання території (земельної ділянки) - {TYPE_PLACEHOLDER}</strong></h2>\n" +
                "<p>&nbsp;</p>\n" +
                "<h3>{LEGEND_PLACEHOLDER}</h3>"

        var zoneDesc = sharedViewModel.getSelectedZone().get()?.first
        var type = ""
        var legend = ""
        when (zoneDesc?.last()) {
            'П' -> {
                type = getString(R.string.preferred_one)
                legend = FIRST_GROUP
            }
            'С' -> {
                type = getString(R.string.accompanying_one)
                legend = SECOND_GROUP
            }
            'Д' -> {
                type = getString(R.string.acceptable_one)
                legend = THIRD_GROUP
            }
        }
        html = html.replace("{ZONE_PLACEHOLDER}", zoneDesc!!.dropLast(1))
        html = html.replace("{TYPE_PLACEHOLDER}", type)
        zoneDesc = sharedViewModel.getSelectedZone().get()?.second!![0]
        html = html.replace("{GROUP_PLACEHOLDER}", zoneDesc)
        html = html.replace("{LEGEND_PLACEHOLDER}", legend)
        html = html.replace("\n", "<br>")

        val bitmap: Bitmap = sharedViewModel.mMapBitmap.value!!
        html = html.replace("{IMAGE_PLACEHOLDER}", prepareImage(bitmap))

        return html
    }

    private fun createWebPrintJob(webView: WebView) {
        val printManager = activity
            ?.getSystemService(Context.PRINT_SERVICE) as PrintManager
        val printAdapter = webView.createPrintDocumentAdapter("MyDocument")
        val jobName = getString(R.string.app_name) + " Print Test"
        printManager.print(
            jobName, printAdapter,
            PrintAttributes.Builder().build()
        )
    }

    private fun prepareImage(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
        val byteArray: ByteArray = byteArrayOutputStream.toByteArray()
        val imageBase64: String = Base64.encodeToString(byteArray, Base64.DEFAULT)
        return "data:image/png;base64,$imageBase64"
    }

    companion object Constants {

        const val ZONE_ON_MAP = 1
        const val ZONE_FOR_PURPOSE = 2

        const val FIRST_GROUP =
                "<h3><span style=\"background-color:#99cc00\">Переважний</span></h3>\n" +
                "<p>Переважний вид використання території (земельної ділянки) &ndash; вид використання, який відповідає переліку дозволених видів для даної територіальної зони і не потребує спеціального погодження</p>\n"

        const val SECOND_GROUP =
                "<h3><span style=\"background-color:#ffbb33\">Супутній</span></h3>\n" +
                "<p>Супутній вид використання території (земельної ділянки) &ndash; вид використання, який є дозволенним та необхідним для забезпечення функціонування переважного виду використання земельної ділянки, (який не потребує спеціального погодження)</p>\n"

        const val THIRD_GROUP =
                "<h3><span style=\"background-color:#33b5e5\">Допустимий</span></h3>\n" +
                "<p>Допустимий вид використання території (земельної ділянки) &ndash; вид використання, який не відповідає переліку переважних та супутніх видів для даної територіальної зони, але може бути дозволеним за умови спеціального погодження</p>"

    }
}