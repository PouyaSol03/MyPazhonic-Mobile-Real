package com.example.mypazhonictest

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.mypazhonictest.ui.theme.MyPazhonicTestTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyPazhonicTestTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    OfflineReactTestScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun OfflineReactTestScreen(modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = true
                settings.allowContentAccess = true

                // Important for local file access to local JS/CSS (sometimes needed on some devices)
                settings.allowFileAccessFromFileURLs = true
                settings.allowUniversalAccessFromFileURLs = true

                settings.cacheMode = WebSettings.LOAD_DEFAULT

                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        Log.d("WebViewDebug", "Page started: $url")
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        Log.d("WebViewDebug", "Page finished: $url")
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?
                    ) {
                        Log.e("WebViewDebug", "Error: ${error?.description} url=${request?.url}")
                    }
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                        Log.d(
                            "WebViewConsole",
                            "${consoleMessage.message()} -- line ${consoleMessage.lineNumber()} of ${consoleMessage.sourceId()}"
                        )
                        return true
                    }
                }

                loadUrl("file:///android_asset/reactapp/index.html")
            }
        }
    )
}