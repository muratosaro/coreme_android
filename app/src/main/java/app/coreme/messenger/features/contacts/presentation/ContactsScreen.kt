package app.coreme.messenger.features.contacts.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import app.coreme.messenger.core.ui.components.AnimatedGradientBackground
import app.coreme.messenger.core.ui.components.CoremeTextField
import app.coreme.messenger.core.ui.components.UserAvatar
import app.coreme.messenger.core.ui.theme.CoremeColors
import app.coreme.messenger.core.ui.theme.InterFamily
import app.coreme.messenger.core.ui.theme.PlusJakartaSansFamily
import app.coreme.messenger.features.contacts.domain.model.Contact
import app.coreme.messenger.features.users.domain.model.UserProfile

@Composable
fun ContactsScreen(
    onOpenProfile: (String) -> Unit,
    viewModel: ContactsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showSearch by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    AnimatedGradientBackground(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {

                // Custom top bar matching ChatsScreen
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(listOf(Color(0xE81A1C22), Color(0xB0141618))),
                        )
                        .border(width = 0.5.dp, color = Color(0x0FFFFFFF)),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(start = 20.dp, end = 10.dp)
                            .height(56.dp),
                    ) {
                        if (showSearch) {
                            Box(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                CoremeTextField(
                                    value = uiState.searchQuery,
                                    onValueChange = viewModel::onSearchQueryChange,
                                    placeholder = "Пошук за іменем або @username",
                                    prefixIcon = Icons.Default.Search,
                                )
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f),
                            ) {
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
                                    text = "Контакти",
                                    style = TextStyle(
                                        fontFamily = PlusJakartaSansFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 24.sp,
                                        letterSpacing = (-0.3).sp,
                                        color = CoremeColors.textPrimary,
                                    ),
                                )
                                if (uiState.contacts.isNotEmpty()) {
                                    Spacer(Modifier.width(8.dp))
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(CoremeColors.glassThick)
                                            .border(0.5.dp, CoremeColors.glassBorder, RoundedCornerShape(8.dp))
                                            .padding(horizontal = 7.dp, vertical = 2.dp),
                                    ) {
                                        Text(
                                            text = "${uiState.contacts.size}",
                                            color = CoremeColors.textSecondary,
                                            fontSize = 11.sp,
                                            fontFamily = InterFamily,
                                            fontWeight = FontWeight.Medium,
                                        )
                                    }
                                }
                            }
                        }

                        // Search toggle
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (showSearch) CoremeColors.accent.copy(alpha = 0.15f) else Color(0x0CFFFFFF),
                                )
                                .then(
                                    if (showSearch) Modifier.border(
                                        0.5.dp,
                                        CoremeColors.accent.copy(alpha = 0.3f),
                                        RoundedCornerShape(12.dp),
                                    ) else Modifier
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) {
                                    showSearch = !showSearch
                                    if (!showSearch) viewModel.onSearchQueryChange("")
                                },
                        ) {
                            Icon(
                                imageVector = if (showSearch) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = null,
                                tint = if (showSearch) CoremeColors.accent else CoremeColors.textSecondary,
                                modifier = Modifier.size(19.dp),
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                    }
                }

                if (uiState.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            color = CoremeColors.accent,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(36.dp),
                        )
                    }
                } else if (showSearch && uiState.searchQuery.length >= 2) {
                    SearchResultsList(
                        results = uiState.searchResults,
                        isSearching = uiState.isSearching,
                        isAddingContact = uiState.isAddingContact,
                        contactIds = uiState.contacts.map { it.id }.toSet(),
                        onAddContact = viewModel::addContact,
                        onOpenProfile = onOpenProfile,
                    )
                } else {
                    ContactsList(
                        contacts = uiState.contacts,
                        removingContact = uiState.removingContact,
                        onRemove = viewModel::removeContact,
                        onOpenProfile = onOpenProfile,
                    )
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .statusBarsPadding(),
            )
        }
    }
}

