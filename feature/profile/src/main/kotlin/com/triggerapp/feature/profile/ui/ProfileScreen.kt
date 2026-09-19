package com.triggerapp.feature.profile.ui

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.triggerapp.core.strings.TriggerStrings
import com.triggerapp.core.ui.AnimatedProfilePhotoViewerOverlay
import com.triggerapp.core.ui.TriggerProfileAvatar
import com.triggerapp.core.ui.TriggerProfileDetailRow
import com.triggerapp.core.ui.TriggerProfileGroupedDivider
import com.triggerapp.core.ui.TriggerProfileGroupedList
import com.triggerapp.core.ui.TriggerProfileMuted
import com.triggerapp.core.ui.theme.TriggerScreenBackground
import com.triggerapp.core.ui.theme.TriggerTheme
import com.triggerapp.core.ui.triggerKeyboardInsetPadding
import com.triggerapp.domain.model.User
import com.triggerapp.domain.text.DisplayTextLimits
import com.triggerapp.feature.profile.crop.ProfilePhotoCropContract
import com.triggerapp.feature.profile.crop.createProfileCameraImageUri
import com.triggerapp.feature.profile.crop.jpegBytesForProfileUpload
import com.triggerapp.feature.profile.crop.profilePhotoCropOptions
import com.triggerapp.feature.profile.presentation.ProfileDialog
import com.triggerapp.feature.profile.presentation.ProfileUiEvent
import com.triggerapp.feature.profile.presentation.ProfileViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

private val LogOutAccent = Color(0xFFFF5252)

/**
 * Profile tab: photo, display name, hints, grouped fields, sign-out, and fullscreen viewer.
 *
 * @param onSignOut Invoked when the user taps Log out (typically shell sign-out + navigation).
 * @param viewModel Supplies ProfileUiState and handles [ProfileUiEvent].
 * @author udit
 */
