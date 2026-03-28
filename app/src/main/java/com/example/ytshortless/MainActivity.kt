package com.example.ytshortless

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
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
    companion object {
        private const val TAG = "MainActivity"
        private const val MAX_FULLSCREEN_ZOOM = 3.0f
    }

    private lateinit var webView: WebView
    private lateinit var fullscreenContainer: FrameLayout
    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null

    @Volatile
    private var isVideoPlaying = false
    private var scaleGestureDetector: ScaleGestureDetector? = null
    private var currentScale = 1.0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        fullscreenContainer = findViewById(R.id.fullscreenContainer)
        configureWebView(webView)

        webView.addJavascriptInterface(VideoStateInterface(), "YTShortlessNative")

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                return handleUrlOverride(request.url.toString())
            }

            @Suppress("DEPRECATION")
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                return handleUrlOverride(url)
            }

            override fun onPageFinished(view: WebView, url: String) {
                injectShortsHidingCss(view)
                injectScrollFix(view)
                injectVideoPlaybackTracking(view)
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
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                enterFullscreenUi()
                setupFullscreenZoom()
            }

            override fun onHideCustomView() {
                if (customView == null) return

                teardownFullscreenZoom()
                fullscreenContainer.removeView(customView)
                fullscreenContainer.visibility = View.GONE
                webView.visibility = View.VISIBLE
                customView = null
                customViewCallback?.onCustomViewHidden()
                customViewCallback = null
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                exitFullscreenUi()
            }
        }

        if (savedInstanceState == null) {
            val urlToLoad = resolveIntentUrl(intent) ?: "https://m.youtube.com"
            webView.loadUrl(urlToLoad)
        } else {
            webView.restoreState(savedInstanceState)
            webView.post {
                injectShortsHidingCss(webView)
                injectVideoPlaybackTracking(webView)
            }
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

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Both this callback and WebChromeClient callbacks run on the main thread,
        // so customView is consistent. isVideoPlaying is @Volatile for cross-thread visibility.
        when (newConfig.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> {
                if (isVideoPlaying && customView == null) {
                    triggerFullscreenJs()
                }
            }
            Configuration.ORIENTATION_PORTRAIT -> {
                if (customView != null) {
                    webView.webChromeClient?.onHideCustomView()
                }
            }
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        // scaleGestureDetector is set/cleared on the main thread together with customView,
        // so the null-safe call is consistent with the customView guard.
        if (customView != null) {
            scaleGestureDetector?.onTouchEvent(ev)
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val url = resolveIntentUrl(intent)
        if (url != null) {
            webView.loadUrl(url)
        }
    }

    private fun resolveIntentUrl(intent: Intent): String? {
        if (intent.action != Intent.ACTION_VIEW) return null
        val uri = intent.data ?: return null
        val scheme = uri.scheme?.lowercase()
        if (scheme != "http" && scheme != "https") return null
        val host = uri.host?.lowercase() ?: return null
        val youtubeHosts = setOf("youtube.com", "www.youtube.com", "m.youtube.com", "youtu.be")
        if (host !in youtubeHosts) return null
        // Normalise desktop/short URLs to the mobile YouTube host the WebView uses
        return uri.buildUpon()
            .scheme("https")
            .authority("m.youtube.com")
            .build()
            .toString()
    }

    private fun handleUrlOverride(rawUrl: String): Boolean {
        val lowerUrl = rawUrl.lowercase()

        if (lowerUrl.startsWith("intent://")) {
            val isYouTubeOpenAppIntent = lowerUrl.contains("mweb_c3_open_app") ||
                lowerUrl.contains("package=com.google.android.youtube")

            if (isYouTubeOpenAppIntent) {
                Log.d(TAG, "Blocked YouTube open-app intent: $rawUrl")
                return true
            }

            try {
                val intent = Intent.parseUri(rawUrl, Intent.URI_INTENT_SCHEME).apply {
                    addCategory(Intent.CATEGORY_BROWSABLE)
                    component = null
                    selector = null
                }
                startActivity(intent)
            } catch (_: ActivityNotFoundException) {
                Log.d(TAG, "No app found for intent URL: $rawUrl")
            } catch (_: Exception) {
                Log.d(TAG, "Failed to parse intent URL: $rawUrl")
            }
            return true
        }

        val uri = Uri.parse(rawUrl)
        val scheme = uri.scheme?.lowercase()
        if (scheme == "http" || scheme == "https") {
            return false
        }

        try {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (_: ActivityNotFoundException) {
            Log.d(TAG, "No app found for external URL: $rawUrl")
        }
        return true
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
            ytd-mini-guide-entry-renderer[aria-label*='shorts' i],
            ytm-open-app-promo-renderer,
            .mweb_c3_open_app,
            a[href^='intent://' i][href*='mweb_c3_open_app' i],
            a[href^='intent://' i][href*='open_app' i] {
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

    private fun injectScrollFix(view: WebView) {
        val script = """
            (function() {
                if (window.__ytShortlessScrollFix) return;
                window.__ytShortlessScrollFix = true;

                var prevScrollHeight = document.documentElement.scrollHeight;
                var rafPending = false;

                window.__ytShortlessScrollFixObserver = new MutationObserver(function() {
                    if (rafPending) return;
                    var capturedPrevHeight = prevScrollHeight;
                    var scrollTop = window.scrollY || document.documentElement.scrollTop;
                    var atBottom = (scrollTop + window.innerHeight) >= (capturedPrevHeight - 50);
                    rafPending = true;
                    requestAnimationFrame(function() {
                        rafPending = false;
                        var newScrollHeight = document.documentElement.scrollHeight;
                        if (newScrollHeight > capturedPrevHeight && atBottom) {
                            window.scrollBy(0, -(newScrollHeight - capturedPrevHeight + 1));
                        }
                        prevScrollHeight = document.documentElement.scrollHeight;
                    });
                });
                window.__ytShortlessScrollFixObserver.observe(document.documentElement, { childList: true, subtree: true });
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

    private fun triggerFullscreenJs() {
        val script = """
            (function() {
                var btn = document.querySelector('button.fullscreen-icon') ||
                          document.querySelector('button[aria-label*="full screen" i]') ||
                          document.querySelector('button[aria-label*="fullscreen" i]') ||
                          document.querySelector('.ytp-fullscreen-button') ||
                          document.querySelector('ytm-custom-fullscreen-button-renderer button') ||
                          document.querySelector('button.player-fullscreen-icon');
                if (btn) { btn.click(); return; }
                var video = document.querySelector('video');
                if (video && video.requestFullscreen) { video.requestFullscreen(); }
            })();
        """.trimIndent()
        webView.evaluateJavascript(script, null)
    }

    private fun setupFullscreenZoom() {
        currentScale = 1.0f
        scaleGestureDetector = ScaleGestureDetector(
            this,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    currentScale = (currentScale * detector.scaleFactor).coerceIn(1.0f, MAX_FULLSCREEN_ZOOM)
                    customView?.scaleX = currentScale
                    customView?.scaleY = currentScale
                    return true
                }
            }
        )
    }

    private fun teardownFullscreenZoom() {
        scaleGestureDetector = null
        customView?.scaleX = 1.0f
        customView?.scaleY = 1.0f
        currentScale = 1.0f
    }

    inner class VideoStateInterface {
        @JavascriptInterface
        fun onVideoPlay() {
            isVideoPlaying = true
        }

        @JavascriptInterface
        fun onVideoPause() {
            isVideoPlaying = false
        }
    }

    private fun injectVideoPlaybackTracking(view: WebView) {
        val script = """
            (function() {
                if (window.__ytVideoTracker) return;
                window.__ytVideoTracker = true;

                function attachVideoListeners(video) {
                    if (video.__ytTracked) return;
                    video.__ytTracked = true;
                    video.addEventListener('play', function() {
                        if (window.YTShortlessNative) { window.YTShortlessNative.onVideoPlay(); }
                    });
                    video.addEventListener('pause', function() {
                        if (window.YTShortlessNative) { window.YTShortlessNative.onVideoPause(); }
                    });
                    video.addEventListener('ended', function() {
                        if (window.YTShortlessNative) { window.YTShortlessNative.onVideoPause(); }
                    });
                    if (!video.paused) {
                        if (window.YTShortlessNative) { window.YTShortlessNative.onVideoPlay(); }
                    }
                }

                document.querySelectorAll('video').forEach(attachVideoListeners);

                new MutationObserver(function(mutations) {
                    mutations.forEach(function(m) {
                        m.addedNodes.forEach(function(node) {
                            if (node.nodeName === 'VIDEO') { attachVideoListeners(node); }
                            if (node.querySelectorAll) {
                                node.querySelectorAll('video').forEach(attachVideoListeners);
                            }
                        });
                    });
                }).observe(document.documentElement, { childList: true, subtree: true });
            })();
        """.trimIndent()
        view.evaluateJavascript(script, null)
    }
}
