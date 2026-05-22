package app.coreme.messenger.features.chats.presentation.creategroup

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import app.coreme.messenger.features.contacts.domain.model.Contact

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupScreen(
    onNavigateToChat: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: CreateGroupViewModel = hiltViewModel(),
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
            title = { Text("New Group", color = Color.White, fontWeight = FontWeight.SemiBold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = CoremeColors.backgroundBase),
        )

        OutlinedTextField(
            value = state.groupName,
            onValueChange = viewModel::onGroupNameChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Group name", color = Color.Gray) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CoremeColors.hermesOrange,
                unfocusedBorderColor = Color.Gray,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = CoremeColors.hermesOrange,
            ),
            singleLine = true,
        )

        Text(
            text = "Select members (${state.selectedIds.size} selected)",
            color = Color.Gray,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )

        if (state.isLoading && state.contacts.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CoremeColors.hermesOrange)
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(state.contacts, key = { it.id }) { contact ->
                    ContactSelectionItem(
                        contact = contact,
                        isSelected = contact.id in state.selectedIds,
                        onToggle = { viewModel.toggleSelection(contact.id) },
                    )
                }
            }
        }

        Button(
            onClick = viewModel::createGroup,
            enabled = state.groupName.isNotBlank() && state.selectedIds.isNotEmpty() && !state.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CoremeColors.hermesOrange),
        ) {
            Text("Create Group", color = Color.White, fontWeight = FontWeight.SemiBold)
        }

        state.error?.let {
            LaunchedEffect(it) { viewModel.clearError() }
        }
    }
}

@Composable
private fun ContactSelectionItem(contact: Contact, isSelected: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        UserAvatar(
            displayName = contact.visibleName,
            avatarUrl = contact.avatarUrl,
            isOnline = contact.isOnline,
            size = 44.dp,
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(contact.visibleName, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 15.sp)
            Text("@${contact.username}", color = Color.Gray, fontSize = 13.sp)
        }
        if (isSelected) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = CoremeColors.hermesOrange,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}
