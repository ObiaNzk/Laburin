package com.example.laburin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.laburin.model.Offer
import com.example.laburin.model.User
import com.example.laburin.model.UserType
import com.example.laburin.ui.components.LaburinNavigationBar
import com.example.laburin.ui.components.OfferItem

val sampleMyOffers = listOf(
    Offer(
        title = "Mozo",
        category = "Gastronomía",
        distanceMeters = 700,
        address = "Calle Ficticia 456",
        businessName = "Bodegón La Esquina"
    )
)

@Composable
fun ProfileScreen(
    onSearchClick: () -> Unit,
    onApplicationClick: () -> Unit,
    onOfferClick: (Offer) -> Unit,
    onLogout: () -> Unit,
    user: User
) {
    Scaffold(bottomBar = {
        LaburinNavigationBar(
            isLoggedIn = true,
            onSearchClick = onSearchClick,
            onApplicationClick = onApplicationClick
        )
    }) { innerPadding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text("Mi perfil", fontWeight = FontWeight.Bold, fontSize = 24.sp)
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(64.dp)
                            .background(Color.LightGray, CircleShape)
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "${user.firstName} ${user.lastName}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(user.email, color = Color.Gray)
                    }
                    TextButton(onClick = {}) {
                        Text("Editar")
                    }
                }
                Spacer(Modifier.height(16.dp))
                if (user.type == UserType.CANDIDATE) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .background(
                                Color.LightGray.copy(alpha = 0.3f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(16.dp)
                    ) {
                        Text("Mi CV", fontWeight = FontWeight.Bold)
                        Text("Todavía no cargaste tu CV. Solo se aceptan archivos PDF.")
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = {}) {
                            Text("Cargar CV")
                        }
                    }
                }

                if (user.type == UserType.BUSINESS) {
                    Text(
                        "Mis publicaciones (${sampleMyOffers.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }

            }

            if (user.type == UserType.BUSINESS) {
                items(sampleMyOffers) { offer ->
                    OfferItem(onClick = { onOfferClick(offer) }, offer = offer)
                }
            }

            item {
                Spacer(Modifier.height(16.dp))
                OutlinedButton(
                    onClick = { onLogout() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Cerrar sesión")
                }
            }
        }
    }
}
