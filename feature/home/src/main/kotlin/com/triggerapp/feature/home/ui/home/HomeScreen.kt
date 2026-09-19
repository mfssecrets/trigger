package com.triggerapp.feature.home.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.DynamicFeed
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.ModeComment
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.triggerapp.core.strings.TriggerStrings
import com.triggerapp.core.ui.R as UiR
import com.triggerapp.core.ui.TriggerPacificoFamily
import com.triggerapp.core.ui.TriggerProfileAvatar
import com.triggerapp.core.ui.theme.TriggerScreenBackground
import com.triggerapp.core.ui.theme.TriggerTheme
import com.triggerapp.core.ui.triggerImeInsetPadding
import com.triggerapp.domain.model.User
import com.triggerapp.feature.home.presentation.conversations.ConversationsUiEvent
import com.triggerapp.feature.home.presentation.conversations.ConversationsViewModel
import com.triggerapp.feature.home.presentation.shell.HomeUiEvent
import com.triggerapp.feature.home.presentation.shell.HomeViewModel
import com.triggerapp.feature.home.presentation.users.UsersUiEvent
import com.triggerapp.feature.home.presentation.users.UsersViewModel
import com.triggerapp.feature.home.ui.feed.FeedsTab
import com.triggerapp.feature.profile.ui.ProfileRoute
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import org.koin.androidx.compose.koinViewModel

private val AppBarBlack = Color.Black
private val MutedTab = Color(0xFFAFACAC)
private val SearchHint = Color(0xFF797B7E)
private val TabActive = Color(0xFF63FFA3)

