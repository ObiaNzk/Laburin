package com.example.laburin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.laburin.model.ApplicationStatus
import com.example.laburin.model.JobApplication
import com.example.laburin.model.Offer
import com.example.laburin.ui.components.LaburinNavigationBar
import org.osmdroid.util.GeoPoint

val sampleApplications = listOf(
    JobApplication(
        Offer(
            "repositor",
            "tareas",
            1000,
            "calle aveallenda",
            GeoPoint
                (-30.33948, 98.323),
            "este trabajo es para repositor",
            "Lunes a Sabado, de 8hs a 18hs",
            "Jornada Completa",
            "Local nombre"
        ),
        ApplicationStatus.SENT
    ),
    JobApplication(
        Offer(
            "Vendedor",
            "tareas",
            1000,
            "calle quilmes",
            GeoPoint
                (-34.33948, 10.323),
            "este trabajo es para vendedor",
            "Lunes a Sabado, de 8hs a 18hs",
            "Jornada Completa",
            "Local nombre"
        ),
        ApplicationStatus.APPROVED
    )
)

@Composable
fun ApplicationsScreen(
    onSearchClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    val pendingCount = sampleApplications.count({ it.status == ApplicationStatus.PENDING })
    Scaffold(bottomBar = {
        LaburinNavigationBar(
            isLoggedIn = true,
            onSearchClick = onSearchClick,
            onLoginClick = onProfileClick
        )
    }) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(8.dp)
        ) {
            Text("Mis postulaciones", fontWeight = FontWeight.Bold, fontSize = 24.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                "$pendingCount postulación se enviará al reconectar",
                Modifier
                    .fillMaxWidth()
                    .background(
                        Color.LightGray.copy(alpha = 0.3f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(16.dp)
            )
            Spacer(Modifier.height(16.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(sampleApplications) { application ->
                    val isApproved = application.status == ApplicationStatus.APPROVED
                    Card(
                        Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.Gray, RoundedCornerShape(12.dp))
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(application.offer.title, fontWeight = FontWeight.Bold)
                            Text(application.offer.businessName)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                applicationStatusToString(application.status),
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .background(
                                        if (isApproved) Color.DarkGray else Color.Transparent,
                                        RoundedCornerShape(50)
                                    )
                                    .border(1.dp, Color.Gray, RoundedCornerShape(50))
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                                color = if (isApproved) Color.White else Color.Black
                            )
                        }
                    }
                }
                item {
                    Text(
                        "Aprobada: el comercio recibió tu contacto y te va a escribir para la entrevista",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }
    }
}


fun applicationStatusToString(applicationStatus: ApplicationStatus): String {
    return when (applicationStatus) {
        ApplicationStatus.SENT -> "Postulación enviada"
        ApplicationStatus.PENDING -> "Postulación pendiente de envío"
        ApplicationStatus.APPROVED -> "Postulación aprobada"
        ApplicationStatus.CLOSED -> "Búsqueda cerrada"
    }
}