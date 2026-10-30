package com.example.laburin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.laburin.model.Offer

@Composable
fun OfferDetailScreen(offer: Offer){
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(16.dp)) {
            TextButton(onClick = {}) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                Text("Volver")
            }
            Box(
                Modifier.fillMaxWidth().height(160.dp)
                    .background(Color.LightGray, RoundedCornerShape(12.dp))
            )
            Spacer(Modifier.height(16.dp))
            Text(offer.title, fontSize = 22.sp)
            Text(offer.category + " · a " + offer.distanceText + " m")
            Text(offer.description)
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp).fillMaxWidth().background(
            Color.White, RoundedCornerShape(24.dp)).border(1.dp, Color.Gray, RoundedCornerShape(24.dp)).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Creá tu cuenta para ver la búsqueda y postularte", fontWeight = FontWeight.Bold, fontSize = 18.sp, textAlign = TextAlign.Center)
                Text("Horario, requisitos y comercio. Te lleva menos de un minuto.", textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Button(onClick = {},
                Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black)) {
                Text("Crear cuenta")
            }
            OutlinedButton(onClick = {}, Modifier.fillMaxWidth()) {
                Text("Ya tengo cuenta")
            }
        }
    }
}