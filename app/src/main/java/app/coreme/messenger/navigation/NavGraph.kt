package app.coreme.messenger.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import app.coreme.messenger.core.ui.components.AnimatedGradientBackground
import app.coreme.messenger.core.ui.theme.CoremeColors

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
            SplashPlaceholder()
        }
        composable(Routes.LOGIN) {
            // Implemented in Etap 2
            SplashPlaceholder()
        }
        composable(Routes.CHATS) {
            // Implemented in Etap 3
            SplashPlaceholder()
        }
    }
}

@Composable
private fun SplashPlaceholder() {
    AnimatedGradientBackground(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "CoreMe",
                style = MaterialTheme.typography.displayMedium,
                color = CoremeColors.textPrimary,
                fontWeight = FontWeight.Light,
            )
        }
    }
}
