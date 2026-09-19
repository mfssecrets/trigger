package com.triggerapp.feature.home.ui.feed

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.triggerapp.core.ui.TriggerProfileAvatar
import com.triggerapp.core.ui.theme.TriggerAccent

private val DialogBackground = Color(0xFF161B21)
private val InputSurface = Color(0xFF1D242D)
private val TextMuted = Color(0xFFAFACAC)

/**
 * Bottom sheet dialog allowing users to create and publish a new post with text, tags, and gallery photos.
 * @author triggerapp
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostDialog(
    currentUserAvatarUrl: String,
    currentUserName: String,
    currentUserHandle: String,
    onDismiss: () -> Unit,
    onPublishPost: (content: String, imageUri: String?, tags: List<String>) -> Unit,
) {
    var contentText by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    val availableTags = remember { listOf("#update", "#tech", "#design", "#trigger", "#community") }
    val selectedTags = remember { mutableStateListOf<String>() }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            selectedImageUri = uri
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DialogBackground,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .size(width = 38.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF384350)),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TriggerProfileAvatar(
                        imageUrl = currentUserAvatarUrl,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape),
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = currentUserName,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                        )
                        Text(
                            text = "@$currentUserHandle",
                            color = TriggerAccent,
                            fontSize = 12.sp,
                        )
                    }
                }

                Button(
                    onClick = {
                        val text = contentText.trim()
                        if (text.isNotEmpty() || selectedImageUri != null) {
                            onPublishPost(text, selectedImageUri?.toString(), selectedTags.toList())
                            onDismiss()
                        }
                    },
                    enabled = contentText.trim().isNotEmpty() || selectedImageUri != null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TriggerAccent,
                        contentColor = Color.Black,
                        disabledContainerColor = Color(0xFF26323E),
                        disabledContentColor = TextMuted,
                    ),
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Text("Post", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(14.dp))

            // Text input
            OutlinedTextField(
                value = contentText,
                onValueChange = { contentText = it },
                placeholder = {
                    Text(
                        text = "What's triggering your mind today?",
                        color = TextMuted,
                        fontSize = 15.sp,
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = InputSurface,
                    unfocusedContainerColor = InputSurface,
                    focusedBorderColor = TriggerAccent.copy(alpha = 0.5f),
                    unfocusedBorderColor = Color(0xFF2A3644),
                    cursorColor = TriggerAccent,
                ),
                shape = RoundedCornerShape(14.dp),
            )

            // Image Preview if picked
            selectedImageUri?.let { uri ->
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFF2E3B48), RoundedCornerShape(12.dp)),
                ) {
                    AsyncImage(
                        model = uri,
                        contentDescription = "Selected image",
                        modifier = Modifier.fillMaxWidth(),
                        contentScale = ContentScale.Crop,
                    )
                    IconButton(
                        onClick = { selectedImageUri = null },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(28.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove photo",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Tag selector row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(availableTags) { tag ->
                    val isSelected = selectedTags.contains(tag)
                    Surface(
                        color = if (isSelected) TriggerAccent.copy(alpha = 0.18f) else Color(0xFF1E2630),
                        shape = RoundedCornerShape(14.dp),
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, TriggerAccent) else null,
                        modifier = Modifier.clickable {
                            if (isSelected) selectedTags.remove(tag) else selectedTags.add(tag)
                        },
                    ) {
                        Text(
                            text = tag,
                            color = if (isSelected) TriggerAccent else TextMuted,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Action: Add Photo
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    }
                    .background(Color(0xFF1B232D), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = "Attach photo",
                    tint = TriggerAccent,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = if (selectedImageUri != null) "Change Photo" else "Attach Photo from Gallery",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
