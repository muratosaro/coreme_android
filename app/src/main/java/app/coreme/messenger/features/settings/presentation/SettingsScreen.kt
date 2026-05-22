package app.coreme.messenger.features.settings.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import app.coreme.messenger.core.ui.theme.CoremeColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToChangePassword: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CoremeColors.backgroundBase),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("Settings", color = CoremeColors.textPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = CoremeColors.textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CoremeColors.hermesOrange)
                }
            } else {
                uiState.settings?.let { settings ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .systemBarsPadding()
                            .padding(horizontal = 16.dp),
                    ) {
                        SettingsSectionHeader("Notifications")

                        SettingsToggleRow(
                            label = "Enable Notifications",
                            checked = settings.notificationsEnabled,
                            onCheckedChange = { viewModel.toggle { s -> s.copy(notificationsEnabled = it) } },
                        )

                        SettingsToggleRow(
                            label = "Sound",
                            checked = settings.soundEnabled,
                            enabled = settings.notificationsEnabled,
                            onCheckedChange = { viewModel.toggle { s -> s.copy(soundEnabled = it) } },
                        )

                        HorizontalDivider(color = CoremeColors.glassBorder, modifier = Modifier.padding(vertical = 8.dp))

                        SettingsSectionHeader("Privacy")

                        SettingsToggleRow(
                            label = "Show Read Receipts",
                            checked = settings.showReadReceipts,
                            onCheckedChange = { viewModel.toggle { s -> s.copy(showReadReceipts = it) } },
                        )

                        SettingsToggleRow(
                            label = "Show Last Seen",
                            checked = settings.lastSeenVisible,
                            onCheckedChange = { viewModel.toggle { s -> s.copy(lastSeenVisible = it) } },
                        )

                        HorizontalDivider(color = CoremeColors.glassBorder, modifier = Modifier.padding(vertical = 8.dp))

                        SettingsSectionHeader("Auto-Reply")

                        SettingsToggleRow(
                            label = "Enable Auto-Reply",
                            checked = settings.autoReplyEnabled,
                            onCheckedChange = { viewModel.toggle { s -> s.copy(autoReplyEnabled = it) } },
                        )

                        HorizontalDivider(color = CoremeColors.glassBorder, modifier = Modifier.padding(vertical = 8.dp))

                        SettingsSectionHeader("Account")

                        TextButton(
                            onClick = onNavigateToChangePassword,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        ) {
                            Text(
                                text = "Change Password",
                                color = CoremeColors.hermesOrange,
                                fontSize = 15.sp,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .systemBarsPadding(),
        )
    }
}

@Composable
private fun SettingsSectionHeader(text: String) {
    Text(
        text = text,
        color = CoremeColors.hermesOrange,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun SettingsToggleRow(
    label: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    ) {
        Text(
            text = label,
            color = if (enabled) CoremeColors.textPrimary else CoremeColors.textTertiary,
            fontSize = 15.sp,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = CoremeColors.hermesOrange,
                checkedTrackColor = CoremeColors.hermesOrangeMuted.copy(alpha = 0.4f),
            ),
        )
    }
}
