package com.example.laburin.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun ScanScreen(){
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED)
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission(), {permissionGranted -> hasCameraPermission = permissionGranted })
    LaunchedEffect(Unit) {
        if(!hasCameraPermission) launcher.launch(Manifest.permission.CAMERA)
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.DarkGray)){
        CameraPreview(Modifier.fillMaxSize())
        IconButton(onClick = {}, modifier = Modifier.align(Alignment.TopStart).statusBarsPadding().padding(8.dp)) {
            Icon(imageVector = Icons.Default.Close, "Cerrar", tint = Color.White)
        }
        Column(modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ){
            Box(modifier = Modifier.size(300.dp).border(3.dp, Color.White, RoundedCornerShape(16.dp)))
            Spacer(modifier = Modifier.height(24.dp))
            Text("Apunta al QR del cartel", fontWeight = FontWeight.Bold, color = Color.White)
            Text("La búsqueda se abre sola", color = Color.White)
        }
        if(!hasCameraPermission) {
            Column(
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding()
                    .padding(24.dp).fillMaxWidth().background(
                    Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp)
                ).border(2.dp, Color.Gray, RoundedCornerShape(16.dp)).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Sin permiso de cámara:", fontWeight = FontWeight.Bold, color = Color.White)
                Text("Activar permiso - Buscar en el mapa", color = Color.White)
            }
        }
    }
}

@Composable
fun CameraPreview(modifier: Modifier = Modifier){
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val previewView = PreviewView(ctx)

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build()
                preview.surfaceProvider = previewView.surfaceProvider
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview)
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
    )
}