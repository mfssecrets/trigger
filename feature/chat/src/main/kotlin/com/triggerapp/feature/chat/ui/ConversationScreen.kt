package com.triggerapp.feature.chat.ui

import android.annotation.SuppressLint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imeNestedScroll
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Report
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.triggerapp.core.strings.TriggerStrings
import com.triggerapp.core.ui.TriggerProfileAvatar
import com.triggerapp.core.ui.theme.TriggerScreenBackground
import com.triggerapp.core.ui.theme.TriggerTheme
import com.triggerapp.core.ui.triggerKeyboardInsetPadding
import com.triggerapp.domain.model.ChatMessage
import com.triggerapp.domain.model.User
import com.triggerapp.feature.chat.presentation.ConversationUiEvent
import com.triggerapp.feature.chat.presentation.ConversationUiState
import com.triggerapp.feature.chat.presentation.ConversationViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val ChatBarBlack = Color.Black

/**
 * Dialog states for header 3-dot overflow options.
 */
private sealed interface ChatActionDialog {
    data object ClearChat : ChatActionDialog
    data object AutoDelete : ChatActionDialog
    data object Report : ChatActionDialog
    data object Block : ChatActionDialog
}

/**
 * Builds the presence subtitle for the chat header.
 */
internal fun lastSeenLabel(lastSeenMs: Long): String? {
    if (lastSeenMs <= 0L) return null
    val calendar = java.util.Calendar.getInstance()
    val nowDay = calendar.get(java.util.Calendar.DAY_OF_YEAR)
    val nowYear = calendar.get(java.util.Calendar.YEAR)
    calendar.timeInMillis = lastSeenMs
    val sameDay = calendar.get(java.util.Calendar.DAY_OF_YEAR) == nowDay &&
        calendar.get(java.util.Calendar.YEAR) == nowYear
    val time = SimpleDateFormat("HH:mm", Locale.getDefault())
        .format(Date(lastSeenMs))
    return if (sameDay) {
        TriggerStrings.Ui.LAST_SEEN_PREFIX + TriggerStrings.Ui.LAST_SEEN_TODAY + time
    } else {
        val day = SimpleDateFormat("d MMM", Locale.getDefault())
            .format(Date(lastSeenMs))
        TriggerStrings.Ui.LAST_SEEN_PREFIX + day + ", " + time
    }
}

/**
 * Incoming message bubble fill.
 */
@SuppressLint("InvalidColorHexValue")
private val BubbleReceiver = Color(0xFFBA535353)
private val BubbleSender = Color(0xFF50DA88)
private val HintCompose = Color(0xFFAAA1A1)

