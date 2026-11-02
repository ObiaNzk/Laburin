package com.example.laburin.ui.screens

import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LoginScreen(
    onBack: () -> Unit = {},
    onSignUpClick: () -> Unit = {},
    onLogin: (String) -> Unit
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    Column(Modifier
        .fillMaxSize()
        .statusBarsPadding()
        .padding(16.dp)) {
        IconButton(onClick = { onBack() }) {
            Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar")
        }
        Spacer(Modifier.height(16.dp))
        Text("Ingresá a tu cuenta", fontWeight = FontWeight.Bold, fontSize = 28.sp)
        Spacer(Modifier.height(24.dp))

        Text("Email", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
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
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { onLogin(email) },
            modifier = Modifier
                .fillMaxWidth()
                .height(55.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Ingresar")
        }

        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = {},
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Continuar con Google")
        }

        Spacer(Modifier.height(8.dp))
        TextButton(
            onClick = { onSignUpClick() },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("No tengo cuenta")
        }
    }
}