@Composable
fun ProfileRoute(
    onSignOut: () -> Unit = {},
    viewModel: ProfileViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingCaptureUri by remember { mutableStateOf<Uri?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    var showSignOutConfirmation by remember { mutableStateOf(false) }
    var showUsernameEditDialog by remember { mutableStateOf(false) }
    var tempUsernameEdit by remember { mutableStateOf("") }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.onEvent(ProfileUiEvent.ClosePhotoViewer)
            viewModel.onEvent(ProfileUiEvent.ClosePhotoSourceSheet)
            viewModel.onEvent(ProfileUiEvent.DismissDialog)
        }
    }

    val cropProfilePhoto = rememberLauncherForActivityResult(ProfilePhotoCropContract()) { result ->
        if (!result.isSuccessful) return@rememberLauncherForActivityResult
        val uri = result.uriContent ?: return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            val raw = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return@launch
            val bytes = jpegBytesForProfileUpload(raw)
            viewModel.onEvent(ProfileUiEvent.PhotoPicked(bytes))
        }
    }

    val pickGallery = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            cropProfilePhoto.launch(profilePhotoCropOptions(uri))
        }
    }

    val takePicture = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { ok ->
        val u = pendingCaptureUri
        pendingCaptureUri = null
        if (ok && u != null) {
            cropProfilePhoto.launch(profilePhotoCropOptions(u))
        }
    }

    val requestCameraPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val u = pendingCaptureUri
        if (granted && u != null) {
            takePicture.launch(u)
        } else {
            pendingCaptureUri = null
            if (!granted) {
                scope.launch {
                    snackbarHostState.showSnackbar(TriggerStrings.Errors.CAMERA_PERMISSION_REQUIRED)
                }
            }
        }
    }

    when (state.dialog) {
        ProfileDialog.Username -> EditDialog(
            title = TriggerStrings.Ui.EDIT_USERNAME,
            text = state.dialogText,
            errorText = state.error,
            onTextChange = { viewModel.onEvent(ProfileUiEvent.DialogTextChanged(it)) },
            onDismiss = { viewModel.onEvent(ProfileUiEvent.DismissDialog) },
            onSave = { viewModel.onEvent(ProfileUiEvent.SaveDialog) },
            loading = state.loading,
        )
        ProfileDialog.Bio -> EditDialog(
            title = TriggerStrings.Ui.EDIT_BIO,
            text = state.dialogText,
            errorText = state.error,
            onTextChange = { viewModel.onEvent(ProfileUiEvent.DialogTextChanged(it)) },
            onDismiss = { viewModel.onEvent(ProfileUiEvent.DismissDialog) },
            onSave = { viewModel.onEvent(ProfileUiEvent.SaveDialog) },
            loading = state.loading,
        )
        ProfileDialog.None -> Unit
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TriggerScreenBackground),
    ) {
    Column(Modifier.fillMaxSize()) {
        val user = state.user
        if (user == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                when (val loadErr = state.profileLoadError) {
                    null ->
                        if (state.initialProfilePending) {
                            CircularProgressIndicator(color = Color.White)
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(24.dp),
                            ) {
                                Text(
                                    text = TriggerStrings.Ui.PROFILE_NOT_AVAILABLE,
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                                Spacer(Modifier.height(16.dp))
                                FilledTonalButton(
                                    onClick = { viewModel.onEvent(ProfileUiEvent.RetryLoadProfile) },
                                ) {
                                    Text(TriggerStrings.Ui.TRY_AGAIN)
                                }
                            }
                        }
                    else -> Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp),
                    ) {
                        Text(
                            text = loadErr,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Spacer(Modifier.height(16.dp))
                        FilledTonalButton(
                            onClick = { viewModel.onEvent(ProfileUiEvent.RetryLoadProfile) },
                        ) {
                            Text(TriggerStrings.Ui.TRY_AGAIN)
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier.padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(modifier = Modifier.size(168.dp)) {
                        TriggerProfileAvatar(
                            imageUrl = user.imageUrl,
                            modifier = Modifier
                                .matchParentSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop,
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clip(CircleShape)
                                .clickable { viewModel.onEvent(ProfileUiEvent.OpenPhotoViewer) },
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(4.dp)
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.Black)
                                .clickable { viewModel.onEvent(ProfileUiEvent.OpenPhotoSourceSheet) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = TriggerStrings.Ui.CHANGE_PHOTO,
                                tint = Color.White,
                                modifier = Modifier
                                    .size(20.dp)
                                    .padding(2.dp),
                            )
                        }
                    }
                }
                if (state.photoUploading) {
                    CircularProgressIndicator(
                        Modifier.padding(8.dp),
                        color = Color.White,
                    )
                }
                val emailPrefix = user.emailId.substringBefore('@').ifBlank {
                    user.username.lowercase().replace(" ", "_")
                }
                val prefs = remember(context) {
                    context.getSharedPreferences("user_profile_prefs", android.content.Context.MODE_PRIVATE)
                }
                var customUsername by rememberSaveable(user.id) {
                    mutableStateOf(prefs.getString("custom_username_${user.id}", null) ?: "@$emailPrefix")
                }

                Text(
                    text = user.username,
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                )
                Text(
                    text = if (customUsername.startsWith("@")) customUsername else "@$customUsername",
                    color = Color(0xFF63FFA3),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 2.dp),
                )
                Text(
                    text  = TriggerStrings.Ui.PROFILE_PHOTO_HINT,
                    color = TriggerProfileMuted,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 40.dp, vertical = 6.dp),
                )
                Spacer(Modifier.height(18.dp))
                TriggerProfileGroupedList(Modifier.padding(horizontal = 16.dp)) {
                    TriggerProfileDetailRow(
                        label = "Name",
                        value = user.username,
                        onClick = { viewModel.onEvent(ProfileUiEvent.OpenUsernameDialog) },
                        showChevron = true,
                        valueMaxLines = 2,
                    )
                    TriggerProfileGroupedDivider()
                    TriggerProfileDetailRow(
                        label = "Username",
                        value = if (customUsername.startsWith("@")) customUsername else "@$customUsername",
                        onClick = {
                            tempUsernameEdit = customUsername.removePrefix("@")
                            showUsernameEditDialog = true
                        },
                        showChevron = true,
                        valueMaxLines = 1,
                    )
                    TriggerProfileGroupedDivider()
                    TriggerProfileDetailRow(
                        label = TriggerStrings.Ui.ABOUT,
                        value = user.bio.ifBlank { TriggerStrings.Defaults.NEW_USER_BIO },
                        onClick = { viewModel.onEvent(ProfileUiEvent.OpenBioDialog) },
                        showChevron = true,
                        valueMaxLines = DisplayTextLimits.MAX_BIO_LINES,
                    )
                    if (user.emailId.isNotBlank()) {
                        TriggerProfileGroupedDivider()
                        TriggerProfileDetailRow(
                            label = TriggerStrings.Ui.EMAIL,
                            value = user.emailId,
                            valueMaxLines = 2,
                        )
                    }
                }
                state.error?.let { err ->
                    Text(
                        err,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(16.dp),
                    )
                }
                Spacer(Modifier.height(28.dp))
                TextButton(
                    onClick = { showSignOutConfirmation = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        tint = LogOutAccent,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = TriggerStrings.Ui.LOG_OUT,
                        color = LogOutAccent,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
    val profileUser = state.user
    AnimatedProfilePhotoViewerOverlay(
        visible = state.photoViewerVisible && profileUser != null,
        imageUrl = profileUser?.imageUrl.orEmpty(),
        title = profileUser?.username.orEmpty(),
        onDismiss = { viewModel.onEvent(ProfileUiEvent.ClosePhotoViewer) },
        onEditPhoto = { viewModel.onEvent(ProfileUiEvent.EditPhotoFromViewer) },
    )

    ProfilePhotoSourceBottomSheet(
        visible = state.photoSourceSheetVisible,
        onDismiss = { viewModel.onEvent(ProfileUiEvent.ClosePhotoSourceSheet) },
        onChooseGallery = {
            pickGallery.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
            )
        },
        onTakePhoto = {
            val uri = createProfileCameraImageUri(context)
            pendingCaptureUri = uri
            when {
                ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED -> takePicture.launch(uri)
                else -> requestCameraPermission.launch(Manifest.permission.CAMERA)
            }
        },
    )

    if (showSignOutConfirmation) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirmation = false },
            title = {
                Text(
                    text = "Sign out",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to sign out of your account?",
                    color = Color(0xFFC7CBD1),
                    fontSize = 15.sp,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSignOutConfirmation = false
                        onSignOut()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LogOutAccent,
                        contentColor = Color.White,
                    ),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text("Sign out", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showSignOutConfirmation = false },
                ) {
                    Text("Cancel", color = Color.White)
                }
            },
            containerColor = Color(0xFF1E242B),
            shape = RoundedCornerShape(16.dp),
        )
    }

    if (showUsernameEditDialog && state.user != null) {
        val currentUser = state.user!!
        val prefs = remember(context) {
            context.getSharedPreferences("user_profile_prefs", android.content.Context.MODE_PRIVATE)
        }
        AlertDialog(
            onDismissRequest = { showUsernameEditDialog = false },
            title = {
                Text("Edit Username", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        "Choose a unique username handle for your profile.",
                        color = Color(0xFFAFACAC),
                        fontSize = 13.sp,
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = tempUsernameEdit,
                        onValueChange = {
                            tempUsernameEdit = it.filter { ch -> ch.isLetterOrDigit() || ch == '_' }
                        },
                        label = { Text("Username") },
                        prefix = { Text("@", color = Color(0xFF63FFA3)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF63FFA3),
                            unfocusedBorderColor = Color(0xFF42474E),
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clean = tempUsernameEdit.trim().removePrefix("@")
                        if (clean.isNotBlank()) {
                            prefs.edit().putString("custom_username_${currentUser.id}", "@$clean").apply()
                        }
                        showUsernameEditDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF63FFA3),
                        contentColor = Color.Black,
                    ),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUsernameEditDialog = false }) {
                    Text("Cancel", color = Color.White)
                }
            },
            containerColor = Color(0xFF1E242B),
            shape = RoundedCornerShape(16.dp),
        )
    }

    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(16.dp),
    )
    }
}

