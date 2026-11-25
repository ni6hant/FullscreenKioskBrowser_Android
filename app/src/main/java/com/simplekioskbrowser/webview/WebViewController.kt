package com.simplekioskbrowser.webview

import android.content.Context
import android.view.View
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.simplekioskbrowser.R

/**
 * Owns the WebView instance and the loading/error overlay UI.
 *
 * Responsibilities:
 *  - Safe WebView configuration (JavaScript enabled only for site compatibility; no file access,
 *    no JavaScript bridges, no universal file access).
 *  - Navigation: loadUrl / reload.
 *  - Overlay states: loading, offline, generic load error, SSL error.
 *  - Lifecycle hooks: onPause / onResume / onDestroy.
 */
class WebViewController(
    context: Context,
    private val shouldReloadOnWebViewError: () -> Boolean = { false }
) {

    val context: Context = context.applicationContext

    private var webView: WebView? = null
    private var overlay: LinearLayout? = null
    private var loadingIndicator: ProgressBar? = null
    private var errorMessage: TextView? = null
    private var retryButton: Button? = null

    var onReloadRequested: (() -> Unit)? = null
    var onUrlLoadFinished: ((url: String?) -> Unit)? = null

    var pinchZoomEnabled: Boolean = true
        set(value) {
            field = value
            webView?.settings?.setBuiltInZoomControls(value)
            webView?.settings?.setDisplayZoomControls(false)
        }

    var clearCacheOnRefresh: Boolean = false

    init {
        val activity = context as? android.app.Activity
            ?: error("WebViewController must be created from an Activity context")
        overlay = activity.findViewById(R.id.error_overlay)
        loadingIndicator = activity.findViewById(R.id.loading_indicator)
        errorMessage = activity.findViewById(R.id.error_message)
        retryButton = activity.findViewById(R.id.error_retry)
        retryButton?.setOnClickListener {
            hideOverlay()
            onReloadRequested?.invoke()
        }
    }

    fun bind(webView: WebView) {
        this.webView = webView
        configureWebView(webView)
    }

    private fun configureWebView(webView: WebView) {
        val settings = webView.settings

        // --- Security-hardened WebView settings (NFR-04) ---
        // JavaScript is enabled because most modern sites are unusable without it. This is a
        // documented trade-off: no JavaScript interfaces are exposed and no file access is
        // allowed, so the attack surface is limited to the renderer's same-origin sandbox.
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.loadsImagesAutomatically = true
        settings.cacheMode = WebSettings.LOAD_DEFAULT

        settings.allowFileAccess = false
        settings.allowFileAccessFromFileURLs = false
        settings.allowUniversalAccessFromFileURLs = false
        settings.blockNetworkLoads = false

        settings.builtInZoomControls = pinchZoomEnabled
        settings.displayZoomControls = false

        // A UA that identifies this as a kiosk browser helps sites render appropriately.
        settings.userAgentString =
            "SimpleKioskBrowser/1.0 (Android Kiosk; +https://example.local)"

        // Safe browsing where supported (no-op on older WebView providers).
        try {
            settings.setSafeBrowsingEnabled(true)
        } catch (_: Exception) {
            // Safe browsing API not available on this WebView provider; not fatal.
        }

        webView.webViewClient = KioskWebViewClient(this, { onReloadRequested?.invoke() }, shouldReloadOnWebViewError)
        webView.webChromeClient = KioskWebChromeClient()
    }

    fun loadUrl(url: String) {
        hideOverlay()
        showLoading(true)
        webView?.loadUrl(url)
    }

    fun reload() {
        hideOverlay()
        showLoading(true)
        if (clearCacheOnRefresh) {
            webView?.clearCache(true)
        }
        webView?.reload()
    }

    fun stopLoading() {
        webView?.stopLoading()
    }

    fun onPageStarted() {
        showLoading(true)
        hideError()
    }

    fun onPageFinished() {
        showLoading(false)
        hideError()
        onUrlLoadFinished?.invoke(webView?.url)
    }

    fun showOfflineState() {
        showLoading(false)
        showMessage(context.getString(R.string.status_offline), showRetry = true)
    }

    fun showLoadError(title: String, description: String) {
        showLoading(false)
        val msg = buildString {
            append(title)
            if (description.isNotEmpty()) append(" (").append(description).append(")")
        }
        showMessage(msg, showRetry = true)
    }

    fun showSslError() {
        showLoading(false)
        showMessage(context.getString(R.string.status_ssl_error), showRetry = false)
    }

    private fun showMessage(message: String, showRetry: Boolean) {
        errorMessage?.text = message
        errorMessage?.visibility = View.VISIBLE
        retryButton?.visibility = if (showRetry) View.VISIBLE else View.GONE
        overlay?.visibility = View.VISIBLE
        overlay?.bringToFront()
    }

    private fun showLoading(show: Boolean) {
        loadingIndicator?.visibility = if (show) View.VISIBLE else View.GONE
        errorMessage?.visibility = View.GONE
        retryButton?.visibility = View.GONE
        if (show) {
            overlay?.visibility = View.VISIBLE
            overlay?.bringToFront()
        } else if (errorMessage?.visibility != View.VISIBLE) {
            hideOverlay()
        }
    }

    private fun hideError() {
        errorMessage?.visibility = View.GONE
        retryButton?.visibility = View.GONE
        maybeHideOverlay()
    }

    private fun hideOverlay() {
        overlay?.visibility = View.GONE
    }

    private fun maybeHideOverlay() {
        if (errorMessage?.visibility != View.VISIBLE &&
            retryButton?.visibility != View.VISIBLE &&
            loadingIndicator?.visibility != View.VISIBLE) {
            overlay?.visibility = View.GONE
        }
    }

    fun onPause() {
        webView?.onPause()
    }

    fun onResume() {
        webView?.onResume()
    }

    fun onDestroy() {
        webView?.destroy()
        webView = null
    }
}
