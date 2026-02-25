package com.example.mypazhonictest

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.WebViewAssetLoader
import com.example.mypazhonictest.bridge.WebViewBridge
import com.example.mypazhonictest.data.local.prefs.BiometricCredentialStore
import com.example.mypazhonictest.data.local.prefs.BiometricPrefs
import com.example.mypazhonictest.data.repository.UserRepository
import dagger.hilt.android.AndroidEntryPoint
import java.io.ByteArrayInputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var biometricPrefs: BiometricPrefs

    @Inject
    lateinit var biometricCredentialStore: BiometricCredentialStore

    private lateinit var webView: WebView
    private var splashView: View? = null
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var webViewBackCallback: OnBackPressedCallback

    /** Hide splash if load never completes (e.g. error). */
    private val splashHideFallback = Runnable { hideSplash() }

    /** Used to hide splash after minimum duration when load is done. */
    private val splashMinDurationHideRunnable = Runnable { hideSplash() }

    private var splashShownAt = 0L

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WebView.setWebContentsDebuggingEnabled(true)

        setContentView(R.layout.activity_main)
        webView = findViewById(R.id.webView)
        splashView = findViewById(R.id.splashInclude)
        splashShownAt = SystemClock.elapsedRealtime()

        setupWebView()
        setupBackPressHandler()
        scheduleSplashFallback()

        Log.d("WebView", "Loading offline app from assets: $APP_START_URL")
        webView.loadUrl(APP_START_URL)
    }

    /** Fallback: hide splash if load never completes (e.g. error). */
    private fun scheduleSplashFallback() {
        handler.postDelayed(splashHideFallback, SPLASH_FALLBACK_DELAY_MS)
    }

    /** Hides splash only after at least SPLASH_MIN_DURATION_MS have passed. Call when page has loaded or React is ready. */
    private fun tryHideSplashAfterLoad() {
        handler.removeCallbacks(splashMinDurationHideRunnable)
        val elapsed = SystemClock.elapsedRealtime() - splashShownAt
        val delay = (SPLASH_MIN_DURATION_MS - elapsed).coerceAtLeast(0L)
        handler.postDelayed(splashMinDurationHideRunnable, delay)
    }

    private fun hideSplash() {
        handler.removeCallbacks(splashHideFallback)
        handler.removeCallbacks(splashMinDurationHideRunnable)
        val splash = splashView ?: return
        splash.animate()
            .alpha(0f)
            .setDuration(250)
            .withEndAction {
                splash.visibility = View.GONE
            }
            .start()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.setBackgroundColor(0xFFFFFFFF.toInt())
        webView.setLayerType(WebView.LAYER_TYPE_HARDWARE, null)

        val assetLoader = WebViewAssetLoader.Builder()
            .setDomain(APP_ASSET_DOMAIN)
            .addPathHandler("/", ReactAppPathHandler(this))
            .build()

        val bridge = WebViewBridge(
            activity = this,
            webView = webView,
            userRepository = userRepository,
            biometricPrefs = biometricPrefs,
            biometricCredentialStore = biometricCredentialStore,
            mainHandler = handler,
            onReactReady = { tryHideSplashAfterLoad() }
        )
        webView.addJavascriptInterface(bridge, "AndroidBridge")

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            userAgentString = WebSettings.getDefaultUserAgent(this@MainActivity)
            useWideViewPort = true
            loadWithOverviewMode = true
            layoutAlgorithm = WebSettings.LayoutAlgorithm.NORMAL
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            setSupportZoom(false)
            builtInZoomControls = false
            displayZoomControls = false
            cacheMode = WebSettings.LOAD_DEFAULT
            allowFileAccess = false
            allowContentAccess = true
            allowFileAccessFromFileURLs = false
            allowUniversalAccessFromFileURLs = false
            loadsImagesAutomatically = true
            blockNetworkImage = false
            blockNetworkLoads = false
        }

        webView.isHorizontalScrollBarEnabled = false
        webView.isVerticalScrollBarEnabled = false
        webView.overScrollMode = View.OVER_SCROLL_NEVER

        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(message: ConsoleMessage?): Boolean {
                message ?: return false
                Log.d("WebView", "JS: ${message.message()} (${message.sourceId()}:${message.lineNumber()})")
                return true
            }

            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                if (newProgress == 100) {
                    handler.postDelayed({ applyHeightFixScript(view) }, 300)
                }
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): android.webkit.WebResourceResponse? {
                return request?.url?.let { assetLoader.shouldInterceptRequest(it) }
                    ?: super.shouldInterceptRequest(view, request)
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                Log.d("WebView", "Page started: $url")
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                Log.d("WebView", "Page finished: $url")
                tryHideSplashAfterLoad()
                if (::webViewBackCallback.isInitialized) {
                    handler.postDelayed({
                        webViewBackCallback.isEnabled = webView.canGoBack()
                    }, 100)
                }
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                Log.e("WebView", "Error: ${error?.description} url=${request?.url}")
            }
        }
    }

    private fun setupBackPressHandler() {
        webViewBackCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, webViewBackCallback)
    }

    private fun applyHeightFixScript(view: WebView?) {
        view?.evaluateJavascript(getHeightFixScript()) { result ->
            Log.d("WebView", "Height fix applied: $result")
        }
    }

    private fun getHeightFixScript(): String = """
        (function() {
          try {
            var root = document.getElementById('root');
            if (!root) return JSON.stringify({status: "NO_ROOT"});
            var html = document.documentElement;
            var body = document.body;
            html.style.setProperty('height', '100%', 'important');
            html.style.setProperty('overflow-x', 'hidden', 'important');
            body.style.setProperty('height', '100%', 'important');
            body.style.setProperty('overflow-x', 'hidden', 'important');
            root.style.setProperty('height', '100%', 'important');
            root.style.setProperty('min-height', '100%', 'important');
            root.style.setProperty('overflow-x', 'hidden', 'important');
            return JSON.stringify({status: "OK"});
          } catch(e) {
            return JSON.stringify({status: "ERROR", error: String(e)});
          }
        })();
    """.trimIndent()

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        webView.destroy()
    }

    companion object {
        private const val APP_ASSET_DOMAIN = "appassets.androidplatform.net"
        private const val APP_START_URL = "https://appassets.androidplatform.net/"
        private const val SPLASH_MIN_DURATION_MS = 3_000L
        private const val SPLASH_FALLBACK_DELAY_MS = 10_000L
    }
}

