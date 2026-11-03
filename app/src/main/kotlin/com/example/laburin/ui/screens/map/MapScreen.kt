package com.example.laburin.ui.screens.map

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.laburin.R
import com.example.laburin.domain.model.Category
import com.example.laburin.domain.model.NearbyOffer
import com.example.laburin.domain.model.Offer
import com.example.laburin.domain.model.SearchFilters
import com.example.laburin.ui.components.DisplayMap
import com.example.laburin.ui.components.LaburinNavigationBar
import com.example.laburin.ui.components.MapCircle
import com.example.laburin.ui.components.MapMarker
import com.example.laburin.ui.components.OfferItem
import com.example.laburin.ui.components.dashedBorder
import com.example.laburin.ui.components.formatDistance
import com.example.laburin.ui.components.labelRes
import com.example.laburin.ui.theme.LaburinTheme
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant

private const val RESULTS_SHEET_HEIGHT_FRACTION = 0.42f

private val LOCATION_PERMISSIONS = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
)

@Composable
fun MapRoute(
    onNavigateToOffer: (String) -> Unit = {},
    onNavigateToScan: () -> Unit = {},
    onNavigateToPublish: () -> Unit = {},
    viewModel: MapViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val refreshFailedMessage = stringResource(R.string.map_refresh_failed)

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results -> viewModel.onLocationPermissionResult(granted = results.values.any { it }) }

    // Saveable so a rotation doesn't ask again after the user already answered.
    var permissionAsked by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val alreadyGranted = LOCATION_PERMISSIONS.any {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        when {
            alreadyGranted -> viewModel.onLocationPermissionResult(granted = true)
            !permissionAsked -> {
                permissionAsked = true
                permissionLauncher.launch(LOCATION_PERMISSIONS)
            }
            else -> viewModel.onLocationPermissionResult(granted = false)
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is MapEffect.NavigateToOffer -> onNavigateToOffer(effect.offerId)
                MapEffect.NavigateToScan -> onNavigateToScan()
                MapEffect.NavigateToPublish -> onNavigateToPublish()
                is MapEffect.ShowMessage -> when (effect.message) {
                    MapMessage.RefreshFailedShowingCache -> snackbarHostState.showSnackbar(refreshFailedMessage)
                }
            }
        }
    }

    val now by produceState(Instant.now()) {
        while (true) {
            delay(60_000)
            value = Instant.now()
        }
    }

    MapContent(
        state = state,
        now = now,
        snackbarHostState = snackbarHostState,
        onQueryChange = viewModel::onQueryChange,
        onClearQuery = viewModel::onClearQuery,
        onRadiusChange = viewModel::onRadiusChange,
        onCategoryChange = viewModel::onCategoryChange,
        onWidenRadius = viewModel::onWidenRadius,
        onShowAllCategories = viewModel::onShowAllCategories,
        onRetry = viewModel::onRetry,
        onOfferClick = viewModel::onOfferClick,
        onScanClick = viewModel::onScanClick,
        onPublishClick = viewModel::onPublishClick,
    )
}

@Composable
fun MapContent(
    state: MapUiState,
    now: Instant,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    onRadiusChange: (Int) -> Unit,
    onCategoryChange: (Category?) -> Unit,
    onWidenRadius: () -> Unit,
    onShowAllCategories: () -> Unit,
    onRetry: () -> Unit,
    onOfferClick: (String) -> Unit,
    onScanClick: () -> Unit,
    onPublishClick: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val cached = (state.offers as? OffersState.Content)?.offers
    val offers = state.offers
    // Without network the map tiles can't load either, so both offline cases share the offline layout.
    val offlineLayout = (!state.isOnline && cached != null) || (offers is OffersState.Unavailable && offers.isOffline)
    Scaffold(
        modifier = modifier,
        topBar = {
            if (offlineLayout) {
                OfflineBanner(lastSyncedAt = state.lastSyncedAt, now = now, hasData = cached != null)
            } else {
                SearchTopBar(state, onQueryChange, onRadiusChange, onCategoryChange, onPublishClick)
            }
        },
        bottomBar = { LaburinNavigationBar() },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        if (offlineLayout) {
            OfflineOffers(
                offers = cached.orEmpty(),
                onOfferClick = onOfferClick,
                onRetry = onRetry,
                modifier = Modifier.padding(innerPadding),
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                DisplayMap(
                    markers = (state.offers as? OffersState.Content)?.offers.orEmpty().map { it.offer.toMarker() },
                    center = state.center,
                    userLocation = state.userLocation,
                    searchArea = MapCircle(state.center, state.radiusKm * 1_000.0),
                    bottomObstructionFraction = RESULTS_SHEET_HEIGHT_FRACTION,
                    onMarkerClick = onOfferClick,
                    modifier = Modifier.fillMaxSize(),
                )
                FloatingActionButton(
                    onClick = onScanClick,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 260.dp),
                    shape = CircleShape,
                ) {
                    val description = stringResource(R.string.map_scan_qr_description)
                    Text(
                        stringResource(R.string.map_scan_qr),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.semantics { contentDescription = description },
                    )
                }
                ResultsSheet(
                    state = state,
                    onOfferClick = onOfferClick,
                    onWidenRadius = onWidenRadius,
                    onShowAllCategories = onShowAllCategories,
                    onClearQuery = onClearQuery,
                    onRetry = onRetry,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }
}

@Composable
private fun SearchTopBar(
    state: MapUiState,
    onQueryChange: (String) -> Unit,
    onRadiusChange: (Int) -> Unit,
    onCategoryChange: (Category?) -> Unit,
    onPublishClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 6.dp,
        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
    ) {
        Column(
            modifier = Modifier
                .statusBarsPadding()
                .padding(vertical = 8.dp)
        ) {
            SearchBar(state.query, onQueryChange)
            FilterButtons(state.radiusKm, onRadiusChange, state.category, onCategoryChange)
            PublishButton(onPublishClick)
            if (state.isRefreshing) {
                LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp))
            }
        }
    }
}