/**
 * Material [AlertDialog] for editing username or bio with validation hints.
 *
 * @param title Dialog title (used to infer username vs bio behavior).
 * @param text Current field text.
 * @param errorText Optional error under the field.
 * @param onTextChange Called when the user edits text.
 * @param onDismiss Closes without saving.
 * @param onSave Persists via the ViewModel.
 * @param loading Disables confirm while a save is running.
 * @author udit
 */
@Composable
private fun EditDialog(
    title: String,
    text: String,
    errorText: String?,
    onTextChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    loading: Boolean,
) {
    val dialogScroll = rememberScrollState()
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.triggerKeyboardInsetPadding(),
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(dialogScroll),
            ) {
                val isBio = title == TriggerStrings.Ui.EDIT_BIO
                val isUsername = title == TriggerStrings.Ui.EDIT_USERNAME
                OutlinedTextField(
                    value = text,
                    onValueChange = onTextChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = !isBio,
                    minLines = if (isBio) 3 else 1,
                    maxLines = if (isBio) DisplayTextLimits.MAX_BIO_LINES else 1,
                    isError = errorText != null,
                    supportingText =
                        if (isBio || isUsername) {
                            {
                                val maxChars =
                                    if (isBio) DisplayTextLimits.MAX_BIO_CHARS
                                    else DisplayTextLimits.MAX_USERNAME_CHARS
                                Text(
                                    text = "${text.length}/$maxChars",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        } else {
                            null
                        },
                )
                errorText?.let { err ->
                    Text(
                        text = err,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onSave, enabled = !loading) {
                Text(TriggerStrings.Ui.SAVE)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(TriggerStrings.Ui.BACK)
            }
        },
    )
}

/**
 * Compose preview for [EditDialog] in username mode.
 * @author udit
 */
@Preview(showBackground = true, showSystemUi = false, name = "Profile · edit username dialog")
@Composable
private fun EditDialogUsernamePreview() {
    TriggerTheme(darkTheme = true, dynamicColor = false, useBrandDarkColors = true) {
        EditDialog(
            title = TriggerStrings.Ui.EDIT_USERNAME,
            text = "Alex",
            errorText = null,
            onTextChange = {},
            onDismiss = {},
            onSave = {},
            loading = false,
        )
    }
}

/**
 * Compose preview for a loaded profile layout (avatar, name, bio) without [ProfileRoute] wiring.
 * @author udit
 */
@Preview(showBackground = true, showSystemUi = false, name = "Profile · loaded (preview)")
@Composable
private fun ProfileLoadedPreview() {
    val user = User(
        id = "1",
        username = "Alex",
        emailId = "",
        timestamp = "0",
        imageUrl = "",
        bio = "Preview bio text for the profile tab.",
        status = "",
        searchKey = "alex",
    )
    TriggerTheme(darkTheme = true, dynamicColor = false, useBrandDarkColors = true) {
        Box(
            Modifier
                .fillMaxSize()
                .background(TriggerScreenBackground)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                TriggerProfileAvatar(
                    imageUrl = user.imageUrl,
                    modifier = Modifier
                        .size(168.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    user.username,
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                TriggerProfileGroupedList(Modifier.fillMaxWidth()) {
                    TriggerProfileDetailRow(
                        label = TriggerStrings.Ui.NAME,
                        value = user.username,
                        valueMaxLines = 2,
                    )
                    TriggerProfileGroupedDivider()
                    TriggerProfileDetailRow(
                        label = TriggerStrings.Ui.ABOUT,
                        value = user.bio,
                        valueMaxLines = DisplayTextLimits.MAX_BIO_LINES,
                    )
                }
            }
        }
    }
}
