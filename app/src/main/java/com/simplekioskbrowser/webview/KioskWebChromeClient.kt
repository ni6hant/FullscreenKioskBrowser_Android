package com.simplekioskbrowser.webview

import android.webkit.WebChromeClient
import android.webkit.WebView

/**
 * Minimal WebChromeClient. No JavaScript interfaces are exposed, so there is nothing to
 * handle here beyond default behaviour. Kept as a separate component for separation of concerns.
 */
class KioskWebChromeClient : WebChromeClient() {
    override fun onProgressChanged(view: WebView, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
    }
}
