package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.CadRiskAssessment
import com.example.model.RwmaRegion
import com.example.model.VesselType
import com.example.ui.theme.CardioSurfaceDark

class ThreeJsBridge(private val onVesselSelected: (VesselType) -> Unit) {
    @JavascriptInterface
    fun onVesselSelected(vesselCode: String) {
        val vessel = when (vesselCode.uppercase()) {
            "LAD" -> VesselType.LAD
            "LCX" -> VesselType.LCX
            "RCA" -> VesselType.RCA
            else -> null
        }
        if (vessel != null) {
            onVesselSelected(vessel)
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ThreeJsHeartView(
    assessment: CadRiskAssessment,
    pulseRate: Int,
    selectedVessel: VesselType?,
    onSelectVessel: (VesselType) -> Unit,
    showRwmaOverlay: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isLoaded by remember { mutableStateOf(false) }

    val ladPercent = assessment.vesselPredictions[VesselType.LAD]?.estimatedStenosisPercent ?: 15
    val lcxPercent = assessment.vesselPredictions[VesselType.LCX]?.estimatedStenosisPercent ?: 15
    val rcaPercent = assessment.vesselPredictions[VesselType.RCA]?.estimatedStenosisPercent ?: 15

    val ladHex = getStenosisHexColor(ladPercent)
    val lcxHex = getStenosisHexColor(lcxPercent)
    val rcaHex = getStenosisHexColor(rcaPercent)
    val vesselCode = selectedVessel?.code ?: ""

    // Update Three.js WebGL scene whenever state variables change
    LaunchedEffect(ladHex, lcxHex, rcaHex, pulseRate, vesselCode, showRwmaOverlay, isLoaded) {
        if (isLoaded && webViewRef != null) {
            val script = "if (window.updateHeartState) { window.updateHeartState('$ladHex', '$lcxHex', '$rcaHex', $pulseRate, '$vesselCode', $showRwmaOverlay); }"
            webViewRef?.evaluateJavascript(script, null)
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(CardioSurfaceDark)
            .testTag("threejs_webgl_viewport")
    ) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        allowFileAccess = true
                        allowContentAccess = true
                        cacheMode = WebSettings.LOAD_NO_CACHE
                        useWideViewPort = true
                        loadWithOverviewMode = true
                    }
                    setBackgroundColor(0xFF090D16.toInt())
                    webChromeClient = WebChromeClient()
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isLoaded = true
                            val script = "if (window.updateHeartState) { window.updateHeartState('$ladHex', '$lcxHex', '$rcaHex', $pulseRate, '$vesselCode', $showRwmaOverlay); }"
                            view?.evaluateJavascript(script, null)
                        }
                    }
                    addJavascriptInterface(ThreeJsBridge(onSelectVessel), "AndroidBridge")
                    loadUrl("file:///android_asset/three_heart.html")
                    webViewRef = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

private fun getStenosisHexColor(stenosisPercent: Int): String {
    return when {
        stenosisPercent >= 70 -> "#FF1744" // Severe Critical Ruby Red
        stenosisPercent >= 50 -> "#FF5252" // Significant Stenosis Red
        stenosisPercent >= 30 -> "#FFB300" // Moderate Warning Amber
        else -> "#00E676"                  // Healthy Emerald
    }
}
