package com.example.laburin.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
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
import com.example.laburin.ui.components.AppTab
import com.example.laburin.ui.components.LaburinNavigationBar
import com.example.laburin.ui.screens.applications.ApplicationsRoute
import com.example.laburin.ui.screens.auth.LoginRoute
import com.example.laburin.ui.screens.auth.SignUpRoute
import com.example.laburin.ui.screens.detail.OfferDetailRoute
import com.example.laburin.ui.screens.map.MapRoute
import com.example.laburin.ui.screens.profile.ProfileRoute

/** App shell: the bottom bar is shown only on top-level destinations. */
@Composable
fun LaburinNavHost(modifier: Modifier = Modifier, shell: AppShellViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val isLoggedIn by shell.isLoggedIn.collectAsStateWithLifecycle()
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
                )
            }
            composable<OfferDetailDestination> {
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
                )
            }
        }
    }
}

/** Back to wherever the user was before login/sign-up (e.g. the offer they wanted to see). */
private fun NavController.leaveAuthFlow() {
    if (!popBackStack<LoginDestination>(inclusive = true)) popBackStack<SignUpDestination>(inclusive = true)
}

private fun NavController.openTab(destination: Any) = navigate(destination) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}
