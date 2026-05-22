package app.coreme.messenger.features.users.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import app.coreme.messenger.core.ui.components.AnimatedGradientBackground
import app.coreme.messenger.core.ui.components.CoremeButton
import app.coreme.messenger.core.ui.components.CoremeButtonVariant
import app.coreme.messenger.core.ui.components.CoremeTextField
import app.coreme.messenger.core.ui.components.UserAvatar
import app.coreme.messenger.core.ui.theme.CoremeColors
import app.coreme.messenger.core.ui.theme.InterFamily
import app.coreme.messenger.core.ui.theme.PlusJakartaSansFamily

@Composable
fun MyProfileScreen(
    onNavigateToSettings: () -> Unit,
    onLogout: () -> Unit = {},
    viewModel: MyProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    AnimatedGradientBackground(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        color = CoremeColors.accent,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(36.dp),
                    )
                }
            } else {
                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {

                    // Hero header
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xF0141618), Color(0xB0111316), Color(0x00000000)),
                                ),
                            ),
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(top = 12.dp, bottom = 28.dp),
                        ) {
                            // Top action row
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .height(48.dp),
                            ) {
                                // Left: dot + title
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(
                                                Brush.radialGradient(
                                                    listOf(CoremeColors.accent, CoremeColors.accentDim),
                                                ),
                                            ),
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "Профіль",
                                        style = TextStyle(
                                            fontFamily = PlusJakartaSansFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 24.sp,
                                            letterSpacing = (-0.3).sp,
                                            color = CoremeColors.textPrimary,
                                        ),
                                    )
                                }

                                // Right: action buttons
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (uiState.isEditing) {
                                        ProfileActionButton(
                                            icon = { Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp)) },
                                            onClick = viewModel::cancelEditing,
                                        )
                                    } else {
                                        ProfileActionButton(
                                            icon = { Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp)) },
                                            onClick = viewModel::startEditing,
                                        )
                                        ProfileActionButton(
                                            icon = { Icon(Icons.Default.Settings, null, modifier = Modifier.size(16.dp)) },
                                            onClick = onNavigateToSettings,
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(20.dp))

                            // Avatar with camera overlay
                            Box(contentAlignment = Alignment.BottomEnd) {
                                Box(
                                    modifier = Modifier
                                        .size(96.dp)
                                        .clip(CircleShape)
                                        .border(
                                            width = 2.dp,
                                            brush = Brush.linearGradient(
                                                listOf(CoremeColors.accent, CoremeColors.accentDim),
                                            ),
                                            shape = CircleShape,
                                        ),
                                ) {
                                    UserAvatar(
                                        displayName = uiState.profile?.displayName ?: "",
                                        avatarUrl = uiState.profile?.avatarUrl,
                                        size = 96.dp,
                                    )
                                }
                                if (uiState.isEditing) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(CoremeColors.accent)
                                            .border(2.dp, Color(0xFF111316), CircleShape),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CameraAlt,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp),
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(14.dp))

                            uiState.profile?.let { profile ->
                                if (!uiState.isEditing) {
                                    Text(
                                        text = profile.displayName,
                                        style = TextStyle(
                                            fontFamily = PlusJakartaSansFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 22.sp,
                                            letterSpacing = (-0.2).sp,
                                            color = CoremeColors.textPrimary,
                                        ),
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "@${profile.username}",
                                        style = TextStyle(
                                            fontFamily = InterFamily,
                                            fontSize = 14.sp,
                                            color = CoremeColors.accent,
                                        ),
                                    )
                                    if (!profile.bio.isNullOrBlank()) {
                                        Spacer(Modifier.height(10.dp))
                                        Text(
                                            text = profile.bio,
                                            style = TextStyle(
                                                fontFamily = InterFamily,
                                                fontSize = 14.sp,
                                                color = CoremeColors.textSecondary,
                                            ),
                                            modifier = Modifier.padding(horizontal = 32.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Edit form or profile content
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                    ) {
                        if (uiState.isEditing) {
                            Spacer(Modifier.height(8.dp))

                            // Section card
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(0x0CFFFFFF))
                                    .border(0.5.dp, Color(0x14FFFFFF), RoundedCornerShape(20.dp))
                                    .padding(20.dp),
                            ) {
                                Text(
                                    text = "Редагувати профіль",
                                    style = TextStyle(
                                        fontFamily = PlusJakartaSansFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 16.sp,
                                        color = CoremeColors.textPrimary,
                                    ),
                                )
                                Spacer(Modifier.height(16.dp))

                                CoremeTextField(
                                    value = uiState.displayName,
                                    onValueChange = viewModel::onDisplayNameChange,
                                    placeholder = "Ім'я",
                                    label = "Відображуване ім'я",
                                    modifier = Modifier.fillMaxWidth(),
                                )

                                Spacer(Modifier.height(14.dp))

                                CoremeTextField(
                                    value = uiState.bio,
                                    onValueChange = viewModel::onBioChange,
                                    placeholder = "Про себе",
                                    label = "Біографія (необов'язково)",
                                    modifier = Modifier.fillMaxWidth(),
                                )

                                Spacer(Modifier.height(20.dp))

                                CoremeButton(
                                    text = "Зберегти зміни",
                                    onClick = viewModel::saveProfile,
                                    isLoading = uiState.isSaving,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        } else {
                            // Info section
                            uiState.profile?.let { profile ->
                                Spacer(Modifier.height(4.dp))

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(Color(0x0AFFFFFF))
                                        .border(0.5.dp, Color(0x10FFFFFF), RoundedCornerShape(20.dp))
                                        .padding(vertical = 4.dp),
                                ) {
                                    ProfileInfoRow(label = "Ім'я", value = profile.displayName)
                                    ProfileInfoDivider()
                                    ProfileInfoRow(label = "Юзернейм", value = "@${profile.username}")
                                    if (!profile.email.isNullOrBlank()) {
                                        ProfileInfoDivider()
                                        ProfileInfoRow(label = "Email", value = profile.email)
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(24.dp))

                        // Danger section
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0x08E5A5A5))
                                .border(0.5.dp, CoremeColors.danger.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                                .padding(16.dp),
                        ) {
                            CoremeButton(
                                text = "Вийти з акаунту",
                                onClick = { viewModel.logout(onLogout) },
                                variant = CoremeButtonVariant.Danger,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                        Spacer(Modifier.height(120.dp))
                    }
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun ProfileActionButton(
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0x0CFFFFFF))
            .border(0.5.dp, Color(0x14FFFFFF), RoundedCornerShape(10.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
    ) {
        Box(modifier = Modifier.size(16.dp)) {
            icon()
        }
    }
}

@Composable
private fun ProfileInfoRow(label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 13.dp),
    ) {
        Text(
            text = label,
            style = TextStyle(
                fontFamily = InterFamily,
                fontSize = 13.sp,
                color = CoremeColors.textTertiary,
            ),
            modifier = Modifier.width(90.dp),
        )
        Text(
            text = value,
            style = TextStyle(
                fontFamily = InterFamily,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = CoremeColors.textPrimary,
            ),
        )
    }
}

@Composable
private fun ProfileInfoDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .padding(horizontal = 16.dp)
            .background(Color(0x0AFFFFFF)),
    )
}
