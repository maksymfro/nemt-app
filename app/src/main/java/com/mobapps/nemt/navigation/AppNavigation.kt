package com.mobapps.nemt.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.mobapps.nemt.data.UserProfile
import com.mobapps.nemt.data.UserProfileRepository
import com.mobapps.nemt.ui.components.BottomNavigationBar
import com.mobapps.nemt.ui.screens.LoginScreen
import com.mobapps.nemt.ui.screens.RegisterScreen
import com.mobapps.nemt.ui.screens.VerifyEmailScreen
import com.mobapps.nemt.ui.screens.WelcomeScreen
import com.mobapps.nemt.ui.screens.BookingScreen
import com.mobapps.nemt.ui.screens.HomeScreen
import com.mobapps.nemt.ui.screens.HelpSupportScreen
import com.mobapps.nemt.ui.screens.ProfileScreen
import com.mobapps.nemt.ui.screens.TermsPrivacyScreen
import com.mobapps.nemt.ui.screens.TripOverviewScreen
import com.mobapps.nemt.ui.screens.TripsScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val auth = remember { FirebaseAuth.getInstance() }
    val currentUser = auth.currentUser
    var currentProfile by remember(currentUser?.uid) { mutableStateOf<UserProfile?>(null) }
    val startDestination = remember(currentUser?.uid, currentUser?.isEmailVerified) {
        when {
            currentUser == null -> Routes.Welcome.route
            currentUser.isEmailVerified -> Routes.Home.route
            else -> Routes.VerifyEmail.route
        }
    }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val authEmail = auth.currentUser?.email.orEmpty()
    val userEmail = currentProfile?.email ?: authEmail
    val userName = currentProfile?.firstName?.takeIf { it.isNotBlank() }
        ?: auth.currentUser?.displayName?.takeIf { it.isNotBlank() }
        ?: authEmail.substringBefore("@").replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase() else char.toString()
        }

    LaunchedEffect(currentUser?.uid) {
        if (currentUser == null) {
            currentProfile = null
            return@LaunchedEffect
        }

        UserProfileRepository.ensureProfile(currentUser) { result ->
            result.onSuccess { profile ->
                currentProfile = profile
            }
        }

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) return@addOnCompleteListener
            val token = task.result ?: return@addOnCompleteListener
            val uid = auth.currentUser?.uid ?: return@addOnCompleteListener
            FirebaseFirestore.getInstance().collection("users").document(uid)
                .set(mapOf("fcmToken" to token), SetOptions.merge())
        }
    }

    val showBottomBar = currentRoute == Routes.Home.route ||
            currentRoute == Routes.Trips.route ||
            currentRoute == Routes.Profile.route

    val navigateToRoot: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.id) {
                inclusive = true
            }
            launchSingleTop = true
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomNavigationBar(
                    currentRoute = currentRoute,
                    onItemClick = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->

        NavHost(
            navController = navController,
            startDestination = startDestination
        ) {
            composable(Routes.Welcome.route) {
                WelcomeScreen(
                    onLoginClick = { navController.navigate(Routes.Login.route) },
                    onRegisterClick = { navController.navigate(Routes.Register.route) }
                )
            }

            composable(Routes.Login.route) {
                LoginScreen(
                    onBack = { navController.popBackStack() },
                    onOpenRegister = { navController.navigate(Routes.Register.route) },
                    onLoginSuccess = { navigateToRoot(Routes.Home.route) },
                    onNeedsVerification = { navigateToRoot(Routes.VerifyEmail.route) }
                )
            }

            composable(Routes.Register.route) {
                RegisterScreen(
                    onBack = { navController.popBackStack() },
                    onOpenLogin = { navController.navigate(Routes.Login.route) },
                    onRegistrationPendingVerification = {
                        navigateToRoot(Routes.VerifyEmail.route)
                    }
                )
            }

            composable(Routes.VerifyEmail.route) {
                VerifyEmailScreen(
                    email = auth.currentUser?.email.orEmpty(),
                    onVerified = { navigateToRoot(Routes.Home.route) },
                    onLogout = {
                        auth.signOut()
                        navigateToRoot(Routes.Welcome.route)
                    }
                )
            }

            composable(Routes.Home.route) {
                HomeScreen(
                    userName = userName.ifBlank { "Passenger" },
                    onGoToTrips = {
                        navController.navigate(Routes.Trips.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onGoToProfile = {
                        navController.navigate(Routes.Profile.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onGoToBooking = {
                        navController.navigate(Routes.Booking.route)
                    },
                    onNeedAssistanceClick = {
                        navController.navigate(Routes.HelpSupport.route)
                    },
                    contentPadding = innerPadding
                )
            }

            composable(Routes.Trips.route) {
                TripsScreen(
                    onBack = { navController.popBackStack() },
                    contentPadding = innerPadding
                )
            }

            composable(Routes.Profile.route) {
                ProfileScreen(
                    authEmail = authEmail,
                    onLogout = {
                        currentProfile = null
                        auth.signOut()
                        navigateToRoot(Routes.Welcome.route)
                    },
                    onProfileUpdated = { updatedProfile ->
                        currentProfile = updatedProfile
                    },
                    onOpenHelpSupport = {
                        navController.navigate(Routes.HelpSupport.route)
                    },
                    onOpenTermsPrivacy = {
                        navController.navigate(Routes.TermsPrivacy.route)
                    },
                    contentPadding = innerPadding
                )
            }

            composable(Routes.HelpSupport.route) {
                HelpSupportScreen(
                    onBack = { navController.popBackStack() },
                    contentPadding = innerPadding
                )
            }

            composable(Routes.TermsPrivacy.route) {
                TermsPrivacyScreen(
                    onBack = { navController.popBackStack() },
                    contentPadding = innerPadding
                )
            }

            composable(Routes.Booking.route) {
                BookingScreen(
                    onBack = { navController.popBackStack() },
                    onTripConfirmed = {
                        navController.navigate(Routes.TripOverview.route) {
                            popUpTo(Routes.Booking.route) { inclusive = true }
                        }
                    },
                    contentPadding = innerPadding
                )
            }

            composable(Routes.TripOverview.route) {
                TripOverviewScreen(
                    onClose = { navController.popBackStack() },
                    contentPadding = innerPadding
                )
            }
        }
    }
}
