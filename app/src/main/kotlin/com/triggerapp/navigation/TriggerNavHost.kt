package com.triggerapp.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.triggerapp.core.common.navigation.TriggerRoutes
import com.triggerapp.core.strings.TriggerStrings
import com.triggerapp.core.ui.theme.TriggerTheme as AppTheme
import com.triggerapp.feature.auth.ui.forgot.ForgotRoute
import com.triggerapp.feature.auth.ui.login.LoginRoute
import com.triggerapp.feature.auth.ui.register.RegisterRoute
import com.triggerapp.feature.chat.ui.ConversationRoute
import com.triggerapp.feature.chat.ui.PeerProfileRoute
import com.triggerapp.feature.home.ui.home.HomeRoute
import com.triggerapp.presence.AppProcessPresenceEffect
import com.triggerapp.splash.SplashRoute

/**
 * Root Compose entry: applies [AppTheme] and hosts [TriggerNavHost]. No parameters.
 *
 * @author udit
 */
@Composable
fun TriggerApp() {
    AppTheme(darkTheme = true, dynamicColor = false, useBrandDarkColors = true) {
        AppProcessPresenceEffect()
        TriggerNavHost(modifier = Modifier.fillMaxSize())
    }
}

/**
 * [NavHost] for splash, auth, home shell, chat, and peer profile destinations.
 *
 * @param modifier Modifier applied to the [NavHost].
 * @author udit
 */
@Composable
fun TriggerNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = TriggerRoutes.SPLASH,
        modifier = modifier,
    ) {
        composable(TriggerRoutes.SPLASH) {
            SplashRoute(navController = navController)
        }
        composable(TriggerRoutes.LOGIN) {
            LoginRoute(
                onSuccess = {
                    navController.navigate(TriggerRoutes.HOME) {
                        popUpTo(TriggerRoutes.SPLASH) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onRegister = { navController.navigate(TriggerRoutes.REGISTER) },
                onForgot = { navController.navigate(TriggerRoutes.FORGOT) },
            )
        }
        composable(TriggerRoutes.REGISTER) {
            RegisterRoute(
                onSuccess = {
                    navController.navigate(TriggerRoutes.HOME) {
                        popUpTo(TriggerRoutes.SPLASH) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onLogin = { navController.popBackStack() },
            )
        }
        composable(TriggerRoutes.FORGOT) {
            ForgotRoute(onBack = { navController.popBackStack() })
        }
        composable(
            route = TriggerRoutes.HOME,
            exitTransition = { homeExitTransition() },
            popEnterTransition = { homePopEnterTransition() },
        ) {
            HomeRoute(
                onSignOut = {
                    navController.navigate(TriggerRoutes.LOGIN) {
                        popUpTo(TriggerRoutes.HOME) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onOpenChat = { peerId ->
                    navController.navigate(TriggerRoutes.chat(peerId))
                },
            )
        }
        composable(
            route = TriggerRoutes.CHAT_PATTERN,
            arguments = listOf(
                navArgument(TriggerStrings.Nav.ARG_PEER_ID) { type = NavType.StringType },
            ),
            enterTransition = { chatEnterTransition() },
            exitTransition = { chatExitTransition() },
            popEnterTransition = { chatPopEnterTransition() },
            popExitTransition = { chatPopExitTransition() },
        ) {
            ConversationRoute(
                onBack = { navController.popBackStack() },
                onOpenPeerProfile = { userId ->
                    navController.navigate(TriggerRoutes.peerProfile(userId))
                },
            )
        }
        composable(
            route = TriggerRoutes.PEER_PROFILE_PATTERN,
            arguments = listOf(
                navArgument(TriggerStrings.Nav.ARG_PROFILE_USER_ID) { type = NavType.StringType },
            ),
            enterTransition = { peerProfileEnterTransition() },
            popExitTransition = { peerProfilePopExitTransition() },
        ) {
            PeerProfileRoute(onBack = { navController.popBackStack() })
        }
    }
}