/**
 * Stateless 1:1 conversation UI: peer top bar, message list, composer, and media attachments.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun ConversationScreenContent(
    state: ConversationUiState,
    listState: LazyListState,
    snackBarHostState: SnackbarHostState,
    composerInteraction: MutableInteractionSource,
    onEvent: (ConversationUiEvent) -> Unit,
    onBack: () -> Unit,
    onOpenPeerProfile: () -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val messages = state.messages
    val peer = state.peerUser
    val draft = state.draft
    val myId = state.myUserId
    val streamError = state.streamError

    var showMenu by remember { mutableStateOf(false) }
    var showAttachMenu by remember { mutableStateOf(false) }
    var activeDialog by remember { mutableStateOf<ChatActionDialog?>(null) }
    var isUploadingMedia by remember { mutableStateOf(false) }
    var previewMediaUrl by remember { mutableStateOf<String?>(null) }
    var autoDeleteOption by remember { mutableStateOf("Off") }
    var reportReason by remember { mutableStateOf("Spam") }

    // Real Photo & Video gallery picker via zero-permission Android Photo Picker
    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            isUploadingMedia = true
            coroutineScope.launch {
                val mimeType = context.contentResolver.getType(uri).orEmpty()
                val payload = if (mimeType.startsWith("video", ignoreCase = true)) {
                    ChatMediaHelper.processVideoUri(context, uri)
                } else {
                    ChatMediaHelper.processImageUri(context, uri)
                }
                isUploadingMedia = false
                if (payload != null) {
                    onEvent(ConversationUiEvent.SendDirect(payload))
                } else {
                    snackBarHostState.showSnackbar("Unable to load selected media from gallery")
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = TriggerScreenBackground,
        snackbarHost = { SnackbarHost(snackBarHostState) },
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets.statusBars,
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(end = 10.dp)
                                .clickable(
                                    enabled = peer != null,
                                    onClick = onOpenPeerProfile,
                                ),
                        ) {
                            TriggerProfileAvatar(
                                imageUrl = peer?.imageUrl.orEmpty(),
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop,
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (peer?.isOnline == true) Color(0xFF63FFA3) else Color(0xFFC9CACD),
                                    ),
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = peer?.username ?: TriggerStrings.Ui.CHATS,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            val presenceLabel = when {
                                peer?.isOnline == true -> TriggerStrings.Defaults.PRESENCE_ONLINE
                                else -> lastSeenLabel(peer?.lastSeen ?: 0L)
                            }
                            if (presenceLabel != null) {
                                Text(
                                    text = presenceLabel,
                                    color = Color(0xFFAFACAC),
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
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
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More options",
                                tint = Color.White,
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(Color(0xFF1E242B)),
                        ) {
                            DropdownMenuItem(
                                text = { Text("View user", color = Color.White) },
                                onClick = {
                                    showMenu = false
                                    onOpenPeerProfile()
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Person,
                                        contentDescription = null,
                                        tint = Color.White,
                                    )
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Clear chat", color = Color.White) },
                                onClick = {
                                    showMenu = false
                                    activeDialog = ChatActionDialog.ClearChat
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.DeleteSweep,
                                        contentDescription = null,
                                        tint = Color.White,
                                    )
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Auto delete", color = Color.White) },
                                onClick = {
                                    showMenu = false
                                    activeDialog = ChatActionDialog.AutoDelete
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Timer,
                                        contentDescription = null,
                                        tint = Color.White,
                                    )
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Report", color = Color(0xFFFFB4AB)) },
                                onClick = {
                                    showMenu = false
                                    activeDialog = ChatActionDialog.Report
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Report,
                                        contentDescription = null,
                                        tint = Color(0xFFFFB4AB),
                                    )
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Block", color = Color(0xFFFF5252)) },
                                onClick = {
                                    showMenu = false
                                    activeDialog = ChatActionDialog.Block
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Block,
                                        contentDescription = null,
                                        tint = Color(0xFFFF5252),
                                    )
                                },
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ChatBarBlack,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                ),
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .triggerKeyboardInsetPadding()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            ) {
                if (isUploadingMedia) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                            .background(Color(0xFF1E242B), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color(0xFF63FFA3),
                            strokeWidth = 2.dp,
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Uploading gallery media…",
                            color = Color.White,
                            fontSize = 12.sp,
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black, RoundedCornerShape(27.dp))
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box {
                        IconButton(
                            onClick = { showAttachMenu = true },
                            modifier = Modifier.size(40.dp),
                        ) {
                            Icon(
                                Icons.Default.AttachFile,
                                contentDescription = "Attach media",
                                tint = Color(0xFF90A4AE),
                                modifier = Modifier.size(22.dp),
                            )
                        }
                        DropdownMenu(
                            expanded = showAttachMenu,
                            onDismissRequest = { showAttachMenu = false },
                            modifier = Modifier.background(Color(0xFF1E242B)),
                        ) {
                            DropdownMenuItem(
                                text = { Text("Send Photo", color = Color.White) },
                                onClick = {
                                    showAttachMenu = false
                                    mediaPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Image,
                                        contentDescription = null,
                                        tint = Color(0xFF63FFA3),
                                    )
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Send Video", color = Color.White) },
                                onClick = {
                                    showAttachMenu = false
                                    mediaPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly),
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Videocam,
                                        contentDescription = null,
                                        tint = Color(0xFF64B5F6),
                                    )
                                },
                            )
                        }
                    }
                    TextField(
                        value = draft,
                        onValueChange = { onEvent(ConversationUiEvent.DraftChanged(it)) },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp),
                        interactionSource = composerInteraction,
                        placeholder = {
                            Text(TriggerStrings.Ui.TYPE_MESSAGE, color = HintCompose, fontSize = 16.sp)
                        },
                        maxLines = 4,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = { onEvent(ConversationUiEvent.Send) },
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = Color.White,
                            fontSize = 16.sp,
                        ),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = Color.White,
                        ),
                    )
                    IconButton(
                        onClick = { onEvent(ConversationUiEvent.Send) },
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = TriggerStrings.Ui.SEND,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            streamError?.let { err ->
                Surface(color = Color(0xFF3E2723)) {
                    Column(Modifier.padding(10.dp)) {
                        Text(
                            text = err,
                            color = Color(0xFFFFCCBC),
                            fontSize = 13.sp,
                        )
                        Row(Modifier.padding(top = 4.dp)) {
                            TextButton(
                                onClick = { onEvent(ConversationUiEvent.RetryStreams) },
                            ) {
                                Text(TriggerStrings.Ui.TRY_AGAIN, color = Color.White)
                            }
                            TextButton(onClick = { onEvent(ConversationUiEvent.DismissStreamError) }) {
                                Text(TriggerStrings.Ui.CLOSE, color = Color.White)
                            }
                        }
                    }
                }
            }
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(TriggerScreenBackground)
                    .imeNestedScroll(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
            ) {
                items(messages, key = { it.pushId ?: "${it.timestamp}_${it.senderId}" }) { msg ->
                    MessageBubble(
                        message = msg,
                        myUserId = myId,
                        onPreviewImage = { url -> previewMediaUrl = url },
                    )
                }
            }
        }
    }

    // Fullscreen media preview
    previewMediaUrl?.let { mediaUrl ->
        Dialog(
            onDismissRequest = { previewMediaUrl = null },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.92f)),
                contentAlignment = Alignment.Center,
            ) {
                IconButton(
                    onClick = { previewMediaUrl = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp),
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                    )
                }
                TriggerProfileAvatar(
                    imageUrl = mediaUrl,
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Fit,
                )
            }
        }
    }

    // 3-Dot Dialogs
    when (activeDialog) {
        ChatActionDialog.ClearChat -> {
            AlertDialog(
                onDismissRequest = { activeDialog = null },
                title = { Text("Clear chat", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        "Are you sure you want to clear messages in this chat?",
                        color = Color(0xFFC9CACD),
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        activeDialog = null
                        coroutineScope.launch {
                            snackBarHostState.showSnackbar("Chat messages cleared")
                        }
                    }) {
                        Text("Clear", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { activeDialog = null }) {
                        Text("Cancel", color = Color.White)
                    }
                },
                containerColor = Color(0xFF1E242B),
            )
        }
        ChatActionDialog.AutoDelete -> {
            val options = listOf("Off", "24 Hours", "7 Days", "30 Days")
            AlertDialog(
                onDismissRequest = { activeDialog = null },
                title = { Text("Auto delete messages", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        options.forEach { opt ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { autoDeleteOption = opt }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(
                                    selected = autoDeleteOption == opt,
                                    onClick = { autoDeleteOption = opt },
                                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF50DA88)),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(opt, color = Color.White, fontSize = 15.sp)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        activeDialog = null
                        coroutineScope.launch {
                            snackBarHostState.showSnackbar("Auto delete timer set to $autoDeleteOption")
                        }
                    }) {
                        Text("Save", color = Color(0xFF50DA88), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { activeDialog = null }) {
                        Text("Cancel", color = Color.White)
                    }
                },
                containerColor = Color(0xFF1E242B),
            )
        }
        ChatActionDialog.Report -> {
            val reasons = listOf("Spam", "Harassment", "Inappropriate content", "Scam or fraud")
            AlertDialog(
                onDismissRequest = { activeDialog = null },
                title = { Text("Report user", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        reasons.forEach { r ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { reportReason = r }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(
                                    selected = reportReason == r,
                                    onClick = { reportReason = r },
                                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFFFB4AB)),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(r, color = Color.White, fontSize = 15.sp)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        activeDialog = null
                        coroutineScope.launch {
                            snackBarHostState.showSnackbar("Report submitted. Thank you for your feedback.")
                        }
                    }) {
                        Text("Report", color = Color(0xFFFFB4AB), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { activeDialog = null }) {
                        Text("Cancel", color = Color.White)
                    }
                },
                containerColor = Color(0xFF1E242B),
            )
        }
        ChatActionDialog.Block -> {
            AlertDialog(
                onDismissRequest = { activeDialog = null },
                title = {
                    Text("Block ${peer?.username ?: "user"}?", color = Color.White, fontWeight = FontWeight.Bold)
                },
                text = {
                    Text(
                        "Blocked contacts will no longer be able to send you messages or view your online status.",
                        color = Color(0xFFC9CACD),
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        activeDialog = null
                        coroutineScope.launch {
                            snackBarHostState.showSnackbar("${peer?.username ?: "User"} has been blocked")
                        }
                    }) {
                        Text("Block", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { activeDialog = null }) {
                        Text("Cancel", color = Color.White)
                    }
                },
                containerColor = Color(0xFF1E242B),
            )
        }
        null -> Unit
    }
}

/**
 * Conversation route: [ConversationViewModel] (MVI), scroll/snackbar side effects, and [ConversationScreenContent].
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ConversationRoute(
    onBack: () -> Unit,
    onOpenPeerProfile: (String) -> Unit,
    viewModel: ConversationViewModel = koinViewModel(),
) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    val messages = s.messages
    val sendMessageError = s.sendError
    val snackBarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val imeBottomPx = WindowInsets.ime.getBottom(density)
    val composerInteraction = remember { MutableInteractionSource() }
    val composerFocused by composerInteraction.collectIsFocusedAsState()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onEvent(ConversationUiEvent.MarkPeerSeen)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(s.myUserId, viewModel.peerId) {
        if (s.myUserId != null) {
            viewModel.onEvent(ConversationUiEvent.MarkPeerSeen)
        }
    }

    val lastMessageKey = messages.lastOrNull()?.let { it.pushId ?: "${it.timestamp}_${it.senderId}" }
    LaunchedEffect(messages.size, lastMessageKey) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }
    LaunchedEffect(imeBottomPx) {
        if (imeBottomPx > 0 && messages.isNotEmpty()) {
            delay(120)
            listState.animateScrollToItem(messages.lastIndex)
        }
    }
    LaunchedEffect(composerFocused) {
        if (composerFocused && messages.isNotEmpty()) {
            delay(150)
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    LaunchedEffect(sendMessageError) {
        val msg = sendMessageError ?: return@LaunchedEffect
        snackBarHostState.showSnackbar(
            message = msg,
            duration = SnackbarDuration.Long,
        )
        viewModel.onEvent(ConversationUiEvent.ClearSendError)
    }

    ConversationScreenContent(
        state = s,
        listState = listState,
        snackBarHostState = snackBarHostState,
        composerInteraction = composerInteraction,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        onOpenPeerProfile = { onOpenPeerProfile(viewModel.peerId) },
    )
}

/**
 * Message content component supporting text, photo, and video preview.
 */
