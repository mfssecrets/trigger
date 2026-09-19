package com.triggerapp.feature.auth.ui.newpassword

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imeNestedScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.triggerapp.core.strings.TriggerStrings
import com.triggerapp.core.ui.TriggerPacificoFamily
import com.triggerapp.core.ui.triggerKeyboardInsetPadding
import com.triggerapp.feature.auth.presentation.newpassword.NewPasswordUiEffect
import com.triggerapp.feature.auth.presentation.newpassword.NewPasswordUiEvent
import com.triggerapp.feature.auth.presentation.newpassword.NewPasswordUiState
import com.triggerapp.feature.auth.presentation.newpassword.NewPasswordViewModel
import com.triggerapp.feature.auth.ui.components.AuthLoadingOverlay
import com.triggerapp.feature.auth.ui.components.AuthOutlinedField
import com.triggerapp.feature.auth.ui.components.FramedAuthButton
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel

/**
 * Stateless new-password UI: two password fields with inline validation, loading overlay.
 *
 * @param state MVI [NewPasswordUiState].
 * @param passwordVisible Whether the first field shows plain text.
 * @param confirmVisible Whether the confirm field shows plain text.
 * @param onPasswordVisibilityChange Toggles first field visibility.
 * @param onConfirmVisibilityChange Toggles confirm field visibility.
 * @param onEvent Dispatches [NewPasswordUiEvent].
 * @author udit
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun NewPasswordScreenContent(
    state: NewPasswordUiState,
    passwordVisible: Boolean,
    confirmVisible: Boolean,
    onPasswordVisibilityChange: () -> Unit,
    onConfirmVisibilityChange: () -> Unit,
    onEvent: (NewPasswordUiEvent) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .triggerKeyboardInsetPadding()
                .verticalScroll(rememberScrollState())
                .imeNestedScroll()
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(40.dp))
            Text(
                text = TriggerStrings.Ui.NEW_PASSWORD_TITLE,
                color = MaterialTheme.colorScheme.onBackground,
                fontFamily = TriggerPacificoFamily,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            )
            Text(
                text = TriggerStrings.Ui.NEW_PASSWORD_SUBTITLE,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            Spacer(Modifier.height(24.dp))
            AuthOutlinedField(
                value = state.password,
                onValueChange = { onEvent(NewPasswordUiEvent.PasswordChanged(it)) },
                placeholder = TriggerStrings.Ui.NEW_PASSWORD,
                visualTransformation = if (passwordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next,
                ),
                trailingIcon = {
                    IconButton(onClick = onPasswordVisibilityChange) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = if (passwordVisible) {
                                TriggerStrings.Ui.HIDE_PASSWORD
                            } else {
                                TriggerStrings.Ui.SHOW_PASSWORD
                            },
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
            )
            state.fieldErrors["password"]?.let { msg ->
                Text(
                    msg,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, top = 6.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            AuthOutlinedField(
                value = state.confirm,
                onValueChange = { onEvent(NewPasswordUiEvent.ConfirmChanged(it)) },
                placeholder = TriggerStrings.Ui.CONFIRM_PASSWORD,
                visualTransformation = if (confirmVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (!state.loading) {
                            focusManager.clearFocus()
                            keyboard?.hide()
                            onEvent(NewPasswordUiEvent.Submit)
                        }
                    },
                ),
                trailingIcon = {
                    IconButton(onClick = onConfirmVisibilityChange) {
                        Icon(
                            imageVector = if (confirmVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = if (confirmVisible) {
                                TriggerStrings.Ui.HIDE_PASSWORD
                            } else {
                                TriggerStrings.Ui.SHOW_PASSWORD
                            },
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
            )
            state.fieldErrors["confirm"]?.let { msg ->
                Text(
                    msg,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, top = 6.dp),
                )
            }
            state.errorMessage?.let { msg ->
                Spacer(Modifier.height(8.dp))
                Text(
                    msg,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
            Spacer(Modifier.height(28.dp))
            FramedAuthButton(
                text = if (state.loading) TriggerStrings.Ui.UPDATING_PASSWORD else TriggerStrings.Ui.UPDATE_PASSWORD,
                onClick = {
                    focusManager.clearFocus()
                    keyboard?.hide()
                    onEvent(NewPasswordUiEvent.Submit)
                },
                enabled = state.canSubmit,
            )
            Spacer(Modifier.height(32.dp))
        }
        AuthLoadingOverlay(state.loading)
    }
}

/**
 * New-password route: wires [NewPasswordViewModel], collects [NewPasswordUiEffect].
 *
 * @param onSuccessHome Password changed and signed in — go to the app shell.
 * @param viewModel Injected [NewPasswordViewModel].
 * @author udit
 */
@Composable
fun NewPasswordRoute(
    onSuccessHome: () -> Unit,
    viewModel: NewPasswordViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmVisible by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                NewPasswordUiEffect.NavigateHome -> onSuccessHome()
            }
        }
    }

    NewPasswordScreenContent(
        state = state,
        passwordVisible = passwordVisible,
        confirmVisible = confirmVisible,
        onPasswordVisibilityChange = { passwordVisible = !passwordVisible },
        onConfirmVisibilityChange = { confirmVisible = !confirmVisible },
        onEvent = viewModel::onEvent,
    )
}
