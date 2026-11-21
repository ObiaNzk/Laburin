package com.example.laburin.ui.screens.myoffers

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.laburin.R
import com.example.laburin.domain.model.PublishedOffer
import com.example.laburin.domain.model.PublishedOfferStatus
import com.example.laburin.ui.components.labelRes

@Composable
fun MyOffersRoute(
    onOfferClick: (offerId: String) -> Unit,
    onPublish: () -> Unit,
    viewModel: MyOffersViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // onStart: coming back from another app or screen. The ViewModel only asks the server if the data is stale.
    LifecycleEventEffect(Lifecycle.Event.ON_START) { viewModel.onScreenStarted() }
    MyOffersContent(state = state, onOfferClick = onOfferClick, onPublish = onPublish)
}

@Composable
fun MyOffersContent(state: MyOffersUiState, onOfferClick: (String) -> Unit, onPublish: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.my_offers_title), fontWeight = FontWeight.Bold, fontSize = 24.sp)
        when (state) {
            MyOffersUiState.Loading -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            MyOffersUiState.Empty -> Column(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(stringResource(R.string.my_offers_empty), textAlign = TextAlign.Center)
                Button(onClick = onPublish) { Text(stringResource(R.string.publish_submit)) }
            }
            is MyOffersUiState.Content -> {
                Button(onClick = onPublish, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.publish_submit)) }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.offers, key = { it.id }) { offer -> OfferRow(offer, onOfferClick) }
                }
            }
        }
    }
}

@Composable
private fun OfferRow(offer: PublishedOffer, onClick: (String) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .border(1.dp, Color.LightGray, RoundedCornerShape(12.dp))
            .clickable { onClick(offer.id) }
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(offer.title, fontWeight = FontWeight.Bold)
        Text(stringResource(offer.category.labelRes()), color = Color.Gray)
        Text(stringResource(offer.status.labelRes()), fontSize = 13.sp)
    }
}

private fun PublishedOfferStatus.labelRes() = when (this) {
    PublishedOfferStatus.PENDING_PUBLISH -> R.string.published_status_pending
    PublishedOfferStatus.EDIT_PENDING_SEND -> R.string.published_status_edit_pending
    PublishedOfferStatus.CLOSE_PENDING_SEND -> R.string.published_status_close_pending
    PublishedOfferStatus.ACTIVE -> R.string.published_status_active
    PublishedOfferStatus.CLOSED -> R.string.published_status_closed
}