private class ReactAppPathHandler(private val context: Context) : WebViewAssetLoader.PathHandler {

    override fun handle(path: String): android.webkit.WebResourceResponse? {
        val normalizedPath = path.trimStart('/').ifEmpty { "index.html" }
        val appRelativePath = normalizedPath
            .removePrefix("$REACT_ASSET_ROOT/")
            .ifEmpty { "index.html" }

        val assetPath = when {
            isSpaRoute(appRelativePath) -> "$REACT_ASSET_ROOT/index.html"
            else -> "$REACT_ASSET_ROOT/$appRelativePath"
        }

        return try {
            if (assetPath.endsWith("index.html")) {
                val html = injectReactReadyScript(injectBaseHref(readAssetText(context, assetPath)))
                htmlResponseWithBase(html)
            } else {
                binaryAssetResponse(context, assetPath)
            }
        } catch (_: IOException) {
            if (!appRelativePath.contains('.') && appRelativePath != "index.html") {
                try {
                    val html = injectReactReadyScript(injectBaseHref(readAssetText(context, "$REACT_ASSET_ROOT/index.html")))
                    htmlResponseWithBase(html)
                } catch (_: IOException) {
                    null
                }
            } else {
                null
            }
        }
    }

    private fun isSpaRoute(path: String): Boolean {
        if (path == "index.html") return true
        if (path.contains('.')) return false
        return true
    }
}

/** Injects a script so that when React dispatches the custom event "react-ready", the bridge's onReactReady() is called. */
private fun injectReactReadyScript(html: String): String {
    val script = """
        <script>
        (function() {
          window.addEventListener('react-ready', function() {
            if (window.AndroidBridge && typeof window.AndroidBridge.onReactReady === 'function') {
              window.AndroidBridge.onReactReady();
            }
          });
        })();
        </script>
    """.trimIndent()
    if (html.contains("</body>", ignoreCase = true)) {
        return html.replace(Regex("</body\\s*>", RegexOption.IGNORE_CASE), "$script\n</body>")
    }
    return html + script
}

private fun injectBaseHref(html: String): String {
    if (html.contains("<base ", ignoreCase = true)) return html
    val baseTag = """<base href="/$REACT_ASSET_ROOT/">"""
    val headOpen = Regex("<head\\b[^>]*>", RegexOption.IGNORE_CASE)
    return if (headOpen.containsMatchIn(html)) {
        headOpen.replace(html) { match -> "${match.value}\n    $baseTag" }
    } else {
        html
    }
}

private const val REACT_ASSET_ROOT = "reactapp"

private fun htmlResponseWithBase(html: String): android.webkit.WebResourceResponse {
    val bytes = injectBaseHref(html).toByteArray(StandardCharsets.UTF_8)
    return android.webkit.WebResourceResponse(
        "text/html",
        "UTF-8",
        ByteArrayInputStream(bytes)
    )
}

private fun binaryAssetResponse(
    context: Context,
    assetPath: String
): android.webkit.WebResourceResponse {
    val mimeType = when {
        assetPath.endsWith(".js") -> "application/javascript"
        assetPath.endsWith(".css") -> "text/css"
        assetPath.endsWith(".svg") -> "image/svg+xml"
        assetPath.endsWith(".png") -> "image/png"
        assetPath.endsWith(".jpg") || assetPath.endsWith(".jpeg") -> "image/jpeg"
        assetPath.endsWith(".webp") -> "image/webp"
        assetPath.endsWith(".ttf") -> "font/ttf"
        assetPath.endsWith(".woff") -> "font/woff"
        assetPath.endsWith(".woff2") -> "font/woff2"
        assetPath.endsWith(".json") -> "application/json"
        else -> null
    }
    return android.webkit.WebResourceResponse(
        mimeType,
        null,
        context.assets.open(assetPath)
    )
}

private fun readAssetText(context: Context, assetPath: String): String =
    context.assets.open(assetPath).bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
