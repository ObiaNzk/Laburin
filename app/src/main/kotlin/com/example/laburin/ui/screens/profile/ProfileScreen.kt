package com.example.laburin.ui.screens.profile

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.laburin.R

@Composable
fun ProfileRoute(
    onLoggedOut: () -> Unit,
    onLogin: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect -> if (effect == ProfileEffect.LoggedOut) onLoggedOut() }
    }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pickCv = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val file = withContext(Dispatchers.IO) { readDocument(context, uri) }
            if (file == null) viewModel.onCvUnreadable() else viewModel.onCvPicked(file.name, file.mimeType, file.bytes)
        }
    }
    ProfileContent(
        state = state, onLogout = viewModel::onLogout, onLogin = onLogin,
        onPickCv = { pickCv.launch(arrayOf(PDF_MIME)) },
    )
}

private const val PDF_MIME = "application/pdf"

private class PickedFile(val name: String, val mimeType: String?, val bytes: ByteArray)

private fun readDocument(context: Context, uri: Uri): PickedFile? = try {
    val resolver = context.contentResolver
    val name = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0) else null
    } ?: "cv.pdf"
    val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
    PickedFile(name, resolver.getType(uri), bytes)
} catch (e: java.io.IOException) {
    null
} catch (e: SecurityException) {
    null
}

@Composable
fun ProfileContent(
    state: ProfileUiState,
    onLogout: () -> Unit,
    onLogin: () -> Unit,
    onPickCv: () -> Unit,
    modifier: Modifier = Modifier,
    roleSection: @Composable () -> Unit = {},
) {
    Column(
        modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.profile_title), fontWeight = FontWeight.Bold, fontSize = 24.sp)
        when (state) {
            ProfileUiState.Loading -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            ProfileUiState.LoggedOut -> {
                Text(stringResource(R.string.profile_logged_out), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Button(onClick = onLogin, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.nav_login)) }
            }
            is ProfileUiState.LoggedIn -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(64.dp)
                            .background(Color.LightGray, CircleShape)
                    )
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(state.session.displayName, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(state.session.email, color = Color.Gray)
                    }
                }
                if (state.showsCv) CvSection(state, onPickCv)
                roleSection()
                OutlinedButton(
                    onClick = onLogout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(stringResource(R.string.profile_logout))
                }
            }
        }
    }
}

@Composable
private fun CvSection(state: ProfileUiState.LoggedIn, onPickCv: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stringResource(R.string.profile_cv_title), fontWeight = FontWeight.Bold)
        Text(state.cv?.fileName ?: stringResource(R.string.profile_cv_missing))
        state.cvError?.let { Text(stringResource(it.messageRes()), color = MaterialTheme.colorScheme.error) }
        OutlinedButton(onClick = onPickCv, enabled = !state.isUploadingCv) {
            if (state.isUploadingCv) {
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
            }
            Text(stringResource(if (state.cv == null) R.string.profile_cv_upload else R.string.profile_cv_replace))
        }
    }
}

private fun CvError.messageRes() = when (this) {
    CvError.NotPdf -> R.string.profile_cv_error_not_pdf
    CvError.TooLarge -> R.string.profile_cv_error_too_large
    CvError.Network -> R.string.profile_cv_error_network
    CvError.Server -> R.string.profile_cv_error_server
    CvError.Unreadable -> R.string.profile_cv_error_unreadable
}
