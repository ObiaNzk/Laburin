package com.example.laburin.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SignUpScreen(
    onBack: () -> Unit = {}
) {
    var name by rememberSaveable() { mutableStateOf("") }
    var surname by rememberSaveable() { mutableStateOf("") }
    var password by rememberSaveable() { mutableStateOf("") }
    var email by rememberSaveable() { mutableStateOf("") }

    Column(Modifier
        .fillMaxSize()
        .statusBarsPadding()
        .padding(16.dp)) {
        IconButton(onClick = { onBack() }) {
            Icon(imageVector = Icons.Default.Close, contentDescription = null)
        }
        Spacer(Modifier.height(16.dp))
        Text("Creá tu cuenta", fontWeight = FontWeight.Bold, fontSize = 28.sp)
        Spacer(Modifier.height(8.dp))
        Text("Después volvés a la búsqueda que viste.")
        Spacer(Modifier.height(24.dp))

        Text("Nombre", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            placeholder = { Text("Escriba su nombre aquí") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(Modifier.height(12.dp))
        Text("Apellido", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        OutlinedTextField(
            value = surname,
            onValueChange = { surname = it },
            placeholder = { Text("Escriba su apellido aquí") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(Modifier.height(12.dp))
        Text("Email", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            placeholder = { Text("Para enviarle la aprobación") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )

        Spacer(Modifier.height(12.dp))
        Text("Contraseña", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            placeholder = { Text("Mínimo 8 carácteres") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { onBack() },
            Modifier
                .fillMaxWidth()
                .height(55.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Crear cuenta y seguir")
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f))
            Text("o", modifier = Modifier.padding(horizontal = 12.dp), color = Color.Gray)
            HorizontalDivider(modifier = Modifier.weight(1f))
        }

        OutlinedButton(
            onClick = {},
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Continuar con Google")
        }

        Text(
            "Podés cargar tu CV antes de postularte.",
            fontSize = 12.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}