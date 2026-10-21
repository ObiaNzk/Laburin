package com.example.laburin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.laburin.model.Offer
import com.example.laburin.ui.components.LaburinNavigationBar
import com.example.laburin.ui.components.OfferItem

@Composable
fun NoConnectionMapScreen(){
    val savedOffers = listOf(
        Offer("Ayudante de taller", "Mecánica", "400 m", 400, "Av. Corrientes 1234"),
        Offer("Mozo/a fin de semana", "Gastronomía", "1,2 km", 1200, "Calle Ficticia 456"),
        Offer("Vendedor/a", "Indumentaria", "1,8 km", 1800, "Av. Rivadavia 2000")
    )

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .background(Color(0xFF333333))
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ){
                Text(
                    text = "Sin conexión - Cargando datos en memoria",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 14.sp
                )
            }
        },
        bottomBar = {
            LaburinNavigationBar()
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Tarjeta con borde punteado (Dashed border) indicando que el mapa no está disponible
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(Color(0xFFF5F5F5), RoundedCornerShape(16.dp))
                    .dashedBorder(color = Color.Gray, strokeWidth = 2.dp, cornerRadius = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "El mapa no está disponible sin conexión",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF444444),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Mostrando búsquedas guardadas",
                        color = Color(0xFF777777),
                        fontSize = 12.sp
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                savedOffers.forEach { offer ->
                    OfferItem(offer = offer)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = {},
                modifier = Modifier
                    .width(200.dp)
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text(
                    text = "Reintentar",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333)
                )
            }
        }
    }
}

fun Modifier.dashedBorder(color: Color, strokeWidth: androidx.compose.ui.unit.Dp, cornerRadius: androidx.compose.ui.unit.Dp) = this.drawBehind {
    val stroke = Stroke(
        width = strokeWidth.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
    )
    drawRoundRect(
        color = color,
        style = stroke,
        cornerRadius = CornerRadius(cornerRadius.toPx()),
        size = size
    )
}
