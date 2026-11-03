package com.example.laburin.ui.screens.profile

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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
    ProfileContent(state = state, onLogout = viewModel::onLogout, onLogin = onLogin)
}

@Composable
fun ProfileContent(
    state: ProfileUiState,
    onLogout: () -> Unit,
    onLogin: () -> Unit,
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
