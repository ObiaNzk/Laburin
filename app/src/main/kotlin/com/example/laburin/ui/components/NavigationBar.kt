package com.example.laburin.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun LaburinNavigationBar() {
    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(
            thickness = 1.dp,
            color = Color.LightGray.copy(alpha = 0.6f)
        )
        NavigationBar {
            NavigationBarItem(
                selected = true,
                onClick = {},
                icon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Buscar"
                    )
                },
                label = { Text("Buscar") }
            )

            NavigationBarItem(
                selected = false,
                onClick = {},
                icon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Assignment,
                        contentDescription = "Postulaciones"
                    )
                },
                label = { Text("Postulaciones") }
            )

            NavigationBarItem(
                selected = false,
                onClick = {},
                icon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Login,
                        contentDescription = "Ingresar"
                    )
                },
                label = { Text("Ingresar") }
            )
        }
    }
}