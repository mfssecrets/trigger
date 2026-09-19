package com.triggerapp.feature.chat.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.triggerapp.core.strings.TriggerStrings
import com.triggerapp.core.ui.AnimatedProfilePhotoViewerOverlay
import com.triggerapp.core.ui.TriggerProfileAvatar
import com.triggerapp.core.ui.TriggerProfileDetailRow
import com.triggerapp.core.ui.TriggerProfileGroupedDivider
import com.triggerapp.core.ui.TriggerProfileGroupedList
import com.triggerapp.core.ui.TriggerProfileMuted
import com.triggerapp.core.ui.theme.TriggerAccent
import com.triggerapp.core.ui.theme.TriggerPurple
import com.triggerapp.core.ui.theme.TriggerScreenBackground
import com.triggerapp.core.ui.theme.TriggerTheme
import com.triggerapp.domain.model.User
import com.triggerapp.domain.text.DisplayTextLimits
import com.triggerapp.feature.chat.presentation.PeerProfileUiEvent
import com.triggerapp.feature.chat.presentation.PeerProfileUiState
import com.triggerapp.feature.chat.presentation.PeerProfileViewModel
import org.koin.androidx.compose.koinViewModel
import kotlin.math.abs

/**
 * Peer profile screen UI with Instagram modern following, followers section,
 * dynamic follow/following/followback/requested actions, and direct message button.
 *
 * @param ui MVI [PeerProfileUiState].
 * @param onBack Navigate up.
 * @param onOpenChat Open direct message chat with this peer.
 * @param onRetry Retry loading the profile ([PeerProfileUiEvent.Retry]).
 * @author triggerapp
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PeerProfileScreenContent(
    ui: PeerProfileUiState,
    onBack: () -> Unit,
    onOpenChat: (peerId: String) -> Unit = {},
    onRetry: () -> Unit,
    onToggleFollow: () -> Unit = {},
) {
    val user = ui.user
    val loadError = ui.loadError
    var showPhotoViewer by remember { mutableStateOf(false) }
    var showOptionsSheet by remember { mutableStateOf(false) }
    var showFollowersSheet by remember { mutableStateOf(false) }
    var showFollowingSheet by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TriggerScreenBackground),
    ) {
        Column(Modifier.fillMaxSize()) {
            TopAppBar(
                title = {
                    Text(
                        text = if (user != null) "@${user.username.lowercase().replace(" ", "_")}" else TriggerStrings.Ui.PROFILE,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = TriggerStrings.Ui.BACK,
                            tint = Color.White,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showOptionsSheet = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = Color.White,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                ),
            )

            when {
                user != null -> {
                    val u = user
                    val follow = ui.follow
                    val isFollowing = follow.isFollowing
                    var showFollowDropdown by remember { mutableStateOf(false) }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Spacer(Modifier.height(12.dp))

                        // Avatar & Verification
                        Box(
                            modifier = Modifier
                                .size(104.dp)
                                .clip(CircleShape)
                                .clickable { showPhotoViewer = true },
                        ) {
                            TriggerProfileAvatar(
                                imageUrl = u.imageUrl,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop,
                            )
                        }

                        Spacer(Modifier.height(10.dp))

                        // Username & Badge (badge shown only for face-verified users)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(horizontal = 24.dp),
                        ) {
                            Text(
                                text = u.username,
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (u.isFaceVerified) {
                                Spacer(Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Filled.Verified,
                                    contentDescription = "Verified",
                                    tint = TriggerAccent,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }

                        Text(
                            text = "@${u.username.lowercase().replace(" ", "_")}",
                            color = TriggerAccent,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 2.dp),
                        )

                        if (u.bio.isNotBlank()) {
                            Text(
                                text = u.bio,
                                color = Color(0xFFCFD8DC),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 36.dp, vertical = 6.dp),
                            )
                        }

                        Spacer(Modifier.height(14.dp))

                        // ==========================================
                        // INSTAGRAM MODERN STATS SECTION
                        // ==========================================
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 28.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            PeerStatColumn(
                                count = "${follow.followersCount}",
                                label = "Followers",
                                onClick = { showFollowersSheet = true },
                            )
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(26.dp)
                                    .background(Color(0xFF2A3441)),
                            )
                            PeerStatColumn(
                                count = "${follow.followingCount}",
                                label = "Following",
                                onClick = { showFollowingSheet = true },
                            )
                        }

                        Spacer(Modifier.height(18.dp))

                        // ==========================================================
                        // ACTION BUTTONS: FOLLOW / FOLLOWING / FOLLOWBACK / REQUESTED + MESSAGE
                        // ==========================================================
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // Follow / Following / Follow Back button (live RTDB state)
                            Box(modifier = Modifier.weight(1f)) {
                                val isPrimary = !isFollowing
                                val buttonBg by animateColorAsState(
                                    targetValue = if (isPrimary) TriggerAccent else Color(0xFF242C35),
                                    animationSpec = tween(250),
                                    label = "buttonBg",
                                )
                                val textColor = if (isPrimary) Color.Black else Color.White

                                Surface(
                                    onClick = {
                                        if (isFollowing) {
                                            showFollowDropdown = true
                                        } else {
                                            onToggleFollow()
                                        }
                                    },
                                    enabled = !ui.followBusy,
                                    shape = RoundedCornerShape(8.dp),
                                    color = buttonBg,
                                    border = if (isPrimary) null else BorderStroke(1.dp, Color(0xFF384351)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(38.dp),
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        if (ui.followBusy) {
                                            CircularProgressIndicator(
                                                color = textColor,
                                                strokeWidth = 2.dp,
                                                modifier = Modifier.size(16.dp),
                                            )
                                        } else {
                                            Text(
                                                text = follow.label,
                                                color = textColor,
                                                fontSize = 14.sp,
                                                fontWeight = if (isPrimary) FontWeight.Bold else FontWeight.SemiBold,
                                            )
                                            if (isFollowing) {
                                                Spacer(Modifier.width(4.dp))
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = textColor,
                                                    modifier = Modifier.size(15.dp),
                                                )
                                                Icon(
                                                    imageVector = Icons.Default.KeyboardArrowDown,
                                                    contentDescription = null,
                                                    tint = textColor,
                                                    modifier = Modifier.size(16.dp),
                                                )
                                            }
                                        }
                                    }
                                }

                                // Unfollow dropdown (only while following)
                                DropdownMenu(
                                    expanded = showFollowDropdown,
                                    onDismissRequest = { showFollowDropdown = false },
                                    modifier = Modifier.background(Color(0xFF222831)),
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Unfollow", color = Color(0xFFFF5252)) },
                                        onClick = {
                                            showFollowDropdown = false
                                            onToggleFollow()
                                        },
                                    )
                                }
                            }

                            // Message Button (Direct Chat Action)
                            Surface(
                                onClick = { onOpenChat(u.id) },
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF242C35),
                                border = BorderStroke(1.dp, Color(0xFF384351)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ChatBubbleOutline,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "Message",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(20.dp))

                        // Grouped Info List
                        TriggerProfileGroupedList(Modifier.padding(horizontal = 16.dp)) {
                            TriggerProfileDetailRow(
                                label = TriggerStrings.Ui.NAME,
                                value = u.username,
                                valueMaxLines = 2,
                            )
                            TriggerProfileGroupedDivider()
                            TriggerProfileDetailRow(
                                label = TriggerStrings.Ui.ABOUT,
                                value = u.bio.ifBlank { TriggerStrings.Defaults.NEW_USER_BIO },
                                valueMaxLines = DisplayTextLimits.MAX_BIO_LINES,
                            )
                        }

                    }
                }
                loadError != null -> {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = loadError,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Spacer(Modifier.height(16.dp))
                        FilledTonalButton(onClick = onRetry) {
                            Text(TriggerStrings.Ui.TRY_AGAIN)
                        }
                    }
                }
                else -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = Color.White)
                    }
                }
            }
        }

        // Fullscreen Profile Photo Overlay
        AnimatedProfilePhotoViewerOverlay(
            visible = showPhotoViewer && user != null,
            imageUrl = user?.imageUrl.orEmpty(),
            title = user?.username.orEmpty(),
            onDismiss = { showPhotoViewer = false },
        )

        // Options bottom sheet
        if (showOptionsSheet) {
            ModalBottomSheet(
                onDismissRequest = { showOptionsSheet = false },
                containerColor = Color(0xFF1E242B),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text("Profile Options", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showOptionsSheet = false }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.Share, contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(12.dp))
                        Text("Share Profile Link", color = Color.White, fontSize = 15.sp)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showOptionsSheet = false }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("🚫 Block User", color = Color(0xFFFF5252), fontSize = 15.sp)
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }

        // Followers Bottom Sheet (real user list resolved from Followers/{uid})
        if (showFollowersSheet) {
            PeerConnectionsSheet(
                title = "Followers",
                users = ui.followersUsers,
                onDismiss = { showFollowersSheet = false },
            )
        }

        // Following Bottom Sheet (real user list resolved from Following/{uid})
        if (showFollowingSheet) {
            PeerConnectionsSheet(
                title = "Following",
                users = ui.followingUsers,
                onDismiss = { showFollowingSheet = false },
            )
        }
    }
}

@Composable
private fun PeerStatColumn(
    count: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = count,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            color = TriggerProfileMuted,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PeerConnectionsSheet(
    title: String,
    users: List<User>,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1C2229),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            if (users.isEmpty()) {
                Text(
                    text = "Nobody here yet.",
                    color = Color(0xFF90A4AE),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            } else {
                users.forEach { person ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TriggerProfileAvatar(
                            imageUrl = person.imageUrl,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape),
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    person.effectiveDisplayName,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                )
                                if (person.isFaceVerified) {
                                    Spacer(Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Filled.Verified,
                                        contentDescription = "Verified",
                                        tint = TriggerAccent,
                                        modifier = Modifier.size(14.dp),
                                    )
                                }
                            }
                            Text(
                                "@${person.username}",
                                color = Color(0xFF90A4AE),
                                fontSize = 12.sp,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/**
 * Peer profile route: [PeerProfileViewModel] and [PeerProfileScreenContent].
 *
 * @param onBack Navigate up.
 * @param onOpenChat Navigate to direct chat with peer.
 * @param viewModel MVI [PeerProfileViewModel].
 * @author triggerapp
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeerProfileRoute(
    onBack: () -> Unit,
    onOpenChat: (peerId: String) -> Unit = {},
    viewModel: PeerProfileViewModel = koinViewModel(),
) {
    val ui by viewModel.state.collectAsStateWithLifecycle()
    PeerProfileScreenContent(
        ui = ui,
        onBack = onBack,
        onOpenChat = onOpenChat,
        onRetry = { viewModel.onEvent(PeerProfileUiEvent.Retry) },
        onToggleFollow = { viewModel.onEvent(PeerProfileUiEvent.ToggleFollow) },
    )
}

/**
 * Compose preview for [PeerProfileScreenContent] with a loaded sample [User].
 * @author triggerapp
 */
@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, showSystemUi = false)
@Composable
private fun PeerProfileScreenPreview() {
    val sample = User(
        id = "1",
        username = "Alex River",
        emailId = "",
        timestamp = "0",
        imageUrl = "",
        bio = "Building Trigger · coffee · photos 🚀",
        status = "",
        searchKey = "alex",
    )
    TriggerTheme(darkTheme = true, dynamicColor = false, useBrandDarkColors = true) {
        PeerProfileScreenContent(
            ui = PeerProfileUiState(user = sample),
            onBack = {},
            onOpenChat = {},
            onRetry = {},
        )
    }
}
