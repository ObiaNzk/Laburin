package com.example.laburin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.laburin.model.Offer
import com.example.laburin.domain.model.GeoLocation
import com.example.laburin.ui.components.DisplayMap
import com.example.laburin.ui.components.MapMarker
import com.example.laburin.ui.screens.map.MapUiState
import java.util.Locale

@Composable
fun OfferDetailScreen(offer: Offer, isLoggedIn: Boolean) {
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            TextButton(onClick = {}) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                Text("Volver")
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(Color.LightGray, RoundedCornerShape(12.dp))
            )
            Spacer(Modifier.height(16.dp))
            Text(offer.title, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(
                if (isLoggedIn) {
                    offer.businessName
                } else {
                    offer.category
                } + " · a " + offer.distanceText
            )

            if (isLoggedIn) {
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(offer.category.replaceFirstChar {
                                if (it.isLowerCase()) it.titlecase(
                                    Locale.ROOT
                                ) else it.toString()
                            })
                        })
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(offer.jobType.replaceFirstChar {
                                if (it.isLowerCase()) it.titlecase(
                                    Locale.ROOT
                                ) else it.toString()
                            })
                        })
                }

                Spacer(Modifier.height(16.dp))
                Text("Horario", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(offer.schedule)

                Spacer(Modifier.height(16.dp))
                Text("Qué buscamos", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(offer.description)

                Spacer(Modifier.height(16.dp))
                Text("Ubicación", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                
                val position = offer.location?.let { GeoLocation(it.latitude, it.longitude) }
                    ?: MapUiState.DEFAULT_CENTER
                DisplayMap(
                    markers = listOf(MapMarker(offer.title, position, offer.title, offer.address)),
                    center = position,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    zoomLevel = 15.0,
                )

                Button(
                    onClick = {},
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp).height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(Color.DarkGray)
                ) {
                    Text("Postularme", fontWeight = FontWeight.Bold)
                }
            } else {
                Spacer(Modifier.height(16.dp))
                Box(Modifier.height(16.dp).background(Color.LightGray, RoundedCornerShape(5.dp)).fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Box(Modifier.height(16.dp).background(Color.LightGray, RoundedCornerShape(5.dp)).fillMaxWidth(0.9f))
                Spacer(Modifier.height(8.dp))
                Box(Modifier.height(16.dp).background(Color.LightGray, RoundedCornerShape(5.dp)).fillMaxWidth(0.7f))
            }
        }

        if (!isLoggedIn) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(16.dp)
                    .fillMaxWidth()
                    .background(
                        Color.White, RoundedCornerShape(24.dp)
                    )
                    .border(1.dp, Color.Gray, RoundedCornerShape(24.dp))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Creá tu cuenta para ver la búsqueda y postularte",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    "Horario, requisitos y comercio. Te lleva menos de un minuto.",
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {},
                    Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
                ) {
                    Text("Crear cuenta")
                }
                OutlinedButton(onClick = {}, Modifier.fillMaxWidth()) {
                    Text("Ya tengo cuenta")
                }
            }
        }
    }
}
