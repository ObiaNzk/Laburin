package com.example.laburin.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.res.stringResource
import com.example.laburin.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import com.example.laburin.ui.screens.scan.ScanRoute
import com.example.laburin.ui.components.AppTab
import com.example.laburin.ui.components.LaburinNavigationBar
import com.example.laburin.ui.screens.applications.ApplicationsRoute
import com.example.laburin.ui.screens.auth.LoginRoute
import com.example.laburin.ui.screens.auth.SignUpRoute
import com.example.laburin.ui.screens.detail.OfferDetailRoute
import com.example.laburin.ui.screens.map.MapRoute
import com.example.laburin.ui.screens.profile.ProfileRoute
import com.example.laburin.ui.screens.publish.BusinessSignUpRoute
import com.example.laburin.ui.screens.publish.PublishRoute
import com.example.laburin.ui.screens.publish.PublishedOfferRoute

/** App shell: the bottom bar is shown only on top-level destinations. */
@Composable
fun LaburinNavHost(modifier: Modifier = Modifier, shell: AppShellViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val isLoggedIn by shell.isLoggedIn.collectAsStateWithLifecycle()
    val isSessionExpired by shell.isSessionExpired.collectAsStateWithLifecycle()
    val destination = navController.currentBackStackEntryAsState().value?.destination
    val tab = when {
        destination?.hasRoute<MapDestination>() == true -> AppTab.SEARCH
        destination?.hasRoute<ProfileDestination>() == true -> AppTab.ACCOUNT
        destination?.hasRoute<ApplicationsDestination>() == true -> AppTab.APPLICATIONS
        else -> null
    }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (tab != null) {
                LaburinNavigationBar(
                    selected = tab,
                    isLoggedIn = isLoggedIn,
                    onSearchClick = { navController.openTab(MapDestination) },
                    onApplicationsClick = {
                        if (isLoggedIn) navController.openTab(ApplicationsDestination) else navController.navigate(LoginDestination)
                    },
                    onAccountClick = {
                        if (isLoggedIn) navController.openTab(ProfileDestination) else navController.navigate(LoginDestination)
                    },
                )
            }
        },
    ) { innerPadding ->
        if (isSessionExpired) {
            SessionExpiredDialog(
                onLogin = {
                    shell.onExpiredNoticeDismissed()
                    navController.navigate(LoginDestination)
                },
                onLater = shell::onExpiredNoticeDismissed,
            )
        }
        NavHost(
            navController = navController,
            startDestination = MapDestination,
            modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding()),
        ) {
            composable<MapDestination> {
                MapRoute(
                    onNavigateToOffer = { offerId, distance ->
                        navController.navigate(OfferDetailDestination(offerId, distance))
                    },
                    onNavigateToPublish = { navController.navigate(PublishDestination) },
                    onNavigateToScan = { navController.navigate(ScanDestination) },
                )
            }
            composable<ScanDestination> {
                ScanRoute(
                    onOpenOffer = { offerId ->
                        navController.navigate(OfferDetailDestination(offerId, fromQr = true)) {
                            popUpTo<ScanDestination> { inclusive = true }
                        }
                    },
                    onClose = { navController.popBackStack() },
                )
            }
            composable<OfferDetailDestination>(
                deepLinks = listOf(navDeepLink<OfferDetailDestination>(basePath = OFFER_DEEP_LINK_BASE)),
            ) {
                OfferDetailRoute(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToSignUp = { navController.navigate(SignUpDestination) },
                    onNavigateToLogin = { navController.navigate(LoginDestination) },
                    onNavigateToCvUpload = { navController.openTab(ProfileDestination) },
                )
            }
            composable<LoginDestination> {
                LoginRoute(
                    onLoggedIn = { navController.leaveAuthFlow() },
                    onSignUpClick = { navController.navigate(SignUpDestination) },
                    onClose = { navController.popBackStack() },
                )
            }
            composable<SignUpDestination> {
                SignUpRoute(
                    onSignedUp = { navController.leaveAuthFlow() },
                    onClose = { navController.popBackStack() },
                )
            }
            composable<PublishDestination> {
                PublishRoute(
                    onPublished = { offerId ->
                        navController.navigate(PublishedOfferDestination(offerId)) {
                            popUpTo<PublishDestination> { inclusive = true }
                        }
                    },
                    onNeedsBusinessAccount = { navController.navigate(BusinessSignUpDestination) },
                    onBack = { navController.popBackStack() },
                )
            }
            composable<BusinessSignUpDestination> {
                BusinessSignUpRoute(
                    onSignedUp = { navController.leaveAuthFlow() },
                    onLogin = { navController.navigate(LoginDestination) },
                    onClose = { navController.popBackStack() },
                )
            }
            composable<PublishedOfferDestination> {
                PublishedOfferRoute(onBack = { navController.popBackStack() })
            }
            composable<ApplicationsDestination> {
                ApplicationsRoute(
                    onLogin = { navController.navigate(LoginDestination) },
                    onOpenOffer = { offerId -> navController.navigate(OfferDetailDestination(offerId)) },
                )
            }
            composable<ProfileDestination> {
                ProfileRoute(
                    onLoggedOut = { navController.openTab(MapDestination) },
                    onLogin = { navController.navigate(LoginDestination) },
                    onMyOfferClick = { offerId -> navController.navigate(PublishedOfferDestination(offerId)) },
                )
            }
        }
    }
}

/**
 * Back to wherever the user was before the auth screens (the offer they wanted to see, the form they were filling).
 * Pops the whole chain, e.g. business sign-up → "Ya tengo cuenta" → login.
 */
private fun NavController.leaveAuthFlow() {
    // Outermost first: business sign-up can lead to login, and login to candidate sign-up, never the other way.
    val firstAuthScreen = runCatching { getBackStackEntry<BusinessSignUpDestination>() }.getOrNull()
        ?: runCatching { getBackStackEntry<LoginDestination>() }.getOrNull()
        ?: runCatching { getBackStackEntry<SignUpDestination>() }.getOrNull()
        ?: return
    popBackStack(firstAuthScreen.destination.id, inclusive = true)
}

private fun NavController.openTab(destination: Any) = navigate(destination) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

/** §13: the session expired; pending actions are kept and sent after logging in again. */
@Composable
private fun SessionExpiredDialog(onLogin: () -> Unit, onLater: () -> Unit) {
    AlertDialog(
        onDismissRequest = onLater,
        title = { Text(stringResource(R.string.session_expired_title)) },
        text = { Text(stringResource(R.string.session_expired_body)) },
        confirmButton = { TextButton(onClick = onLogin) { Text(stringResource(R.string.session_expired_login)) } },
        dismissButton = { TextButton(onClick = onLater) { Text(stringResource(R.string.session_expired_later)) } },
    )
}
