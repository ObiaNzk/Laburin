package com.example.laburin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.laburin.ui.navigation.AppNavigation
import com.example.laburin.ui.theme.LaburinTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LaburinTheme {
                AppNavigation()
            }
        }
    }
}
