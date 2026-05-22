package app.coreme.messenger.features.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.coreme.messenger.core.ui.components.BottomNavBar
import app.coreme.messenger.features.calls.presentation.history.CallHistoryScreen
import app.coreme.messenger.features.chats.presentation.list.ChatsScreen
import app.coreme.messenger.features.contacts.presentation.ContactsScreen
import app.coreme.messenger.features.users.presentation.profile.MyProfileScreen
import app.coreme.messenger.navigation.Routes

@Composable
fun MainScreen(
    onNavigateToChat: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToUserProfile: (String) -> Unit,
    onNavigateToChannels: () -> Unit,
    onNavigateToActiveCall: (String) -> Unit,
    onNavigateToNewChat: () -> Unit = {},
    onLogout: () -> Unit,
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Routes.CHATS

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = Routes.CHATS,
            modifier = Modifier.fillMaxSize(),
        ) {
            composable(Routes.CHATS) {
                ChatsScreen(
                    onOpenChat = onNavigateToChat,
                    onNavigateToChannels = onNavigateToChannels,
                    onNavigateToNewChat = onNavigateToNewChat,
                )
            }
            composable(Routes.CALLS) {
                CallHistoryScreen(onNavigateToActiveCall = onNavigateToActiveCall)
            }
            composable(Routes.CONTACTS) {
                ContactsScreen(onOpenProfile = onNavigateToUserProfile)
            }
            composable(Routes.PROFILE) {
                MyProfileScreen(
                    onNavigateToSettings = onNavigateToSettings,
                    onLogout = onLogout,
                )
            }
        }

        BottomNavBar(
            currentRoute = currentRoute,
            onNavigate = { route ->
                navController.navigate(route) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}