@Composable
private fun SearchBar(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(24.dp),
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        placeholder = { Text(stringResource(R.string.map_search_placeholder)) },
    )
}

@Composable
private fun FilterButtons(
    radiusKm: Int,
    onRadiusChange: (Int) -> Unit,
    category: Category?,
    onCategoryChange: (Category?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterDropdown(
            label = stringResource(R.string.map_radius_chip, radiusKm),
            options = SearchFilters.RADIUS_OPTIONS_KM,
            optionLabel = { stringResource(R.string.map_radius_option, it) },
            onSelect = onRadiusChange,
            modifier = Modifier.weight(1f),
        )
        FilterDropdown(
            label = stringResource(R.string.map_category_chip, stringResource(category.labelRes())),
            options = listOf<Category?>(null) + Category.entries,
            optionLabel = { stringResource(it.labelRes()) },
            onSelect = onCategoryChange,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun <T> FilterDropdown(
    label: String,
    options: List<T>,
    optionLabel: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        AssistChip(
            onClick = { expanded = true },
            label = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(label)
                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        expanded = false
                        onSelect(option)
                    },
                )
            }
        }
    }
}

@Composable
private fun PublishButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(20.dp),
    ) {
        Text(stringResource(R.string.map_publish), fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ResultsSheet(
    state: MapUiState,
    onOfferClick: (String) -> Unit,
    onWidenRadius: () -> Unit,
    onShowAllCategories: () -> Unit,
    onClearQuery: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(RESULTS_SHEET_HEIGHT_FRACTION),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        shadowElevation = 16.dp,
        color = Color.White,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .background(Color.LightGray, RoundedCornerShape(2.dp))
                )
            }
            if (state.locationStatus == LocationStatus.Denied || state.locationStatus == LocationStatus.Unavailable) {
                Text(
                    stringResource(R.string.map_location_fallback),
                    fontSize = 12.sp,
                    color = Color(0xFF777777),
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            when (val offers = state.offers) {
                OffersState.Loading -> CenteredBox { CircularProgressIndicator() }
                is OffersState.Content -> {
                    Text(
                        pluralStringResource(R.plurals.map_results_count, offers.offers.size, offers.offers.size, state.radiusKm),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                    OfferList(offers.offers, onOfferClick)
                }
                OffersState.Empty -> EmptyResults(state, onWidenRadius, onShowAllCategories, onClearQuery)
                is OffersState.Unavailable -> CenteredMessage(
                    title = stringResource(
                        if (offers.isOffline) R.string.map_unavailable_offline else R.string.map_unavailable_server
                    ),
                    action = { OutlinedButton(onClick = onRetry) { Text(stringResource(R.string.map_retry)) } },
                )
            }
        }
    }
}

@Composable
private fun EmptyResults(
    state: MapUiState,
    onWidenRadius: () -> Unit,
    onShowAllCategories: () -> Unit,
    onClearQuery: () -> Unit,
) {
    // A typed search is the likeliest cause, and widening the radius wouldn't fix it.
    if (state.query.isNotBlank()) {
        CenteredMessage(
            title = stringResource(R.string.map_empty_title_query, state.query.trim()),
            action = { OutlinedButton(onClick = onClearQuery) { Text(stringResource(R.string.map_clear_query)) } },
        )
        return
    }
    val nextRadius = SearchFilters.RADIUS_OPTIONS_KM.firstOrNull { it > state.radiusKm }
    CenteredMessage(
        title = if (state.category == null) {
            stringResource(R.string.map_empty_title_all_categories, state.radiusKm)
        } else {
            stringResource(R.string.map_empty_title, stringResource(state.category.labelRes()).lowercase(), state.radiusKm)
        },
        body = stringResource(R.string.map_empty_body),
        action = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (nextRadius != null) {
                    Button(onClick = onWidenRadius) { Text(stringResource(R.string.map_widen_radius, nextRadius)) }
                }
                if (state.category != null) {
                    OutlinedButton(onClick = onShowAllCategories) {
                        Text(stringResource(R.string.map_show_all_categories))
                    }
                }
            }
        },
    )
}

