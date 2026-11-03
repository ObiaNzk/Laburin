package com.example.laburin.ui.screens.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.laburin.R
import com.example.laburin.domain.model.Category
import com.example.laburin.domain.model.GeoLocation
import com.example.laburin.domain.model.Offer
import com.example.laburin.ui.components.DisplayMap
import com.example.laburin.ui.components.MapMarker
import com.example.laburin.ui.components.formatDistance
import com.example.laburin.ui.components.labelRes
import com.example.laburin.ui.theme.LaburinTheme
import java.time.Instant

@Composable
fun OfferDetailRoute(
    onNavigateBack: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    onNavigateToLogin: () -> Unit = {},
    onNavigateToApply: (String) -> Unit = {},
    viewModel: OfferDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                OfferDetailEffect.NavigateBack -> onNavigateBack()
                OfferDetailEffect.NavigateToSignUp -> onNavigateToSignUp()
                OfferDetailEffect.NavigateToLogin -> onNavigateToLogin()
                is OfferDetailEffect.NavigateToApply -> onNavigateToApply(effect.offerId)
            }
        }
    }

    OfferDetailContent(
        state = state,
        onBack = viewModel::onBack,
        onCreateAccount = viewModel::onCreateAccount,
        onLogin = viewModel::onLogin,
        onApply = viewModel::onApply,
        onRetry = viewModel::onRetry,
    )
}

@Composable
fun OfferDetailContent(
    state: OfferDetailUiState,
    onBack: () -> Unit,
    onCreateAccount: () -> Unit,
    onLogin: () -> Unit,
    onApply: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            TextButton(onClick = onBack) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                Text(stringResource(R.string.detail_back))
            }
            when (state) {
                OfferDetailUiState.Loading ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                is OfferDetailUiState.Guest -> GuestBody(state)
                is OfferDetailUiState.Full -> FullBody(state, Modifier.weight(1f))
                OfferDetailUiState.NoLongerAvailable -> Message(
                    title = stringResource(R.string.detail_no_longer_available),
                    body = stringResource(R.string.detail_no_longer_available_body),
                )
                is OfferDetailUiState.Unavailable -> Message(
                    title = stringResource(
                        if (state.isOffline) R.string.detail_unavailable_offline else R.string.detail_unavailable_server
                    ),
                    action = { OutlinedButton(onClick = onRetry) { Text(stringResource(R.string.map_retry)) } },
                )
            }
        }
        // Primary actions pinned to the bottom third for one-handed use (RNF-01).
        when (state) {
            is OfferDetailUiState.Guest -> SignUpPrompt(onCreateAccount, onLogin, Modifier.align(Alignment.BottomCenter))
            is OfferDetailUiState.Full -> ApplyButton(onApply, Modifier.align(Alignment.BottomCenter))
            else -> Unit
        }
    }
}

@Composable
private fun Header(title: String, subtitle: String, distanceMeters: Int?) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(160.dp)
            .background(Color.LightGray, RoundedCornerShape(12.dp))
    )
    Spacer(Modifier.height(16.dp))
    Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    val distance = distanceMeters?.let { stringResource(R.string.detail_distance, formatDistance(it)) }
    Text(listOfNotNull(subtitle, distance).joinToString(" · "))
}

@Composable
private fun GuestBody(state: OfferDetailUiState.Guest) {
    Header(state.title, stringResource(state.category.labelRes()), state.distanceMeters)
    Spacer(Modifier.height(16.dp))
    listOf(1f, 0.9f, 0.7f).forEach { width ->
        Box(
            Modifier
                .height(16.dp)
                .fillMaxWidth(width)
                .background(Color.LightGray, RoundedCornerShape(5.dp))
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun FullBody(state: OfferDetailUiState.Full, modifier: Modifier = Modifier) {
    val offer = state.offer
    Column(
        modifier
            .verticalScroll(rememberScrollState())
            .padding(bottom = 84.dp)
    ) {
        Header(offer.title, offer.businessName, state.distanceMeters)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(onClick = {}, label = { Text(stringResource(offer.category.labelRes())) })
            if (offer.jobType.isNotBlank()) AssistChip(onClick = {}, label = { Text(offer.jobType) })
        }
        Section(stringResource(R.string.detail_schedule)) { Text(offer.schedule) }
        Section(stringResource(R.string.detail_what_we_look_for)) { Text(offer.description) }
        Section(stringResource(R.string.detail_location)) {
            Text(offer.address)
            Spacer(Modifier.height(8.dp))
            DisplayMap(
                markers = listOf(MapMarker(offer.id, offer.location, offer.title, offer.address)),
                center = offer.location,
                zoomLevel = 15.0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(12.dp)),
            )
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Spacer(Modifier.height(16.dp))
    Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    content()
}

@Composable
private fun ApplyButton(onApply: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onApply,
        modifier = modifier
            .navigationBarsPadding()
            .padding(16.dp)
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(Color.DarkGray),
    ) {
        Text(stringResource(R.string.detail_apply), fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SignUpPrompt(onCreateAccount: () -> Unit, onLogin: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .navigationBarsPadding()
            .padding(16.dp)
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(24.dp))
            .border(1.dp, Color.Gray, RoundedCornerShape(24.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(R.string.detail_guest_title),
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
        )
        Text(stringResource(R.string.detail_guest_body), textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onCreateAccount,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
        ) {
            Text(stringResource(R.string.detail_create_account))
        }
        OutlinedButton(onClick = onLogin, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.detail_have_account))
        }
    }
}

@Composable
private fun Message(title: String, body: String? = null, action: @Composable () -> Unit = {}) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, textAlign = TextAlign.Center)
        if (body != null) Text(body, color = Color(0xFF777777), textAlign = TextAlign.Center)
        action()
    }
}

private val previewOffer = Offer(
    id = "p-1", title = "Ayudante de taller", category = Category.MECHANICS,
    description = "Buscamos una persona con ganas de aprender.", schedule = "Lunes a viernes, 8 a 17 h",
    jobType = "Jornada completa", businessName = "[COMERCIO]", address = "[DIRECCION]",
    location = GeoLocation(-34.6037, -58.3816), updatedAt = Instant.EPOCH,
)

@Composable
private fun PreviewDetail(state: OfferDetailUiState) = LaburinTheme {
    OfferDetailContent(state, {}, {}, {}, {}, {})
}

@Preview(showBackground = true)
@Composable
private fun GuestPreview() = PreviewDetail(OfferDetailUiState.Guest(previewOffer.title, previewOffer.category, 400))

@Preview(showBackground = true)
@Composable
private fun FullPreview() = PreviewDetail(OfferDetailUiState.Full(previewOffer, 400))

@Preview(showBackground = true)
@Composable
private fun NoLongerAvailablePreview() = PreviewDetail(OfferDetailUiState.NoLongerAvailable)
