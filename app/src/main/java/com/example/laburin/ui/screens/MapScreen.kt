package com.example.laburin.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.laburin.ui.components.LaburinNavigationBar

@Composable
fun MapScreen() {
    // Scaffold con bottomBar para que la barra quede fija abajo de todo
    Scaffold(
        bottomBar = {
            LaburinNavigationBar()
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {

        }
    }
}