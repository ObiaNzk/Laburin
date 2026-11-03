package com.example.laburin.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.laburin.ui.screens.SignUpScreen
import com.example.laburin.ui.screens.detail.OfferDetailRoute
import com.example.laburin.ui.screens.map.MapRoute

@Composable
fun LaburinNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = MapDestination, modifier = modifier) {
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
            )
        }
        composable<SignUpDestination> {
            SignUpScreen(onClose = { navController.popBackStack() })
        }
    }
}
