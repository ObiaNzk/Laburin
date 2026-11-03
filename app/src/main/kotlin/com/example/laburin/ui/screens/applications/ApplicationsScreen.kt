package com.example.laburin.ui.screens.applications

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.laburin.R
import com.example.laburin.domain.model.JobApplication
import com.example.laburin.ui.components.ApplicationStatusChip
import com.example.laburin.ui.components.hintRes

@Composable
fun ApplicationsRoute(
    onLogin: () -> Unit,
    onOpenOffer: (offerId: String) -> Unit,
    viewModel: ApplicationsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ApplicationsContent(state = state, onLogin = onLogin, onApplicationClick = onOpenOffer)
}

@Composable
fun ApplicationsContent(state: ApplicationsUiState, onLogin: () -> Unit, onApplicationClick: (offerId: String) -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        Text(stringResource(R.string.applications_title), fontWeight = FontWeight.Bold, fontSize = 24.sp)
        Spacer(Modifier.height(12.dp))
        when (state) {
            ApplicationsUiState.Loading -> Centered { CircularProgressIndicator() }
            ApplicationsUiState.LoggedOut -> Centered {
                Text(stringResource(R.string.applications_logged_out), textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                Button(onClick = onLogin) { Text(stringResource(R.string.nav_login)) }
            }
            ApplicationsUiState.Empty -> Centered {
                Text(stringResource(R.string.applications_empty), textAlign = TextAlign.Center)
            }
            is ApplicationsUiState.Content -> {
                if (state.pendingToSend > 0) {
                    Text(
                        pluralStringResource(R.plurals.applications_pending, state.pendingToSend, state.pendingToSend),
                        Modifier
                            .fillMaxWidth()
                            .background(Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(16.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.applications, key = { it.id }) { ApplicationCard(it, onApplicationClick) }
                }
            }
        }
    }
}

@Composable
private fun ApplicationCard(application: JobApplication, onClick: (String) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .border(1.dp, Color.Gray, RoundedCornerShape(12.dp))
            .clickable { onClick(application.offerId) }
            .padding(16.dp)
    ) {
        Text(application.offerTitle, fontWeight = FontWeight.Bold)
        Text(application.businessName)
        Spacer(Modifier.height(8.dp))
        ApplicationStatusChip(application.status)
        application.status.hintRes()?.let {
            Spacer(Modifier.height(4.dp))
            Text(stringResource(it), fontSize = 12.sp, color = Color.Gray)
        }
    }
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) { content() }
    }
}
