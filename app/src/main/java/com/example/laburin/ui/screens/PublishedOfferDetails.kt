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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.laburin.model.Applicant
import com.example.laburin.model.Offer

val sampleApplicants = listOf(
    Applicant("Post 1", "112232198394"),
    Applicant("Post 2", "323291"),
    Applicant("Post 3", "238293")
)

@Composable
fun PublishedOfferDetails(offer: Offer) {
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = {}) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver"
                )
            }
            Text(offer.title, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(120.dp)
                    .background(Color.LightGray, RoundedCornerShape(8.dp))
            )
            Column(
                modifier = Modifier.padding(start = 16.dp)
            ) {
                Text("QR de la búsqueda", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                OutlinedButton(
                    onClick = {},
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Imprimir cartel")
                }
                OutlinedButton(
                    onClick = {},
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Compartir")
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Text(
            "Postulantes (${sampleApplicants.size})",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
        Spacer(Modifier.height(8.dp))

        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(sampleApplicants) { applicant ->
                var isApproved by rememberSaveable() { mutableStateOf(false) }
                Card(
                    Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.Gray, RoundedCornerShape(12.dp))
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(40.dp)
                                    .background(Color.LightGray, CircleShape)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column() {
                                Text(applicant.name)
                                Text("Ver CV", color = Color.Gray)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        if (isApproved) {
                            Text(applicant.contact)
                        } else {
                            Text(
                                "Contacto oculto hasta aprobar",
                                Modifier
                                    .fillMaxWidth()
                                    .background(Color.DarkGray, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                color = Color.White,
                                fontSize = 11.sp
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { isApproved = true },
                                Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                            ) {
                                Text("Aprobar")
                            }
                            OutlinedButton(onClick = {}, Modifier.weight(1f)) {
                                Text("Rechazar")
                            }
                        }
                    }
                }
            }
        }

    }
}