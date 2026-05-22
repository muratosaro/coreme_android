package app.coreme.messenger.features.auth.presentation.register

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import app.coreme.messenger.core.ui.components.AnimatedGradientBackground
import app.coreme.messenger.core.ui.components.CoremeTextField
import app.coreme.messenger.core.ui.theme.CoremeColors
import app.coreme.messenger.core.ui.theme.InterFamily
import app.coreme.messenger.core.ui.theme.PlusJakartaSansFamily
import app.coreme.messenger.features.auth.presentation.login.LogoWordmark

@Composable
fun RegisterScreen(
    onNavigateToLogin: () -> Unit,
    onRegisterSuccess: () -> Unit,
    viewModel: RegisterViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) onRegisterSuccess()
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    AnimatedGradientBackground(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {

                // ── Hero ────────────────────────────────────────────────────
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(top = 44.dp, bottom = 32.dp),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(contentAlignment = Alignment.Center) {
                            Box(
                                modifier = Modifier
                                    .size(90.dp)
                                    .background(
                                        Brush.radialGradient(
                                            listOf(
                                                CoremeColors.accent.copy(alpha = 0.15f),
                                                Color.Transparent,
                                            ),
                                        ),
                                    ),
                            )
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(52.dp)
                                    .shadow(
                                        elevation = 22.dp,
                                        shape = RoundedCornerShape(16.dp),
                                        spotColor = CoremeColors.accent.copy(alpha = 0.5f),
                                        ambientColor = CoremeColors.accent.copy(alpha = 0.25f),
                                    )
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFFF0914A), CoremeColors.accent, Color(0xFFBF5E18)),
                                        ),
                                    )
                                    .border(
                                        1.dp,
                                        Brush.linearGradient(listOf(Color(0x80FFFFFF), Color(0x10FFFFFF))),
                                        RoundedCornerShape(16.dp),
                                    ),
                            ) {
                                Text(
                                    text = "C",
                                    style = TextStyle(
                                        fontFamily = PlusJakartaSansFamily,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 24.sp,
                                        color = Color(0xFF0D0D12),
                                        letterSpacing = (-0.5).sp,
                                    ),
                                )
                            }
                        }

                        Spacer(Modifier.height(14.dp))
                        LogoWordmark(fontSize = 36)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Приєднуйтесь до спільноти",
                            style = TextStyle(
                                fontFamily = InterFamily,
                                fontSize = 13.sp,
                                color = CoremeColors.textTertiary,
                                letterSpacing = 0.2.sp,
                            ),
                        )
                    }
                }

                // ── Form card ───────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .shadow(
                            elevation = 20.dp,
                            shape = RoundedCornerShape(28.dp),
                            spotColor = Color(0x40000000),
                            ambientColor = Color(0x20000000),
                        )
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color(0xFF1C1E26))
                        .border(0.7.dp, Color(0x20FFFFFF), RoundedCornerShape(28.dp)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color.Transparent, Color(0x30FFFFFF), Color.Transparent),
                                ),
                            ),
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 28.dp),
                    ) {
                        Text(
                            text = "Створити акаунт",
                            style = TextStyle(
                                fontFamily = PlusJakartaSansFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                letterSpacing = (-0.3).sp,
                                color = CoremeColors.textPrimary,
                            ),
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Заповніть поля нижче",
                            style = TextStyle(
                                fontFamily = InterFamily,
                                fontSize = 14.sp,
                                color = CoremeColors.textTertiary,
                            ),
                        )

                        Spacer(Modifier.height(24.dp))

                        CoremeTextField(
                            value = uiState.displayName,
                            onValueChange = viewModel::onDisplayNameChange,
                            placeholder = "Ваше ім'я",
                            label = "Ім'я та прізвище",
                            prefixIcon = Icons.Outlined.Badge,
                            enabled = !uiState.isLoading,
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        )

                        Spacer(Modifier.height(12.dp))

                        CoremeTextField(
                            value = uiState.username,
                            onValueChange = viewModel::onUsernameChange,
                            placeholder = "@username",
                            label = "Ім'я користувача",
                            prefixIcon = Icons.Outlined.Person,
                            enabled = !uiState.isLoading,
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        )

                        Spacer(Modifier.height(12.dp))

                        CoremeTextField(
                            value = uiState.email,
                            onValueChange = viewModel::onEmailChange,
                            placeholder = "email@example.com",
                            label = "Email",
                            prefixIcon = Icons.Outlined.Email,
                            enabled = !uiState.isLoading,
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next,
                            ),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        )

                        Spacer(Modifier.height(12.dp))

                        CoremeTextField(
                            value = uiState.password,
                            onValueChange = viewModel::onPasswordChange,
                            placeholder = "Мінімум 8 символів",
                            label = "Пароль",
                            prefixIcon = Icons.Outlined.Lock,
                            enabled = !uiState.isLoading,
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done,
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    viewModel.register()
                                },
                            ),
                        )

                        Spacer(Modifier.height(24.dp))

                        // Primary CTA
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .shadow(
                                    elevation = 14.dp,
                                    shape = RoundedCornerShape(16.dp),
                                    spotColor = CoremeColors.accent.copy(alpha = 0.5f),
                                    ambientColor = CoremeColors.accent.copy(alpha = 0.25f),
                                )
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (uiState.isLoading)
                                        Brush.linearGradient(listOf(Color(0xFF666670), Color(0xFF666670)))
                                    else
                                        Brush.linearGradient(listOf(Color(0xFFF0914A), CoremeColors.accent, Color(0xFFBF5E18))),
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = {
                                        if (!uiState.isLoading) {
                                            focusManager.clearFocus()
                                            viewModel.register()
                                        }
                                    },
                                ),
                        ) {
                            Text(
                                text = if (uiState.isLoading) "Реєстрація…" else "Зареєструватись",
                                style = TextStyle(
                                    fontFamily = PlusJakartaSansFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    letterSpacing = 0.2.sp,
                                    color = Color(0xFF0D0D12),
                                ),
                            )
                        }

                        Spacer(Modifier.height(18.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0x10FFFFFF), thickness = 0.7.dp)
                            Text(
                                text = "  вже є акаунт?  ",
                                style = TextStyle(fontFamily = InterFamily, fontSize = 12.sp, color = CoremeColors.textTertiary),
                            )
                            HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0x10FFFFFF), thickness = 0.7.dp)
                        }

                        Spacer(Modifier.height(14.dp))

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0x0CFFFFFF))
                                .border(
                                    0.7.dp,
                                    Brush.linearGradient(
                                        listOf(
                                            CoremeColors.accent.copy(alpha = 0.5f),
                                            CoremeColors.accentDim.copy(alpha = 0.3f),
                                        ),
                                    ),
                                    RoundedCornerShape(16.dp),
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { if (!uiState.isLoading) onNavigateToLogin() },
                                ),
                        ) {
                            Text(
                                text = "Увійти до акаунту",
                                style = TextStyle(
                                    fontFamily = PlusJakartaSansFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = CoremeColors.accent,
                                    letterSpacing = 0.1.sp,
                                ),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(48.dp))
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp),
            )
        }
    }
}