@Composable
private fun OfferList(offers: List<NearbyOffer>, onOfferClick: (String) -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(offers, key = { it.offer.id }) { nearby ->
            OfferItem(
                title = nearby.offer.title,
                subtitle = stringResource(nearby.offer.category.labelRes()),
                distance = formatDistance(nearby.distanceMeters),
                onClick = { onOfferClick(nearby.offer.id) },
            )
        }
    }
}

@Composable
private fun OfflineBanner(lastSyncedAt: Instant?, now: Instant, hasData: Boolean) {
    val minutes = lastSyncedAt?.let { Duration.between(it, now).toMinutes().coerceAtLeast(0) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF333333))
            .statusBarsPadding()
            .padding(14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = when {
                !hasData -> stringResource(R.string.map_offline_banner_no_data)
                minutes != null && minutes > 0 -> stringResource(R.string.map_offline_banner, minutes.toInt())
                else -> stringResource(R.string.map_offline_banner_now)
            },
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontSize = 14.sp,
        )
    }
}

@Composable
private fun OfflineOffers(
    offers: List<NearbyOffer>,
    onOfferClick: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .background(Color(0xFFF5F5F5), RoundedCornerShape(16.dp))
                .dashedBorder(color = Color.Gray, strokeWidth = 2.dp, cornerRadius = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    stringResource(R.string.map_offline_map_unavailable),
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF444444),
                    fontSize = 14.sp,
                )
                if (offers.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(stringResource(R.string.map_offline_showing_saved), color = Color(0xFF777777), fontSize = 12.sp)
                }
            }
        }
        if (offers.isEmpty()) {
            Text(
                stringResource(R.string.map_unavailable_offline),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
            )
        } else {
            Box(modifier = Modifier.weight(1f, fill = false)) { OfferList(offers, onOfferClick) }
        }
        OutlinedButton(
            onClick = onRetry,
            modifier = Modifier
                .width(200.dp)
                .height(48.dp),
            shape = RoundedCornerShape(24.dp),
        ) {
            Text(stringResource(R.string.map_retry), fontWeight = FontWeight.Bold, color = Color(0xFF333333))
        }
    }
}

@Composable
private fun CenteredBox(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
}

@Composable
private fun CenteredMessage(title: String, body: String? = null, action: @Composable () -> Unit = {}) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, textAlign = TextAlign.Center)
        if (body != null) Text(body, color = Color(0xFF777777), textAlign = TextAlign.Center)
        action()
    }
}

private fun Offer.toMarker() = MapMarker(id = id, position = location, title = title, snippet = address)

private val previewOffers = listOf(
    NearbyOffer(
        Offer(
            id = "p-1", title = "Ayudante de taller", category = Category.MECHANICS, description = "",
            schedule = "", jobType = "", businessName = "[COMERCIO]", address = "[DIRECCION]",
            location = MapUiState.DEFAULT_CENTER, updatedAt = Instant.EPOCH,
        ),
        distanceMeters = 400,
    ),
)

@Composable
private fun PreviewMapContent(state: MapUiState) = LaburinTheme {
    MapContent(state, Instant.EPOCH.plusSeconds(15 * 60), {}, {}, {}, {}, {}, {}, {}, {}, {}, {})
}

@Preview
@Composable
private fun MapContentPreview() =
    PreviewMapContent(MapUiState(locationStatus = LocationStatus.Granted, offers = OffersState.Content(previewOffers)))

@Preview
@Composable
private fun MapEmptyPreview() =
    PreviewMapContent(MapUiState(category = Category.MECHANICS, locationStatus = LocationStatus.Granted, offers = OffersState.Empty))

@Preview
@Composable
private fun MapOfflinePreview() = PreviewMapContent(
    MapUiState(isOnline = false, lastSyncedAt = Instant.EPOCH, offers = OffersState.Content(previewOffers))
)

@Preview
@Composable
private fun MapUnavailablePreview() =
    PreviewMapContent(MapUiState(locationStatus = LocationStatus.Denied, offers = OffersState.Unavailable(isOffline = true)))