private enum class HeaderMenuDialog {
    None,
    Verification,
    Settings,
    MySite,
    Wallet,
    Help,
}
/**
 * Home shell UI: top bar (page title + 3-dot menu), 56dp mobile nav (30% reduced), and five tabs.
 *
 * @param isOnline When false, shows the offline banner under the tabs.
 * @param selectedTabIndex Selected tab index (`0` Chats, `1` Feeds, `2` Notifications, `3` Users, `4` Profile).
 * @param onTabSelected Called when the user selects a tab.
 * @param currentUser Signed-in user for dialog contexts.
 * @param tab0 Composable content for the Chats tab.
 * @param tab1 Composable content for the Feeds tab.
 * @param tab2 Composable content for the Notifications tab.
 * @param tab3 Composable content for the Users tab.
 * @param tab4 Composable content for the Profile tab.
 * @author udit
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeMainLayout(
    isOnline: Boolean,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    currentUser: User?,
    tab0: @Composable () -> Unit,
    tab1: @Composable () -> Unit,
    tab2: @Composable () -> Unit,
    tab3: @Composable () -> Unit,
    tab4: @Composable () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }
    var activeMenuDialog by remember { mutableStateOf(HeaderMenuDialog.None) }

    val pageTitle = when (selectedTabIndex) {
        0 -> "Chats"
        1 -> "Feeds"
        2 -> "Notifications"
        3 -> "Users"
        4 -> "Profile"
        else -> "Trigger"
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = TriggerScreenBackground,
        bottomBar = {
            // Mobile navigation with 30% reduced height (56dp vs default 80dp)
            // Surface stays dark edge-to-edge behind the system bar, while navigationBarsPadding lifts the content
            Surface(
                color = AppBarBlack,
                tonalElevation = 4.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val navTabs = listOf(
                        Triple(0, "Chats", Icons.Filled.ChatBubble),
                        Triple(1, "Feeds", Icons.Filled.DynamicFeed),
                        Triple(2, "Notifications", Icons.Filled.Notifications),
                        Triple(3, "Users", Icons.Filled.Group),
                        Triple(4, "Profile", Icons.Filled.Person),
                    )

                    navTabs.forEach { (index, title, icon) ->
                        val isSelected = selectedTabIndex == index
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { onTabSelected(index) },
                                )
                                .padding(vertical = 4.dp),
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .height(28.dp)
                                    .width(48.dp)
                                    .background(
                                        color = if (isSelected) Color(0xFF1F2B23) else Color.Transparent,
                                        shape = RoundedCornerShape(14.dp),
                                    ),
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = title,
                                    tint = if (isSelected) TabActive else MutedTab,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = title,
                                color = if (isSelected) Color.White else MutedTab,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        },
        topBar = {
            Surface(
                color = AppBarBlack,
                tonalElevation = 0.dp,
            ) {
                // Generous top padding ensures header content never overlaps with the status bar
                val statusBarInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                val safeTopPadding = maxOf(statusBarInset, 36.dp)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = safeTopPadding)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Left: Page Title (Avatar & Profile name removed)
                    Text(
                        text = pageTitle,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp,
                    )

                    // Right: 3-dot overflow menu
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Menu options",
                                tint = Color.White,
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier
                                .background(Color(0xFF1E242B))
                                .widthIn(min = 190.dp),
                        ) {
                            DropdownMenuItem(
                                text = { Text("Verification", color = Color.White, fontSize = 14.sp) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Outlined.Verified,
                                        contentDescription = null,
                                        tint = TabActive,
                                        modifier = Modifier.size(20.dp),
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    activeMenuDialog = HeaderMenuDialog.Verification
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Settings", color = Color.White, fontSize = 14.sp) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Outlined.Settings,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp),
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    activeMenuDialog = HeaderMenuDialog.Settings
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("My Site", color = Color.White, fontSize = 14.sp) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Outlined.Language,
                                        contentDescription = null,
                                        tint = Color(0xFF64B5F6),
                                        modifier = Modifier.size(20.dp),
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    activeMenuDialog = HeaderMenuDialog.MySite
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Wallet", color = Color.White, fontSize = 14.sp) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Outlined.AccountBalanceWallet,
                                        contentDescription = null,
                                        tint = Color(0xFFFFD54F),
                                        modifier = Modifier.size(20.dp),
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    activeMenuDialog = HeaderMenuDialog.Wallet
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Help", color = Color.White, fontSize = 14.sp) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Outlined.HelpOutline,
                                        contentDescription = null,
                                        tint = Color(0xFF81C784),
                                        modifier = Modifier.size(20.dp),
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    activeMenuDialog = HeaderMenuDialog.Help
                                },
                            )
                        }
                    }
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(TriggerScreenBackground),
        ) {
            if (!isOnline) {
                Surface(color = Color(0xFF5C3A2E)) {
                    Text(
                        text = TriggerStrings.Ui.OFFLINE_INDICATOR,
                        color = Color.White,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }
            when (selectedTabIndex) {
                0 -> tab0()
                1 -> tab1()
                2 -> tab2()
                3 -> tab3()
                4 -> tab4()
            }
        }
    }

    when (activeMenuDialog) {
        HeaderMenuDialog.Verification -> {
            AlertDialog(
                onDismissRequest = { activeMenuDialog = HeaderMenuDialog.None },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Verified, contentDescription = null, tint = TabActive)
                        Spacer(Modifier.width(8.dp))
                        Text("Account Verification", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Get the verified badge next to your profile handle.", color = Color(0xFFCFD8DC), fontSize = 14.sp)
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("✓", color = TabActive, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(8.dp))
                            Text("Email Address Confirmed", color = Color.White, fontSize = 13.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("✓", color = TabActive, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(8.dp))
                            Text("Realtime Presence Active", color = Color.White, fontSize = 13.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("○", color = Color(0xFFFFD54F), fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(8.dp))
                            Text("Identity Badge: Ready to Submit", color = Color.White, fontSize = 13.sp)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { activeMenuDialog = HeaderMenuDialog.None },
                        colors = ButtonDefaults.buttonColors(containerColor = TabActive, contentColor = Color.Black),
                    ) {
                        Text("Request Badge", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { activeMenuDialog = HeaderMenuDialog.None }) {
                        Text("Close", color = Color.White)
                    }
                },
                containerColor = Color(0xFF1E242B),
                shape = RoundedCornerShape(16.dp),
            )
        }
        HeaderMenuDialog.Settings -> {
            AlertDialog(
                onDismissRequest = { activeMenuDialog = HeaderMenuDialog.None },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Settings, contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("Settings", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("App Preferences", color = TabActive, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("• Push Notifications: Enabled", color = Color.White, fontSize = 13.sp)
                        Text("• Privacy: Everyone can message", color = Color.White, fontSize = 13.sp)
                        Text("• Dark Mode: Permanent Active", color = Color.White, fontSize = 13.sp)
                        Text("• Media Cache: 4.2 MB", color = Color(0xFFAFACAC), fontSize = 12.sp)
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { activeMenuDialog = HeaderMenuDialog.None },
                        colors = ButtonDefaults.buttonColors(containerColor = TabActive, contentColor = Color.Black),
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { activeMenuDialog = HeaderMenuDialog.None }) {
                        Text("Cancel", color = Color.White)
                    }
                },
                containerColor = Color(0xFF1E242B),
                shape = RoundedCornerShape(16.dp),
            )
        }
        HeaderMenuDialog.MySite -> {
            AlertDialog(
                onDismissRequest = { activeMenuDialog = HeaderMenuDialog.None },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Language, contentDescription = null, tint = Color(0xFF64B5F6))
                        Spacer(Modifier.width(8.dp))
                        Text("My Site", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    val handle = currentUser?.username?.lowercase()?.replace(" ", "") ?: "user"
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Your public web profile and invite URL:", color = Color(0xFFCFD8DC), fontSize = 14.sp)
                        Surface(
                            color = Color(0xFF121519),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                        ) {
                            Text(
                                "https://trigger.app/@$handle",
                                color = Color(0xFF64B5F6),
                                fontSize = 14.sp,
                                modifier = Modifier.padding(12.dp),
                            )
                        }
                        Text("Share this link with your contacts to chat directly on Trigger.", color = Color(0xFFAFACAC), fontSize = 12.sp)
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { activeMenuDialog = HeaderMenuDialog.None },
                        colors = ButtonDefaults.buttonColors(containerColor = TabActive, contentColor = Color.Black),
                    ) {
                        Text("Copy Link", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { activeMenuDialog = HeaderMenuDialog.None }) {
                        Text("Close", color = Color.White)
                    }
                },
                containerColor = Color(0xFF1E242B),
                shape = RoundedCornerShape(16.dp),
            )
        }
        HeaderMenuDialog.Wallet -> {
            AlertDialog(
                onDismissRequest = { activeMenuDialog = HeaderMenuDialog.None },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.AccountBalanceWallet, contentDescription = null, tint = Color(0xFFFFD54F))
                        Spacer(Modifier.width(8.dp))
                        Text("Trigger Wallet", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Available Balance", color = Color(0xFFAFACAC), fontSize = 13.sp)
                        Text("$0.00 USD", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Text("0 Trigger Credits", color = TabActive, fontSize = 14.sp)
                        Spacer(Modifier.height(4.dp))
                        Text("Send tips to friends and manage microtransactions in chats securely.", color = Color(0xFFCFD8DC), fontSize = 13.sp)
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { activeMenuDialog = HeaderMenuDialog.None },
                        colors = ButtonDefaults.buttonColors(containerColor = TabActive, contentColor = Color.Black),
                    ) {
                        Text("Add Funds", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { activeMenuDialog = HeaderMenuDialog.None }) {
                        Text("Close", color = Color.White)
                    }
                },
                containerColor = Color(0xFF1E242B),
                shape = RoundedCornerShape(16.dp),
            )
        }
        HeaderMenuDialog.Help -> {
            AlertDialog(
                onDismissRequest = { activeMenuDialog = HeaderMenuDialog.None },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.HelpOutline, contentDescription = null, tint = Color(0xFF81C784))
                        Spacer(Modifier.width(8.dp))
                        Text("Help & Support", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Need help with your Trigger account?", color = Color.White, fontSize = 14.sp)
                        Text("• Email: support@triggerapp.com", color = Color(0xFFCFD8DC), fontSize = 13.sp)
                        Text("• Frequently Asked Questions", color = Color(0xFF64B5F6), fontSize = 13.sp)
                        Text("• Privacy Policy & Terms of Service", color = Color(0xFF64B5F6), fontSize = 13.sp)
                        Spacer(Modifier.height(4.dp))
                        Text("Trigger App v1.0.0 (Production)", color = Color(0xFFAFACAC), fontSize = 12.sp)
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { activeMenuDialog = HeaderMenuDialog.None },
                        colors = ButtonDefaults.buttonColors(containerColor = TabActive, contentColor = Color.Black),
                    ) {
                        Text("Done", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { activeMenuDialog = HeaderMenuDialog.None }) {
                        Text("Contact Us", color = Color.White)
                    }
                },
                containerColor = Color(0xFF1E242B),
                shape = RoundedCornerShape(16.dp),
            )
        }
        HeaderMenuDialog.None -> Unit
    }
}

/**
 * Home feature route: wires [HomeViewModel] and embeds chats/feeds/notifications/users/profile tabs via [HomeMainLayout].
 *
 * @param onSignOut Navigate after local sign-out (e.g. to login).
 * @param onOpenChat Navigate to the chat thread for the given partner user id.
 * @param homeViewModel Shell MVI [HomeViewModel].
 * @author udit
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeRoute(
    onSignOut: () -> Unit,
    onOpenChat: (String) -> Unit,
    onOpenEditProfile: () -> Unit = {},
    homeViewModel: HomeViewModel = koinViewModel(),
) {
    LaunchedEffect(Unit) {
        homeViewModel.onEvent(HomeUiEvent.RegisterPushToken)
    }

    val homeState by homeViewModel.state.collectAsStateWithLifecycle()
    val isOnline = homeState.isOnline

    var tabIndex by rememberSaveable { mutableIntStateOf(0) }

    HomeMainLayout(
        isOnline = isOnline,
        selectedTabIndex = tabIndex,
        onTabSelected = { tabIndex = it },
        currentUser = homeState.currentUser,
        tab0 = { ConversationsTab(onOpenChat = onOpenChat) },
        tab1 = { FeedsTab(currentUser = homeState.currentUser, onOpenChat = onOpenChat) },
        tab2 = { NotificationsTab() },
        tab3 = { UsersTab(onOpenChat = onOpenChat) },
        tab4 = {
            ProfileRoute(
                onSignOut = {
                    homeViewModel.onEvent(HomeUiEvent.SignOut)
                    onSignOut()
                },
                onOpenEditProfile = onOpenEditProfile,
            )
        },
    )
}

private data class NotificationItem(
    val id: String,
    val title: String,
    val body: String,
    val timeAgo: String,
    val isUnread: Boolean,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

@Composable
private fun NotificationsTab() {
    var notifications by remember {
        mutableStateOf(
            listOf(
                NotificationItem(
                    id = "1",
                    title = "New Message",
                    body = "Jordan sent you a new message in Chats.",
                    timeAgo = "5m ago",
                    isUnread = true,
                    icon = Icons.Filled.ChatBubble,
                ),
                NotificationItem(
                    id = "2",
                    title = "Post Liked",
                    body = "Alex River liked your post in Feeds.",
                    timeAgo = "25m ago",
                    isUnread = true,
                    icon = Icons.Filled.Favorite,
                ),
                NotificationItem(
                    id = "3",
                    title = "New Connection",
                    body = "Cleopatra accepted your conversation invite.",
                    timeAgo = "2h ago",
                    isUnread = false,
                    icon = Icons.Filled.Group,
                ),
                NotificationItem(
                    id = "4",
                    title = "System Verification",
                    body = "Your account security status was updated.",
                    timeAgo = "1d ago",
                    isUnread = false,
                    icon = Icons.Outlined.Verified,
                ),
            ),
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TriggerScreenBackground)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "${notifications.count { it.isUnread }} unread notifications",
                color = Color(0xFFAFACAC),
                fontSize = 13.sp,
            )
            TextButton(
                onClick = {
                    notifications = notifications.map { it.copy(isUnread = false) }
                },
            ) {
                Text("Mark all read", color = TabActive, fontSize = 13.sp)
            }
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(notifications, key = { it.id }) { item ->
                NotificationRowCard(item = item)
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun NotificationRowCard(item: NotificationItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isUnread) Color(0xFF1C2229) else Color(0xFF15191E),
        ),
        shape = RoundedCornerShape(10.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(if (item.isUnread) Color(0xFF1F2B23) else Color(0xFF26303A)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = if (item.isUnread) TabActive else Color(0xFFAFACAC),
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        item.title,
                        color = Color.White,
                        fontWeight = if (item.isUnread) FontWeight.Bold else FontWeight.SemiBold,
                        fontSize = 14.sp,
                    )
                    Text(item.timeAgo, color = Color(0xFF797B7E), fontSize = 11.sp)
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    item.body,
                    color = if (item.isUnread) Color(0xFFCFD8DC) else Color(0xFF90A4AE),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                )
            }
            if (item.isUnread) {
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(TabActive),
                )
            }
        }
    }
}

private val PreviewHomeUser = User(
    id = "1",
    username = "Jordan",
    emailId = "",
    timestamp = "0",
    imageUrl = "",
    bio = "",
    status = TriggerStrings.Defaults.PRESENCE_ONLINE,
    searchKey = "j",
)

/**
 * Compose preview for [HomeMainLayout] on the Chats tab with sample [UserRow] items.
 * @author udit
 */
