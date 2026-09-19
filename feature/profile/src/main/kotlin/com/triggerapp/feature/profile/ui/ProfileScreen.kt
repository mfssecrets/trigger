package com.triggerapp.feature.profile.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import com.triggerapp.domain.model.User
import com.triggerapp.domain.text.DisplayTextLimits
import com.triggerapp.feature.profile.crop.ProfilePhotoCropContract
import com.triggerapp.feature.profile.crop.createProfileCameraImageUri
import com.triggerapp.feature.profile.crop.jpegBytesForProfileUpload
import com.triggerapp.feature.profile.crop.profilePhotoCropOptions
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
    onOpenEditProfile: () -> Unit = {},
    onOpenVerification: () -> Unit = {},
    viewModel: ProfileViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingCaptureUri by remember { mutableStateOf<Uri?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    var showSignOutConfirmation by remember { mutableStateOf(false) }
    val userState = state.user

    DisposableEffect(Unit) {
        onDispose {
            viewModel.onEvent(ProfileUiEvent.ClosePhotoViewer)
            viewModel.onEvent(ProfileUiEvent.ClosePhotoSourceSheet)
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
                val profileHandle = "@" + user.username.lowercase()

                Text(
                    text = user.effectiveDisplayName,
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
                if (user.isFaceVerified) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Verified,
                            contentDescription = TriggerStrings.Ui.VERIFIED_BADGE,
                            tint = Color(0xFF63FFA3),
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = TriggerStrings.Ui.VERIFIED_BADGE,
                            color = Color(0xFF63FFA3),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                Text(
                    text = profileHandle,
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

                Spacer(Modifier.height(6.dp))

                // Profile action buttons (Edit Profile & Share Profile)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Edit Profile Button
                    Surface(
                        onClick = onOpenEditProfile,
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
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(15.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Edit profile",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }

                    // Share Profile Button
                    Surface(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "Add me on Trigger — my handle is $profileHandle",
                                )
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share profile via"))
                        },
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
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(15.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Share profile",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))
                TriggerProfileGroupedList(Modifier.padding(horizontal = 16.dp)) {
                    TriggerProfileDetailRow(
                        label = "Name",
                        value = user.effectiveDisplayName,
                        onClick = onOpenEditProfile,
                        showChevron = true,
                        valueMaxLines = 2,
                    )
                    TriggerProfileGroupedDivider()
                    TriggerProfileDetailRow(
                        label = TriggerStrings.Ui.ABOUT,
                        value = user.bio.ifBlank { TriggerStrings.Defaults.NEW_USER_BIO },
                        onClick = onOpenEditProfile,
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
                    TriggerProfileGroupedDivider()
                    TriggerProfileDetailRow(
                        label = TriggerStrings.Ui.VERIFY_PROFILE,
                        value = if (user.isFaceVerified) {
                            TriggerStrings.Ui.VERIFY_STATUS_VERIFIED
                        } else {
                            TriggerStrings.Ui.VERIFY_PROFILE_CTA
                        },
                        onClick = onOpenVerification,
                        showChevron = true,
                    )
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

    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(16.dp),
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
