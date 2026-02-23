package com.example.ytshortless

import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        configureWebView(webView)

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                injectShortsHidingCss(view)
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
                if (webView.canGoBack()) {
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

    private fun configureWebView(view: WebView) {
        val settings = view.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        settings.loadsImagesAutomatically = true
        settings.mediaPlaybackRequiresUserGesture = false

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

                if (!window.__ytShortlessObserver) {
                    window.__ytShortlessObserver = new MutationObserver(function() {
                        if (!document.getElementById(styleId)) {
                            document.head.appendChild(style);
                        }
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