@Composable
private fun MessageContent(
    text: String,
    mine: Boolean,
    onPreviewImage: (String) -> Unit,
) {
    when {
        text.startsWith("[image]:") || text.startsWith("data:image/") -> {
            val imgUri = if (text.startsWith("[image]:")) text.removePrefix("[image]:") else text
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onPreviewImage(imgUri) },
            ) {
                TriggerProfileAvatar(
                    imageUrl = imgUri,
                    modifier = Modifier
                        .widthIn(max = 240.dp)
                        .heightIn(max = 240.dp),
                    contentScale = ContentScale.Crop,
                )
            }
        }
        text.startsWith("[video]:") -> {
            val payload = text.removePrefix("[video]:")
            val parts = payload.split("|")
            val thumbUri = parts.getOrNull(0).orEmpty()
            val duration = parts.getOrNull(1) ?: "Video"
            Box(
                modifier = Modifier
                    .widthIn(min = 180.dp, max = 240.dp)
                    .heightIn(min = 120.dp, max = 180.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center,
            ) {
                if (thumbUri.isNotEmpty()) {
                    TriggerProfileAvatar(
                        imageUrl = thumbUri,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play video",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp),
                    )
                }
                Surface(
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp),
                ) {
                    Text(
                        text = duration,
                        color = Color.White,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
        }
        else -> {
            val senderShape = RoundedCornerShape(
                topStart = 30.dp,
                topEnd = 0.dp,
                bottomEnd = 30.dp,
                bottomStart = 30.dp,
            )
            val receiverShape = RoundedCornerShape(
                topStart = 0.dp,
                topEnd = 30.dp,
                bottomEnd = 30.dp,
                bottomStart = 30.dp,
            )
            Text(
                text = text,
                color = Color.White,
                fontSize = 17.sp,
                modifier = Modifier
                    .background(if (mine) BubbleSender else BubbleReceiver, if (mine) senderShape else receiverShape)
                    .padding(14.dp),
            )
        }
    }
}

/**
 * Renders a single [ChatMessage] bubble aligned by sender with NO read ticks.
 */
@Composable
private fun MessageBubble(
    message: ChatMessage,
    myUserId: String?,
    onPreviewImage: (String) -> Unit,
) {
    val mine = message.senderId == myUserId
    val time = formatChatTime(message.timestamp)

    if (mine) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.End,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
            ) {
                Text(
                    text = time,
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(end = 7.dp),
                )
                MessageContent(
                    text = message.message,
                    mine = true,
                    onPreviewImage = onPreviewImage,
                )
            }
            // Removed seen status / read ticks as requested
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
        ) {
            MessageContent(
                text = message.message,
                mine = false,
                onPreviewImage = onPreviewImage,
            )
            Text(
                text = time,
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 7.dp),
            )
        }
    }
}

