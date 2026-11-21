package com.example.laburin.ui.screens.cv

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.laburin.R
import java.io.File

@Composable
fun CvViewerRoute(onBack: () -> Unit, viewModel: CvViewerViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CvViewerContent(state = state, onBack = onBack, onRetry = viewModel::onRetry, onRenderFailed = viewModel::onRenderFailed)
}

@Composable
fun CvViewerContent(
    state: CvViewerUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onRenderFailed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextButton(onClick = onBack) {
            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            Text(stringResource(R.string.detail_back))
        }
        Text(stringResource(R.string.cv_viewer_title), fontWeight = FontWeight.Bold, fontSize = 24.sp)
        when (state) {
            CvViewerUiState.Loading -> Centered { CircularProgressIndicator() }
            is CvViewerUiState.Ready -> PdfPages(state.path, onRenderFailed)
            CvViewerUiState.NotUploaded -> Centered { Text(stringResource(R.string.cv_viewer_not_uploaded), textAlign = TextAlign.Center) }
            is CvViewerUiState.Error -> Centered {
                Text(stringResource(state.error.messageRes()), textAlign = TextAlign.Center)
                if (state.error != CvViewerError.Unreadable) {
                    Button(onClick = onRetry) { Text(stringResource(R.string.map_retry)) }
                }
            }
        }
    }
}

private fun CvViewerError.messageRes() = when (this) {
    CvViewerError.Network -> R.string.cv_viewer_error_network
    CvViewerError.Server -> R.string.cv_viewer_error_server
    CvViewerError.Unreadable -> R.string.cv_viewer_error_unreadable
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) { content() }
}

private sealed interface OpenedPdf {
    data object Opening : OpenedPdf
    data class Opened(val document: PdfDocument) : OpenedPdf
    data object Failed : OpenedPdf
}

/** Every page, rendered at the screen's width only while it is on screen (a list keeps memory low on cheap phones). */
@Composable
private fun PdfPages(path: String, onRenderFailed: () -> Unit) {
    val currentOnRenderFailed by rememberUpdatedState(onRenderFailed)
    val opened by produceState<OpenedPdf>(OpenedPdf.Opening, path) {
        val document = PdfDocument.open(File(path))
        value = document?.let(OpenedPdf::Opened) ?: OpenedPdf.Failed
        awaitDispose { document?.close() }
    }
    when (val pdf = opened) {
        OpenedPdf.Opening -> Centered { CircularProgressIndicator() }
        OpenedPdf.Failed -> LaunchedEffect(path) { currentOnRenderFailed() }
        is OpenedPdf.Opened -> BoxWithConstraints(Modifier.fillMaxSize()) {
            val widthPx = constraints.maxWidth.coerceAtMost(MAX_PAGE_WIDTH_PX)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(pdf.document.pageCount) { index ->
                    PdfPage(pdf.document, index, widthPx, onFailed = currentOnRenderFailed)
                }
            }
        }
    }
}

@Composable
private fun PdfPage(document: PdfDocument, index: Int, widthPx: Int, onFailed: () -> Unit) {
    val bitmap by produceState<ImageBitmap?>(null, document, index, widthPx) {
        value = document.render(index, widthPx)
        if (value == null) onFailed()
    }
    val description = stringResource(R.string.cv_viewer_page, index + 1, document.pageCount)
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(document.aspectRatio(index))
            .background(Color.White)
            .semantics { contentDescription = description },
    ) {
        bitmap?.let { Image(it, contentDescription = null, modifier = Modifier.fillMaxSize()) }
    }
}

/** Sharp enough to read a CV; a wider bitmap only costs memory on low-end phones. */
private const val MAX_PAGE_WIDTH_PX = 1440
