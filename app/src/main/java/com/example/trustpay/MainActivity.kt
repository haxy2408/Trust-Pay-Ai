package com.example.trustpay

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.trustpay.ui.ReceiveMoneyQrActivity
import com.example.trustpay.ui.ScanAndPayActivity

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var btnOpenScan: Button
    private lateinit var btnOpenReceive: Button

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnOpenScan = findViewById(R.id.btnOpenScan)
        btnOpenReceive = findViewById(R.id.btnOpenReceive)
        webView = findViewById(R.id.mainWebView)

        btnOpenScan.setOnClickListener {
            val intent = Intent(this, ScanAndPayActivity::class.java)
            startActivity(intent)
        }

        btnOpenReceive.setOnClickListener {
            val intent = Intent(this, ReceiveMoneyQrActivity::class.java)
            startActivity(intent)
        }

        // Configure hardware accelerated hybrid WebView
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            cacheMode = WebSettings.LOAD_DEFAULT
            mediaPlaybackRequiresUserGesture = false
        }

        webView.webViewClient = WebViewClient()
        webView.webChromeClient = WebChromeClient()

        // Load local bundled web assets or hosted instance
        webView.loadUrl("https://ais-dev-my4ku2d6bhwtfyjm3eta2z-724809916276.asia-southeast1.run.app")
    }

    override fun onBackPressed() {
        if (this::webView.isInitialized && webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
