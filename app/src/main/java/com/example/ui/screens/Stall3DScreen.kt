package com.example.ui.screens

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.viewmodel.StallViewModel

/**
 * 3D-Stall-Rundgang (Seite /stall3d der Stallblick-Webapp).
 *
 * Die three.js-Szene der Webapp laeuft unveraendert im WebView (WebGL) –
 * derselbe Rundgang wie im Browser, inklusive Orbit-/Walk-Modus und
 * Kuh-Steckbriefen. Die Webapp laeuft ohne Passwortschutz, daher ist kein
 * Login noetig.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun Stall3DScreen(
    viewModel: StallViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val einstellungen by viewModel.streamSettings.collectAsState()
    var webView by remember { mutableStateOf<WebView?>(null) }

    BackHandler {
        val wv = webView
        if (wv != null && wv.canGoBack()) wv.goBack() else onBack()
    }

    Column(modifier = modifier.fillMaxSize().background(Color(0xFF0F172A))) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("stall3d_back_btn")) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Zurück",
                    tint = Color.White,
                )
            }
            Column {
                Text(
                    text = "Stall in 3D",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                )
                Text(
                    text = "Rundgang · Herde des Oberen Stollenhofs",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 10.sp,
                )
            }
        }

        if (!einstellungen.isWebappConfigured) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Keine Webapp-URL konfiguriert – unter Konfig. → Stallblick Cloud eintragen.",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(24.dp),
                )
            }
        } else {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        webViewClient = WebViewClient()
                        loadUrl("${einstellungen.webappUrlClean}/stall3d")
                        webView = this
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webView?.destroy()
            webView = null
        }
    }
}
