package com.triggerapp.feature.profile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.triggerapp.core.strings.TriggerStrings
import com.triggerapp.core.ui.theme.TriggerScreenBackground
import com.triggerapp.core.ui.theme.TriggerTheme
import com.triggerapp.feature.profile.presentation.EditProfileUiEvent
import com.triggerapp.feature.profile.presentation.EditProfileViewModel
import org.koin.androidx.compose.koinViewModel

private val FieldContainer = Color(0xFF242C35)
private val FieldBorder = Color(0xFF384351)
private val MintAccent = Color(0xFF63FFA3)
private val AppBarBlack = Color(0xFF000000)
private val MutedText = Color(0xFFAFACAC)

/**
 * Full-screen Edit Profile: name, username, bio, gender dropdown, date of birth (DD/MM/YYYY
 * with live age), and a bottom Save button with loading state.
 *
 * **Insets:** the M3 [TopAppBar] applies status-bar insets natively and the pinned bottom bar
 * applies navigation-bar + IME insets, so no content is overlaid by the system status bar,
 * gesture bar, or keyboard.
 *
 * @param onBack Navigate up after saving or when the back arrow is pressed.
 * @param viewModel MVI [EditProfileUiState] + [EditProfileUiEvent].
 * @author udit
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileRoute(
    onBack: () -> Unit,
    viewModel: EditProfileViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.saveError) {
        state.saveError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onEvent(EditProfileUiEvent.SaveErrorShown)
        }
    }
    LaunchedEffect(state.saved) {
        if (state.saved) onBack()
    }

    Scaffold(
        containerColor = TriggerScreenBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = TriggerStrings.Ui.EDIT_PROFILE,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppBarBlack,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            // Pinned save bar: navigation-bar + keyboard insets keep it visible and unobstructed.
            Surface(color = AppBarBlack) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    Button(
                        onClick = { viewModel.onEvent(EditProfileUiEvent.Save) },
                        enabled = state.canSave,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MintAccent,
                            contentColor = Color.Black,
                            disabledContainerColor = FieldContainer,
                            disabledContentColor = MutedText,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                    ) {
                        if (state.saving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = Color.Black,
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "Saving…",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        } else {
                            Text(
                                text = TriggerStrings.Ui.SAVE,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        },
    ) { padding ->
        when {
            state.initialLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
            state.loadError != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = state.loadError.orEmpty(),
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(16.dp))
                    FilledTonalButton(onClick = { viewModel.onEvent(EditProfileUiEvent.RetryLoad) }) {
                        Text(TriggerStrings.Ui.TRY_AGAIN)
                    }
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    OutlinedTextField(
                        value = state.name,
                        onValueChange = { viewModel.onEvent(EditProfileUiEvent.NameChanged(it)) },
                        label = { Text(TriggerStrings.Ui.NAME) },
                        singleLine = true,
                        supportingText = {
                            Text(
                                "${state.name.length}/${com.triggerapp.domain.text.DisplayTextLimits.MAX_USERNAME_CHARS}",
                                color = MutedText,
                            )
                        },
                        colors = editFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = state.username,
                        onValueChange = { viewModel.onEvent(EditProfileUiEvent.UsernameChanged(it)) },
                        label = { Text(TriggerStrings.Ui.USERNAME) },
                        prefix = { Text("@", color = MintAccent) },
                        singleLine = true,
                        isError = state.usernameError != null,
                        supportingText = {
                            when {
                                state.usernameError != null -> Text(
                                    state.usernameError.orEmpty(),
                                    color = MaterialTheme.colorScheme.error,
                                )
                                else -> Text(
                                    "Unique handle — validated server-side",
                                    color = MutedText,
                                )
                            }
                        },
                        colors = editFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = state.bio,
                        onValueChange = { viewModel.onEvent(EditProfileUiEvent.BioChanged(it)) },
                        label = { Text(TriggerStrings.Ui.BIO) },
                        minLines = 3,
                        maxLines = com.triggerapp.domain.text.DisplayTextLimits.MAX_BIO_LINES,
                        supportingText = {
                            Text(
                                "${state.bio.length}/${com.triggerapp.domain.text.DisplayTextLimits.MAX_BIO_CHARS}",
                                color = MutedText,
                            )
                        },
                        colors = editFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(12.dp))
                    GenderDropdown(
                        selected = state.gender,
                        expanded = state.genderMenuExpanded,
                        onToggle = { viewModel.onEvent(EditProfileUiEvent.GenderMenuToggled(it)) },
                        onSelect = { viewModel.onEvent(EditProfileUiEvent.GenderSelected(it)) },
                        options = EditProfileViewModel.genderOptions,
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = state.dobText,
                        onValueChange = { viewModel.onEvent(EditProfileUiEvent.DobChanged(it)) },
                        label = { Text(TriggerStrings.Ui.DATE_OF_BIRTH) },
                        placeholder = { Text(TriggerStrings.Ui.DOB_HINT, color = MutedText) },
                        singleLine = true,
                        isError = state.dobError != null,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.NumberPassword,
                            imeAction = ImeAction.Done,
                        ),
                        supportingText = {
                            when {
                                state.dobError != null -> Text(
                                    state.dobError.orEmpty(),
                                    color = MaterialTheme.colorScheme.error,
                                )
                                state.ageText.isNotBlank() -> Text(
                                    state.ageText,
                                    color = MintAccent,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                else -> Text(TriggerStrings.Ui.DOB_HINT, color = MutedText)
                            }
                        },
                        colors = editFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GenderDropdown(
    selected: String,
    expanded: Boolean,
    onToggle: (Boolean) -> Unit,
    onSelect: (String) -> Unit,
    options: List<String>,
) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onToggle,
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(TriggerStrings.Ui.GENDER) },
            placeholder = { Text(TriggerStrings.Ui.GENDER_UNDISCLOSED, color = MutedText) },
            trailingIcon = {
                Icon(
                    Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MutedText,
                )
            },
            colors = editFieldColors(),
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onToggle(false) },
            containerColor = Color(0xFF1E242B),
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            option,
                            color = if (option == selected) MintAccent else Color.White,
                            fontWeight = if (option == selected) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    },
                    onClick = { onSelect(option) },
                )
            }
        }
    }
}

@Composable
private fun editFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedContainerColor = FieldContainer,
    unfocusedContainerColor = FieldContainer,
    focusedBorderColor = MintAccent,
    unfocusedBorderColor = FieldBorder,
    cursorColor = MintAccent,
    focusedLabelColor = MintAccent,
    unfocusedLabelColor = MutedText,
)

@Preview(showBackground = true, showSystemUi = false, name = "Edit profile · fields")
@Composable
private fun EditProfileScreenPreview() {
    TriggerTheme(darkTheme = true, dynamicColor = false, useBrandDarkColors = true) {
        Box(Modifier.background(TriggerScreenBackground))
    }
}