@Composable
private fun ContactsList(
    contacts: List<Contact>,
    removingContact: String?,
    onRemove: (String) -> Unit,
    onOpenProfile: (String) -> Unit,
) {
    val navBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    if (contacts.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(Color(0x0EFFFFFF))
                        .border(1.dp, Color(0x14FFFFFF), CircleShape),
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = null,
                        tint = CoremeColors.textTertiary,
                        modifier = Modifier.size(40.dp),
                    )
                }
                Spacer(Modifier.height(20.dp))
                Text(
                    text = "Контактів немає",
                    style = TextStyle(
                        fontFamily = PlusJakartaSansFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 20.sp,
                        color = CoremeColors.textSecondary,
                    ),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Знайдіть людей через пошук",
                    style = TextStyle(
                        fontFamily = InterFamily,
                        fontSize = 14.sp,
                        color = CoremeColors.textTertiary,
                    ),
                )
            }
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(
            top = 6.dp,
            bottom = navBottomPadding + 88.dp,
            start = 14.dp,
            end = 14.dp,
        ),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(contacts, key = { it.id }) { contact ->
            ContactItem(
                contact = contact,
                isRemoving = removingContact == contact.id,
                onRemove = { onRemove(contact.id) },
                onClick = { onOpenProfile(contact.id) },
            )
        }
    }
}

@Composable
private fun ContactItem(
    contact: Contact,
    isRemoving: Boolean,
    onRemove: () -> Unit,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x0AFFFFFF), RoundedCornerShape(16.dp))
            .border(0.5.dp, Color(0x10FFFFFF), RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 11.dp),
    ) {
        UserAvatar(
            displayName = contact.visibleName,
            avatarUrl = contact.avatarUrl,
            size = 48.dp,
            isOnline = contact.isOnline,
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = contact.visibleName,
                style = TextStyle(
                    fontFamily = InterFamily,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CoremeColors.textPrimary,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "@${contact.username}",
                style = TextStyle(
                    fontFamily = InterFamily,
                    fontSize = 12.sp,
                    color = CoremeColors.textTertiary,
                ),
            )
        }

        if (isRemoving) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = CoremeColors.danger,
                strokeWidth = 2.dp,
            )
        } else {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0x0CFFFFFF))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onRemove,
                    ),
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Видалити",
                    tint = CoremeColors.textTertiary.copy(alpha = 0.6f),
                    modifier = Modifier.size(17.dp),
                )
            }
        }
    }
}

@Composable
private fun SearchResultsList(
    results: List<UserProfile>,
    isSearching: Boolean,
    isAddingContact: String?,
    contactIds: Set<String>,
    onAddContact: (String) -> Unit,
    onOpenProfile: (String) -> Unit,
) {
    if (isSearching) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = CoremeColors.accent, strokeWidth = 2.dp, modifier = Modifier.size(36.dp))
        }
        return
    }

    if (results.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "🔍", fontSize = 48.sp)
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Користувачів не знайдено",
                    style = TextStyle(
                        fontFamily = InterFamily,
                        fontSize = 16.sp,
                        color = CoremeColors.textTertiary,
                    ),
                )
            }
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(top = 6.dp, bottom = 100.dp, start = 14.dp, end = 14.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(results, key = { it.id }) { user ->
            SearchResultItem(
                user = user,
                isContact = contactIds.contains(user.id),
                isAdding = isAddingContact == user.id,
                onAdd = { onAddContact(user.id) },
                onClick = { onOpenProfile(user.id) },
            )
        }
    }
}

@Composable
private fun SearchResultItem(
    user: UserProfile,
    isContact: Boolean,
    isAdding: Boolean,
    onAdd: () -> Unit,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x0AFFFFFF), RoundedCornerShape(16.dp))
            .border(0.5.dp, Color(0x10FFFFFF), RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 11.dp),
    ) {
        UserAvatar(
            displayName = user.displayName,
            avatarUrl = user.avatarUrl,
            size = 48.dp,
            isOnline = user.isOnline,
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.displayName,
                style = TextStyle(
                    fontFamily = InterFamily,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CoremeColors.textPrimary,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "@${user.username}",
                style = TextStyle(fontFamily = InterFamily, fontSize = 12.sp, color = CoremeColors.textTertiary),
            )
        }

        if (!isContact) {
            if (isAdding) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = CoremeColors.accent,
                    strokeWidth = 2.dp,
                )
            } else {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(CoremeColors.accent.copy(alpha = 0.15f))
                        .border(0.5.dp, CoremeColors.accent.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onAdd,
                        )
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            tint = CoremeColors.accent,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Додати",
                            style = TextStyle(
                                fontFamily = InterFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CoremeColors.accent,
                            ),
                        )
                    }
                }
            }
        } else {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0x0AFFFFFF))
                    .border(0.5.dp, Color(0x10FFFFFF), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 7.dp),
            ) {
                Text(
                    text = "В контактах",
                    style = TextStyle(
                        fontFamily = InterFamily,
                        fontSize = 11.sp,
                        color = CoremeColors.textTertiary,
                    ),
                )
            }
        }
    }
}
