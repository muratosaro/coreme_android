package app.coreme.messenger.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.coreme.messenger.features.auth.presentation.login.LoginScreen
import app.coreme.messenger.features.auth.presentation.register.RegisterScreen
import app.coreme.messenger.features.auth.presentation.splash.SplashScreen
import app.coreme.messenger.features.calls.presentation.active.ActiveCallScreen
import app.coreme.messenger.features.channels.presentation.detail.ChannelDetailScreen
import app.coreme.messenger.features.channels.presentation.list.ChannelsScreen
import app.coreme.messenger.features.chats.presentation.creategroup.CreateGroupScreen
import app.coreme.messenger.features.chats.presentation.detail.ChatDetailScreen
import app.coreme.messenger.features.chats.presentation.newchat.NewChatScreen
import app.coreme.messenger.features.main.MainScreen
import app.coreme.messenger.features.settings.presentation.ChangePasswordScreen
import app.coreme.messenger.features.settings.presentation.SettingsScreen
import app.coreme.messenger.features.users.presentation.profile.UserProfileScreen

@Composable
fun CoremeNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH,
        modifier = modifier,
    ) {
        composable(Routes.SPLASH) {
            SplashScreen(
                onNavigateToLogin = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
                onNavigateToMain = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.LOGIN) {
            LoginScreen(
                onNavigateToRegister = { navController.navigate(Routes.REGISTER) },
                onLoginSuccess = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.REGISTER) {
            RegisterScreen(
                onNavigateToLogin = { navController.popBackStack() },
                onRegisterSuccess = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.NEW_CHAT) {
            NewChatScreen(
                onNavigateToChat = { chatId ->
                    navController.navigate(Routes.chatDetail(chatId)) {
                        popUpTo(Routes.NEW_CHAT) { inclusive = true }
                    }
                },
                onNavigateToCreateGroup = { navController.navigate(Routes.CREATE_GROUP) },
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.CREATE_GROUP) {
            CreateGroupScreen(
                onNavigateToChat = { chatId ->
                    navController.navigate(Routes.chatDetail(chatId)) {
                        popUpTo(Routes.NEW_CHAT) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.MAIN) {
            MainScreen(
                onNavigateToChat = { chatId ->
                    navController.navigate(Routes.chatDetail(chatId))
                },
                onNavigateToSettings = {
                    navController.navigate(Routes.SETTINGS)
                },
                onNavigateToUserProfile = { userId ->
                    navController.navigate(Routes.userProfile(userId))
                },
                onNavigateToChannels = {
                    navController.navigate(Routes.CHANNELS)
                },
                onNavigateToActiveCall = { callId ->
                    navController.navigate(Routes.activeCall(callId))
                },
                onNavigateToNewChat = {
                    navController.navigate(Routes.NEW_CHAT)
                },
                onLogout = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.MAIN) { inclusive = true }
                    }
                },
            )
        }

        composable(
            route = Routes.CHAT_DETAIL,
            arguments = listOf(navArgument("chatId") { type = NavType.StringType }),
        ) {
            ChatDetailScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = Routes.USER_PROFILE,
            arguments = listOf(navArgument("userId") { type = NavType.StringType }),
        ) {
            UserProfileScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onNavigateToChangePassword = { navController.navigate(Routes.CHANGE_PASSWORD) },
            )
        }

        composable(Routes.CHANGE_PASSWORD) {
            ChangePasswordScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = Routes.ACTIVE_CALL,
            arguments = listOf(navArgument("callId") { type = NavType.StringType }),
        ) {
            ActiveCallScreen(onCallEnded = { navController.popBackStack() })
        }

        composable(Routes.CHANNELS) {
            ChannelsScreen(
                onBack = { navController.popBackStack() },
                onOpenChannel = { channelId ->
                    navController.navigate(Routes.channelDetail(channelId))
                },
            )
        }

        composable(
            route = Routes.CHANNEL_DETAIL,
            arguments = listOf(navArgument("channelId") { type = NavType.StringType }),
        ) {
            ChannelDetailScreen(onBack = { navController.popBackStack() })
        }
    }
}
