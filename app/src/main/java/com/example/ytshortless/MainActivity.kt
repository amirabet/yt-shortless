package com.example.ytshortless

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.view.View
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var fullscreenContainer: FrameLayout
    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null
    private var previousRequestedOrientation: Int = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        fullscreenContainer = findViewById(R.id.fullscreenContainer)
        configureWebView(webView)

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                injectShortsHidingCss(view)
            }

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val scheme = request.url.scheme ?: return false
                if (scheme == "http" || scheme == "https") return false
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, request.url))
                } catch (_: ActivityNotFoundException) {
                }
                return true
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                if (customView != null) {
                    callback.onCustomViewHidden()
                    return
                }

                customView = view
                customViewCallback = callback
                webView.visibility = View.GONE
                fullscreenContainer.visibility = View.VISIBLE
                fullscreenContainer.addView(
                    view,
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                )
                previousRequestedOrientation = requestedOrientation
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                enterFullscreenUi()
            }

            override fun onHideCustomView() {
                if (customView == null) return

                fullscreenContainer.removeView(customView)
                fullscreenContainer.visibility = View.GONE
                webView.visibility = View.VISIBLE
                customView = null
                customViewCallback?.onCustomViewHidden()
                customViewCallback = null
                requestedOrientation = previousRequestedOrientation
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                exitFullscreenUi()
            }
        }

        if (savedInstanceState == null) {
            webView.loadUrl("https://m.youtube.com")
        } else {
            webView.restoreState(savedInstanceState)
            webView.post { injectShortsHidingCss(webView) }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (customView != null) {
                    webView.webChromeClient?.onHideCustomView()
                } else if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    override fun onDestroy() {
        webView.webChromeClient?.onHideCustomView()
        super.onDestroy()
    }

    private fun enterFullscreenUi() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    private fun exitFullscreenUi() {
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.show(WindowInsetsCompat.Type.systemBars())
        WindowCompat.setDecorFitsSystemWindows(window, true)
    }

    private fun configureWebView(view: WebView) {
        val settings = view.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        settings.loadsImagesAutomatically = true
        settings.mediaPlaybackRequiresUserGesture = false
        settings.useWideViewPort = true
        settings.textZoom = 100
        settings.userAgentString = WebSettings.getDefaultUserAgent(this)
            .replace("; wv", "")

        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(view, true)
    }

    private fun injectShortsHidingCss(view: WebView) {
        val css = """
            ytm-pivot-bar-renderer > ytm-pivot-bar-item-renderer:nth-of-type(2),
            ytd-reel-shelf-renderer,
            ytd-reel-shelf-renderer[is-shorts],
            ytm-reel-shelf-renderer,
            ytd-shorts,
            ytm-shorts-shelf-renderer,
            ytd-rich-section-renderer[is-shorts],
            ytm-shorts-lockup-view-model,
            .big-shorts-singleton,
            a[aria-label*='shorts' i],
            a[title*='shorts' i],
            a[href*='/shorts' i],
            ytm-pivot-bar-item-renderer[aria-label*='shorts' i],
            ytd-mini-guide-entry-renderer[aria-label*='shorts' i] {
                display: none !important;
            }
        """.trimIndent()

        val script = """
            (function() {
                var styleId = 'yt-shortless-style';
                var existing = document.getElementById(styleId);
                if (existing) { existing.remove(); }
                var style = document.createElement('style');
                style.id = styleId;
                style.type = 'text/css';
                style.appendChild(document.createTextNode(${escapeForJs(css)}));
                document.head.appendChild(style);

                function hideShortsSectionHeaders() {
                    document.querySelectorAll('yt-section-header-view-model').forEach(function(header) {
                        if (header.textContent.toLowerCase().indexOf('shorts') !== -1) {
                            var shelf = header.closest('grid-shelf-view-model') || header.parentElement;
                            if (shelf) { shelf.style.setProperty('display', 'none', 'important'); }
                        }
                    });
                }
                hideShortsSectionHeaders();

                if (!window.__ytShortlessObserver) {
                    window.__ytShortlessObserver = new MutationObserver(function() {
                        if (!document.getElementById(styleId)) {
                            document.head.appendChild(style);
                        }
                        hideShortsSectionHeaders();
                    });
                    window.__ytShortlessObserver.observe(document.documentElement, { childList: true, subtree: true });
                }
            })();
        """.trimIndent()

        view.evaluateJavascript(script, null)
    }

    private fun escapeForJs(value: String): String {
        return "'" + value
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\n", "\\n") + "'"
    }
}
