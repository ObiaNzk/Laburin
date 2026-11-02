package com.example.laburin.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PublishSearchScreen(
    onBack: () -> Unit = {}
) {
    var title by rememberSaveable() { mutableStateOf("") }
    var rubro by rememberSaveable() { mutableStateOf("") }
    var schedule by rememberSaveable() { mutableStateOf("") }
    var description by rememberSaveable() { mutableStateOf("") }
    var businessName by rememberSaveable() { mutableStateOf("") }
    var jobType by rememberSaveable() { mutableStateOf("") }
    var address by rememberSaveable() { mutableStateOf("") }
    var photoUri by rememberSaveable() { mutableStateOf<Uri?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> photoUri = uri }

    val isComplete = listOf(title, rubro, schedule, businessName, jobType, address)
        .all { it.isNotBlank() }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onBack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
            }
            Text("Nueva búsqueda", fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, Color.Gray, RoundedCornerShape(12.dp))
                .clickable {
                    photoPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text(
                    if (photoUri == null) "Agregar foto del comercio" else "Foto seleccionada",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(16.dp))
            }
        }

        FormField(
            label = "Nombre del comercio",
            value = businessName,
            onValueChange = { businessName = it },
            placeholder = "Ej: UADE"
        )

        FormField(
            label = "Puesto",
            value = title,
            onValueChange = { title = it },
            placeholder = "Ej: Ayudante de taller"
        )

        FormField(
            label = "Rubro",
            value = rubro,
            onValueChange = { rubro = it },
            placeholder = "Ej: Mecánica"
        )

        FormField(
            label = "Horario",
            value = schedule,
            onValueChange = { schedule = it },
            placeholder = "Ej: Lunes a Viernes"
        )

        FormField(
            label = "Tipo de trabajo",
            value = jobType,
            onValueChange = { jobType = it },
            placeholder = "Ej: Jornada Completa"
        )

        FormField(
            label = "Ubicación",
            value = address,
            onValueChange = { address = it },
            placeholder = "Agregá la dirección del comercio",
        )

        FormField(
            label = "Qué buscas?",
            value = description,
            onValueChange = { description = it },
            placeholder = "Contá brevemente sobre la propuesta de trabajo",
            singleLine = false,
            modifier = Modifier.height(200.dp)
        )

        Button(
            onClick = { onBack() },
            enabled = isComplete,
            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
        ) {
            Text("Publicar la oferta", fontSize = 16.sp)
        }
    }
}

@Composable
fun FormField(
    label: String,
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true
) {
    Column(modifier.padding(bottom = 12.dp)) {
        Text(label, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        OutlinedTextField(
            value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder) },
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            singleLine = singleLine
        )
    }
}