/**
 * Formats a millisecond string into short local time (for example "3:45 PM").
 */
private fun formatChatTime(timestamp: String): String {
    val ms = timestamp.toLongOrNull() ?: return ""
    return SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(ms))
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Preview(showBackground = true, showSystemUi = false)
@Composable
private fun ConversationScreenPreview() {
    val peer = User(
        id = "peer",
        username = "Alex",
        emailId = "",
        timestamp = "0",
        imageUrl = "",
        bio = "",
        status = TriggerStrings.Defaults.PRESENCE_ONLINE,
        searchKey = "alex",
    )
    val now = System.currentTimeMillis().toString()
    val messages = listOf(
        ChatMessage("1", "peer", "me", "Hey!", now, true),
        ChatMessage("2", "me", "peer", "Hi there — preview message.", now, false),
    )
    val state = ConversationUiState(
        myUserId = "me",
        messages = messages,
        peerUser = peer,
        draft = "Type here…",
        streamError = null,
        sendError = null,
    )
    TriggerTheme(darkTheme = true, dynamicColor = false, useBrandDarkColors = true) {
        ConversationScreenContent(
            state = state,
            listState = rememberLazyListState(),
            snackBarHostState = remember { SnackbarHostState() },
            composerInteraction = remember { MutableInteractionSource() },
            onEvent = {},
            onBack = {},
            onOpenPeerProfile = {},
        )
    }
}