@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, showSystemUi = false, name = "Home · Chats tab")
@Composable
private fun HomeMainLayoutChatsPreview() {
    var tab by remember { mutableIntStateOf(0) }
    TriggerTheme(darkTheme = true, dynamicColor = false, useBrandDarkColors = true) {
        HomeMainLayout(
            isOnline = true,
            selectedTabIndex = tab,
            onTabSelected = { tab = it },
            currentUser = PreviewHomeUser,
            tab0 = {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(4) { UserRow(user = PreviewHomeUser, onClick = {}) }
                }
            },
            tab1 = { Box(Modifier.fillMaxSize()) },
            tab2 = { Box(Modifier.fillMaxSize()) },
            tab3 = { Box(Modifier.fillMaxSize()) },
            tab4 = { Box(Modifier.fillMaxSize()) },
        )
    }
}

/**
 * Compose preview for [HomeMainLayout] with the offline connectivity banner visible.
 * @author udit
 */
@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, showSystemUi = false, name = "Home · offline banner")
@Composable
private fun HomeMainLayoutOfflinePreview() {
    var tab by remember { mutableIntStateOf(0) }
    TriggerTheme(darkTheme = true, dynamicColor = false, useBrandDarkColors = true) {
        HomeMainLayout(
            isOnline = false,
            selectedTabIndex = tab,
            onTabSelected = { tab = it },
            currentUser = PreviewHomeUser,
            tab0 = {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item { UserRow(user = PreviewHomeUser, onClick = {}) }
                }
            },
            tab1 = { Box(Modifier.fillMaxSize()) },
            tab2 = { Box(Modifier.fillMaxSize()) },
            tab3 = { Box(Modifier.fillMaxSize()) },
            tab4 = { Box(Modifier.fillMaxSize()) },
        )
    }
}

