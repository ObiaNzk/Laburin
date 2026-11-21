package com.example.laburin

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.laburin.ui.navigation.LaburinNavHost
import com.example.laburin.ui.theme.LaburinTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The app is always light (LaburinTheme): dark status and navigation bar icons even when the phone is in dark mode.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        keepInitialFocusOffTheContent()
        setContent {
            LaburinTheme {
                LaburinNavHost()
            }
        }
    }

    /**
     * Up to Android 8.1 the system focuses the first focusable view when the window opens, even in touch mode: the
     * map's search field took focus and the keyboard covered the list. The content container keeps that initial
     * focus instead; text fields still take it when the user taps them. Android 9+ no longer does this.
     */
    private fun keepInitialFocusOffTheContent() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) return
        findViewById<ViewGroup>(android.R.id.content).apply {
            isFocusableInTouchMode = true
            descendantFocusability = ViewGroup.FOCUS_BEFORE_DESCENDANTS
        }
    }
}
