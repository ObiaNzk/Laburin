package com.example.laburin.ui.screens.scan

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.laburin.R
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

@Composable
fun ScanRoute(
    onOpenOffer: (offerId: String) -> Unit,
    onClose: () -> Unit,
    viewModel: ScanViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.onCameraPermissionResult(granted)
    }
    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (granted) viewModel.onCameraPermissionResult(true) else permission.launch(Manifest.permission.CAMERA)
    }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect -> if (effect is ScanEffect.OpenOffer) onOpenOffer(effect.offerId) }
    }
    ScanContent(
        state = state, onClose = onClose, onScanAgain = viewModel::onScanAgain, onSearchMap = onClose,
        camera = { QrCameraPreview(onCode = viewModel::onCodeScanned) },
    )
}

@Composable
fun ScanContent(
    state: ScanUiState,
    onClose: () -> Unit,
    onScanAgain: () -> Unit,
    onSearchMap: () -> Unit,
    camera: @Composable () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.DarkGray)
    ) {
        if (state == ScanUiState.Scanning) {
            camera()
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(260.dp).border(3.dp, Color.White, RoundedCornerShape(16.dp)))
                Spacer(Modifier.height(24.dp))
                Text(stringResource(R.string.scan_hint), fontWeight = FontWeight.Bold, color = Color.White)
                Text(stringResource(R.string.scan_hint_body), color = Color.White)
            }
        }
        when (state) {
            ScanUiState.PermissionDenied -> Message(
                title = stringResource(R.string.scan_no_permission),
                body = stringResource(R.string.scan_no_permission_body),
            ) { Button(onClick = onSearchMap) { Text(stringResource(R.string.scan_search_map)) } }
            ScanUiState.InvalidQr -> Message(
                title = stringResource(R.string.scan_invalid),
                body = stringResource(R.string.scan_invalid_body),
            ) {
                Button(onClick = onScanAgain) { Text(stringResource(R.string.scan_again)) }
                OutlinedButton(onClick = onSearchMap) { Text(stringResource(R.string.scan_search_map), color = Color.White) }
            }
            ScanUiState.AskingPermission, ScanUiState.Scanning -> Unit
        }
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(8.dp),
        ) {
            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.auth_close), tint = Color.White)
        }
    }
}

@Composable
private fun Message(title: String, body: String, actions: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
        Text(body, color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        actions()
    }
}

/** CameraX preview plus an ML Kit analyzer that reports every QR it reads. */
@OptIn(ExperimentalGetImage::class)
@Composable
private fun QrCameraPreview(onCode: (String) -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnCode by rememberUpdatedState(onCode)
    val scanner = androidx.compose.runtime.remember {
        BarcodeScanning.getClient(BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build())
    }
    DisposableEffect(Unit) { onDispose { scanner.close() } }
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val providerFuture = ProcessCameraProvider.getInstance(ctx)
            providerFuture.addListener({
                val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysis.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { proxy ->
                    val media = proxy.image
                    if (media == null) {
                        proxy.close()
                        return@setAnalyzer
                    }
                    scanner.process(InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees))
                        .addOnSuccessListener { codes -> codes.firstNotNullOfOrNull { it.rawValue }?.let(currentOnCode) }
                        .addOnCompleteListener { proxy.close() }
                }
                providerFuture.get().apply {
                    unbindAll()
                    bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
    )
}