/**
 * Compose preview for a single [UserRow] on the app background.
 * @author udit
 */
@Preview(showBackground = true, showSystemUi = false, name = "User row")
@Composable
private fun UserRowPreview() {
    TriggerTheme(darkTheme = true, dynamicColor = false, useBrandDarkColors = true) {
        Box(Modifier.background(TriggerScreenBackground)) {
            UserRow(user = PreviewHomeUser, onClick = {})
        }
    }
}

/**
 * Chats tab: empty state, error + retry, or a paged [LazyColumn] of partners with a load-more footer.
 *
 * Near-end scroll dispatches [ConversationsUiEvent.LoadMore] through [LazyListNearEndLoadEffect].
 *
 * @param onOpenChat Opens chat for the selected partner id.
 * @param viewModel MVI ConversationsUiState + [ConversationsViewModel.onEvent].
 * @author udit
 */
@Composable
private fun ConversationsTab(
    onOpenChat: (String) -> Unit,
    viewModel: ConversationsViewModel = koinViewModel(),
) {
    val ui by viewModel.state.collectAsStateWithLifecycle()
    val partners = ui.partners
    val listError = ui.listError
    val isInitialLoading = ui.isInitialLoading
    val isLoadingMore = ui.isLoadingMore
    val hasMore = ui.hasMore
    val listState = rememberLazyListState()
    LazyListNearEndLoadEffect(
        listState = listState,
        itemCount = partners.size,
        hasMore = hasMore,
        isLoadingMore = isLoadingMore,
        onLoadMore = { viewModel.onEvent(ConversationsUiEvent.LoadMore) },
    )
    when {
        isInitialLoading && partners.isEmpty() && listError == null -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(TriggerScreenBackground),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        }
        partners.isEmpty() -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(TriggerScreenBackground)
                    .padding(top = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                listError?.let { err ->
                    Text(
                        text = err,
                        color = Color(0xFFFFAB91),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 24.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { viewModel.onEvent(ConversationsUiEvent.RetryList) }) {
                        Text(TriggerStrings.Ui.TRY_AGAIN)
                    }
                    Spacer(Modifier.height(24.dp))
                }
                if (listError == null) {
                    Image(
                        painter = painterResource(UiR.drawable.ic_trigger_empty_chats),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(330.dp)
                            .padding(horizontal = 15.dp, vertical = 20.dp),
                        contentScale = ContentScale.Fit,
                    )
                    Text(
                        text = TriggerStrings.Ui.NO_CHATS_YET,
                        color = Color.White,
                        fontFamily = TriggerPacificoFamily,
                        fontSize = 25.sp,
                        modifier = Modifier.padding(top = 30.dp),
                    )
                }
            }
        }
        else -> {
            Column(Modifier.fillMaxSize()) {
                listError?.let { err ->
                    Surface(color = Color(0xFF3E2723)) {
                        Column(Modifier.padding(10.dp)) {
                            Text(err, color = Color(0xFFFFCCBC), fontSize = 13.sp)
                            TextButton(onClick = { viewModel.onEvent(ConversationsUiEvent.RetryList) }) {
                                Text(TriggerStrings.Ui.TRY_AGAIN, color = Color.White)
                            }
                        }
                    }
                }
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(TriggerScreenBackground),
                ) {
                    items(partners, key = { it.id }) { user ->
                        UserRow(user = user, onClick = { onOpenChat(user.id) })
                    }
                    if (isLoadingMore || hasMore) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (isLoadingMore) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(28.dp),
                                        color = Color(0xFF63FFA3),
                                        strokeWidth = 2.dp,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Users tab: search field, error surface, paged [LazyColumn], and end-of-list loading footer.
 *
 * **Search / paging:** Backed by [UsersViewModel]; near-end scroll dispatches [UsersUiEvent.LoadMore] via
 * [LazyListNearEndLoadEffect].
 *
 * **IME:** [androidx.compose.ui.platform.LocalFocusManager] clears focus when the list scrolls so the keyboard
 * does not re-open during pagination. The column uses [triggerImeInsetPadding] only (no `imeNestedScroll` on the list).
 *
 * @param onOpenChat Starts a chat with the tapped user id.
 * @param viewModel MVI [UsersUiState] + [UsersViewModel.onEvent].
 * @author udit
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UsersTab(
    onOpenChat: (String) -> Unit,
    viewModel: UsersViewModel = koinViewModel(),
) {
    val ui by viewModel.state.collectAsStateWithLifecycle()
    val users = ui.users
    val search = ui.searchFieldText
    val listError = ui.listError
    val isInitialLoading = ui.isInitialLoading
    val isLoadingMore = ui.isLoadingMore
    val hasMore = ui.hasMore
    val listState = rememberLazyListState()
    LazyListNearEndLoadEffect(
        listState = listState,
        itemCount = users.size,
        hasMore = hasMore,
        isLoadingMore = isLoadingMore,
        onLoadMore = { viewModel.onEvent(UsersUiEvent.LoadMore) },
    )
    val focusManager = LocalFocusManager.current
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .distinctUntilChanged()
            .filter { it }
            .collect { focusManager.clearFocus(force = true) }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TriggerScreenBackground)
            .triggerImeInsetPadding(),
    ) {
        listError?.let { err ->
            Surface(color = Color(0xFF3E2723)) {
                Column(Modifier.padding(10.dp)) {
                    Text(err, color = Color(0xFFFFCCBC), fontSize = 13.sp)
                    TextButton(onClick = { viewModel.onEvent(UsersUiEvent.RetryList) }) {
                        Text(TriggerStrings.Ui.TRY_AGAIN, color = Color.White)
                    }
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 15.dp, top = 6.dp, end = 15.dp)
                .height(40.dp)
                .background(Color.White, RoundedCornerShape(6.dp))
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (search.isEmpty()) {
                        Text(
                            text = TriggerStrings.Ui.SEARCH,
                            color = SearchHint,
                            fontSize = 14.sp,
                        )
                    }
                    BasicTextField(
                        value = search,
                        onValueChange = { viewModel.onEvent(UsersUiEvent.SearchFieldChanged(it)) },
                        singleLine = true,
                        cursorBrush = SolidColor(Color.Black),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = Color.Black,
                            fontSize = 14.sp,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = Color(0xFF3F3F3F),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        when {
            isInitialLoading && users.isEmpty() && listError == null -> {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                ) {
                    items(users, key = { it.id }) { user ->
                        UserRow(user = user, onClick = { onOpenChat(user.id) })
                    }
                    if (isLoadingMore || hasMore) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (isLoadingMore) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(28.dp),
                                        color = Color(0xFF63FFA3),
                                        strokeWidth = 2.dp,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Fires [onLoadMore] once when the last visible item index is within three rows of the end, while [hasMore] is true
 * and a load is not already running.
 *
 * @param listState [LazyListState] of the column to observe.
 * @param itemCount Current row count (user rows only; footer items are excluded from this count by callers).
 * @param hasMore Whether the ViewModel reports additional server pages.
 * @param isLoadingMore Suppresses re-entry while a page request is in flight.
 * @param onLoadMore Typically the ViewModel’s `loadMore*` function.
 * @author udit
 */
@Composable
private fun LazyListNearEndLoadEffect(
    listState: LazyListState,
    itemCount: Int,
    hasMore: Boolean,
    isLoadingMore: Boolean,
    onLoadMore: () -> Unit,
) {
    LaunchedEffect(listState, itemCount, hasMore, isLoadingMore) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: return@snapshotFlow false
            itemCount > 0 && lastVisible >= itemCount - 3
        }
            .distinctUntilChanged()
            .filter { nearEnd -> nearEnd && hasMore && !isLoadingMore }
            .collect { onLoadMore() }
    }
}

/**
 * Single row for a [User] with avatar and display name.
 *
 * @param user User to display.
 * @param onClick Invoked when the row is tapped.
 * @author udit
 */
@Composable
private fun UserRow(
    user: User,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 1.dp),
        colors = CardDefaults.cardColors(containerColor = TriggerScreenBackground),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TriggerProfileAvatar(
                imageUrl = user.imageUrl,
                modifier = Modifier
                    .padding(start = 5.dp)
                    .size(52.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
            Text(
                text = user.username,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp, top = 8.dp, bottom = 8.dp),
            )
        }
    }
}
