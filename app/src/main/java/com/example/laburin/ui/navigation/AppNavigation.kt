package com.example.laburin.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.laburin.model.Offer
import com.example.laburin.model.User
import com.example.laburin.model.UserType
import com.example.laburin.ui.screens.ApplicationsScreen
import com.example.laburin.ui.screens.LoginScreen
import com.example.laburin.ui.screens.MapScreen
import com.example.laburin.ui.screens.NoConnectionMapScreen
import com.example.laburin.ui.screens.OfferDetailScreen
import com.example.laburin.ui.screens.ProfileScreen
import com.example.laburin.ui.screens.PublishSearchScreen
import com.example.laburin.ui.screens.PublishedOfferDetails
import com.example.laburin.ui.screens.ScanScreen
import com.example.laburin.ui.screens.SignUpScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    var selectedOffer by remember { mutableStateOf<Offer?>(null) }
    var currentUser by remember { mutableStateOf<User?>(null) }

    NavHost(navController = navController, startDestination = "map") {
        composable("map") {
            MapScreen(
                onLoginClick = {
                    if (currentUser == null) navController.navigate("login")
                    else navController.navigate("profile")
                },
                onApplicationClick = { navController.navigate("applications") },
                onPublishClick = { navController.navigate("publish") },
                onScanClick = { navController.navigate("scan") },
                onOfferClick = { offer ->
                    selectedOffer = offer
                    navController.navigate("detail")
                },
                isLoggedIn = currentUser != null
            )
        }
        composable("offline") {
            NoConnectionMapScreen(
                onLoginClick = {
                    if (currentUser == null) navController.navigate("login")
                    else navController.navigate("profile")
                },
                onApplicationClick = { navController.navigate("applications") },
                onOfferClick = { offer ->
                    selectedOffer = offer
                    navController.navigate("detail")
                }
            )
        }
        composable("scan") {
            ScanScreen(
                onClose = { navController.popBackStack() }
            )
        }
        composable("login") {
            LoginScreen(
                onBack = { navController.popBackStack() },
                onSignUpClick = { navController.navigate("signup") },
                onLogin = { email ->
                    // TODO: el id lo va a generar el backend. Por ahora usamos el email.
                    currentUser = User(id = email, firstName = "", lastName = "", email = email, type = UserType.BUSINESS)
                    navController.popBackStack()
                }
            )
        }
        composable("signup") {
            SignUpScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable("profile") {
            currentUser?.let {
                user ->
            ProfileScreen(
                user = user,
                onSearchClick = { navController.navigate("map") },
                onApplicationClick = { navController.navigate("applications") },
                onOfferClick = { offer ->
                    selectedOffer = offer
                    navController.navigate("detail")
                },
                onLogout = {
                    currentUser = null
                    navController.popBackStack("map", inclusive = false)
                })
        }}
        composable("applications") {
            ApplicationsScreen(
                onSearchClick = { navController.navigate("map") },
                onProfileClick = { navController.navigate("profile") }
            )
        }
        composable("publish") {
            PublishSearchScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable("detail") {
            selectedOffer?.let { offer ->
                OfferDetailScreen(
                    offer,
                    currentUser,
                    onBack = { navController.popBackStack() },
                    onLogin = {
                        navController.navigate("login")
                    },
                    onCreateAccount = { navController.navigate("signup") })
            }
        }
        composable("published") {
            selectedOffer?.let { offer -> PublishedOfferDetails(offer) }
        }
    }
}
