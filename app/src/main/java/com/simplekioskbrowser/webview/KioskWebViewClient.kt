package com.simplekioskbrowser.webview

import android.annotation.SuppressLint
import android.net.http.SslError
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.simplekioskbrowser.R

/**
 * WebView client for the kiosk browser.
 *
 * Security rules (NFR-04):
 *  - Only http:// and https:// URLs are allowed into the WebView; other schemes are blocked.
 *  - SSL errors are never ignored: [onReceivedSslError] calls [SslHttpClientHandler.cancel].
 *  - HTTP error responses (4xx/5xx) are not treated as network disconnects (FR-07).
 */
class KioskWebViewClient(
    private val controller: WebViewController,
    private val onRecoverableError: () -> Unit,
    private val shouldReloadOnWebViewError: () -> Boolean = { false }
) : WebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        return blockNonWebScheme(request.url.toString())
    }

    @SuppressLint("DeprecatedApi")
    override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
        return blockNonWebScheme(url)
    }

    private fun blockNonWebScheme(url: String): Boolean {
        val isWeb = url.startsWith("http://", ignoreCase = true) ||
            url.startsWith("https://", ignoreCase = true)
        if (!isWeb) {
            // Non-HTTP(S) scheme: do not load in the WebView and do not launch arbitrary intents.
            // The kiosk is single-purpose; opening external schemes is out of scope.
            return true
        }
        return false
    }

    override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
        if (request.isForMainFrame && isRecoverableNetworkError(error.errorCode)) {
            // Recoverable network error: show offline state, and schedule a debounced reload only
            // when the operator enabled retry-on-failure (FR-07).
            controller.showOfflineState()
            if (shouldReloadOnWebViewError()) {
                onRecoverableError()
            }
        } else if (request.isForMainFrame) {
            controller.showLoadError(
                controller.context.getString(R.string.status_error_unable),
                error.description?.toString() ?: ""
            )
        }
        super.onReceivedError(view, request, error)
    }

    override fun onReceivedHttpError(
        view: WebView,
        request: WebResourceRequest,
        errorResponse: android.webkit.WebResourceResponse
    ) {
        // HTTP error response from the server (e.g. 404/500). The site sent a response, so this
        // is not a connectivity loss; show the page as-is and do not trigger recovery.
        super.onReceivedHttpError(view, request, errorResponse)
    }

    override fun onReceivedSslError(
        view: WebView,
        handler: SslErrorHandler,
        error: SslError
    ) {
        // NEVER proceed on a certificate error (NFR-04). Cancel and show a clear error.
        handler.cancel()
        controller.showSslError()
    }

    override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
        controller.onPageStarted()
        super.onPageStarted(view, url, favicon)
    }

    override fun onPageFinished(view: WebView, url: String) {
        controller.onPageFinished()
        super.onPageFinished(view, url)
    }

    /**
     * WebView main-frame error codes that indicate a recoverable network problem rather than a
     * site-level HTTP response or a permanent failure.
     */
    private fun isRecoverableNetworkError(errorCode: Int): Boolean {
        return when (errorCode) {
            ERROR_HOST_LOOKUP,
            ERROR_CONNECT,
            ERROR_TIMEOUT,
            ERROR_IO,
            ERROR_UNKNOWN,
            ERROR_REDIRECT_LOOP -> true
            else -> false
        }
    }
}
