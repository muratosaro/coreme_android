package app.coreme.messenger.features.chats.presentation.newchat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import app.coreme.messenger.core.ui.components.UserAvatar
import app.coreme.messenger.core.ui.theme.CoremeColors
import app.coreme.messenger.features.users.domain.model.UserProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewChatScreen(
    onNavigateToChat: (String) -> Unit,
    onNavigateToCreateGroup: () -> Unit,
    onBack: () -> Unit,
    viewModel: NewChatViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.createdChatId) {
        state.createdChatId?.let { onNavigateToChat(it) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CoremeColors.backgroundBase)
            .systemBarsPadding(),
    ) {
        TopAppBar(
            title = { Text("New Message", color = Color.White, fontWeight = FontWeight.SemiBold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
                }
            },
            actions = {
                IconButton(onClick = onNavigateToCreateGroup) {
                    Text("Group", color = CoremeColors.hermesOrange, fontSize = 14.sp)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = CoremeColors.backgroundBase),
        )

        OutlinedTextField(
            value = state.query,
            onValueChange = viewModel::onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Search by username…", color = Color.Gray) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CoremeColors.hermesOrange,
                unfocusedBorderColor = Color.Gray,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = CoremeColors.hermesOrange,
            ),
            singleLine = true,
        )

        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CoremeColors.hermesOrange)
            }
        } else {
            LazyColumn {
                items(state.results, key = { it.id }) { user ->
                    UserResultItem(user = user, onClick = { viewModel.startChat(user.id) })
                }
            }
        }

        state.error?.let { err ->
            LaunchedEffect(err) {
                viewModel.clearError()
            }
        }
    }
}

@Composable
private fun UserResultItem(user: UserProfile, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        UserAvatar(
            displayName = user.displayName,
            avatarUrl = user.avatarUrl,
            isOnline = user.isOnline,
            size = 44.dp,
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(user.displayName, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 15.sp)
            Text("@${user.username}", color = Color.Gray, fontSize = 13.sp)
        }
    }
}
