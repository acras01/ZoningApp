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
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import ua.od.acros.zoningapp.R
import ua.od.acros.zoningapp.databinding.FragmentHtmlPrintBinding
import ua.od.acros.zoningapp.vm.MainViewModel
import java.io.ByteArrayOutputStream


class HTMLPrintFragment : Fragment() {

    private var _binding: FragmentHtmlPrintBinding? = null

    private val binding get() = _binding!!

    private val sharedViewModel: MainViewModel by activityViewModels()

    private var myWebView: WebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val fragmentId = arguments?.getInt("id")!!

        activity?.onBackPressedDispatcher?.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when (fragmentId) {
                    ZoneExportFragment.FRAGMENT_ID -> findNavController().navigate(R.id.action_HTMLPrintFragment_to_zoneExportFragment)
                    ZonesMapFragment.FRAGMENT_ID -> findNavController().navigate(R.id.action_HTMLPrintFragment_to_zonesMapFragment)
                }
            }
        })
        printWebView(fragmentId)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHtmlPrintBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun printWebView(fragmentId: Int) {
        val webView = activity?.let { WebView(it) }
        if (webView != null) {
            webView.webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest
                ): Boolean {
                    return false
                }

                override fun onPageFinished(view: WebView, url: String) {
                    createWebPrintJob(view)
                    myWebView = null
                    activity?.onBackPressed()
                }
            }
        }

        var html = ""
        when (fragmentId) {
            ZoneExportFragment.FRAGMENT_ID -> html = prepareHtmlForSelectedZone()
            ZonesMapFragment.FRAGMENT_ID -> html = prepareHtmlForSelectedPurposes()
        }

        webView?.loadDataWithBaseURL(
            null, html,
            "text/HTML", "UTF-8", null
        )
        myWebView = webView
    }

    private fun prepareHtmlForSelectedZone(): String {
        var html = "<h1>Зона {ZONE_PLACEHOLDER}</h1>\n" +
                "<p><img src=\"{IMAGE_PLACEHOLDER}\"  style=\"height:320px; width:240px\" /></p>\n" +
                "<h2><strong>Вид використання території (земельної ділянки)</strong></h2>\n" +
                FIRST +
                "<h3>{ZONE1_PLACEHOLDER}</h3>\n" +
                SECOND +
                "<h3>{ZONE2_PLACEHOLDER}</h3>\n" +
                THIRD +
                "<h3>{ZONE3_PLACEHOLDER}</h3>\n"

        var zoneDesc = sharedViewModel.selectedZone.value!!.first
        html = html.replace("{ZONE_PLACEHOLDER}", zoneDesc!!)
        zoneDesc = sharedViewModel.selectedZone.value!!.second!![0]
        html = html.replace("{ZONE1_PLACEHOLDER}", zoneDesc)
        zoneDesc = sharedViewModel.selectedZone.value!!.second!![1]
        html = html.replace("{ZONE2_PLACEHOLDER}", zoneDesc)
        zoneDesc = sharedViewModel.selectedZone.value!!.second!![2]
        html = html.replace("{ZONE3_PLACEHOLDER}", zoneDesc)
        html = html.replace("\n", "<br>")

        // Convert bitmap to Base64 encoded image for web
        val bitmap: Bitmap = sharedViewModel.mapBitmap.value!!
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
        val byteArray: ByteArray = byteArrayOutputStream.toByteArray()
        val imageBase64: String = Base64.encodeToString(byteArray, Base64.DEFAULT)
        val image = "data:image/png;base64,$imageBase64"
        // Use image for the img src parameter in your html and load to webview
        html = html.replace("{IMAGE_PLACEHOLDER}", image)

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

        var zoneDesc = sharedViewModel.selectedZone.value!!.first
        var type = ""
        var legend = ""
        when (zoneDesc?.last()) {
            'П' -> {
                type = getString(R.string.preferred_one)
                legend = FIRST
            }
            'С' -> {
                type = getString(R.string.accompanying_one)
                legend = SECOND
            }
            'Д' -> {
                type = getString(R.string.acceptable_one)
                legend = THIRD
            }
        }
        html = html.replace("{ZONE_PLACEHOLDER}", zoneDesc!!.dropLast(1))
        html = html.replace("{TYPE_PLACEHOLDER}", type)
        zoneDesc = sharedViewModel.selectedZone.value!!.second!![0]
        html = html.replace("{GROUP_PLACEHOLDER}", zoneDesc)
        html = html.replace("{LEGEND_PLACEHOLDER}", legend)
        html = html.replace("\n", "<br>")

        // Convert bitmap to Base64 encoded image for web
        val bitmap: Bitmap = sharedViewModel.mapBitmap.value!!
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
        val byteArray: ByteArray = byteArrayOutputStream.toByteArray()
        val imageBase64: String = Base64.encodeToString(byteArray, Base64.DEFAULT)
        val image = "data:image/png;base64,$imageBase64"
        // Use image for the img src parameter in your html and load to webview
        html = html.replace("{IMAGE_PLACEHOLDER}", image)

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

    companion object Groups {
        const val FIRST =
                "<h3><span style=\"background-color:#99cc00\">Переважний</span></h3>\n" +
                "<p>Переважний вид використання території (земельної ділянки) &ndash; вид використання, який відповідає переліку дозволених видів для даної територіальної зони і не потребує спеціального погодження</p>\n"

        const val SECOND =
                "<h3><span style=\"background-color:#ffbb33\">Супутній</span></h3>\n" +
                "<p>Супутній вид використання території (земельної ділянки) &ndash; вид використання, який є дозволенним та необхідним для забезпечення функціонування переважного виду використання земельної ділянки, (який не потребує спеціального погодження)</p>\n"

        const val THIRD =
                "<h3><span style=\"background-color:#33b5e5\">Допустимий</span></h3>\n" +
                "<p>Допустимий вид використання території (земельної ділянки) &ndash; вид використання, який не відповідає переліку переважних та супутніх видів для даної територіальної зони, але може бути дозволеним за умови спеціального погодження</p>"

    }
}