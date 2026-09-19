package com.triggerapp.feature.auth.ui.register

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imeNestedScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.triggerapp.core.ui.TriggerPacificoFamily
import com.triggerapp.core.ui.theme.TriggerAccent
import com.triggerapp.core.ui.theme.TriggerScreenBackground
import com.triggerapp.core.ui.theme.TriggerTheme
import org.koin.androidx.compose.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.triggerapp.core.strings.TriggerStrings
import com.triggerapp.domain.text.DisplayTextLimits
import com.triggerapp.core.ui.triggerKeyboardInsetPadding
import com.triggerapp.feature.auth.R
import com.triggerapp.feature.auth.presentation.register.RegisterUiEffect
import com.triggerapp.feature.auth.presentation.register.RegisterUiEvent
import com.triggerapp.feature.auth.presentation.register.RegisterUiState
import com.triggerapp.feature.auth.presentation.register.RegisterViewModel
import com.triggerapp.feature.auth.presentation.register.UsernameStatus
import com.triggerapp.feature.auth.ui.components.AuthLoadingOverlay
import com.triggerapp.feature.auth.ui.components.AuthLogoHeader
import com.triggerapp.feature.auth.ui.components.AuthOutlinedField
import com.triggerapp.feature.auth.ui.components.FramedAuthButton
import kotlinx.coroutines.flow.collectLatest

/** Verified-available tick color (blue check, per spec). */
private val UsernameTickBlue = Color(0xFF1D9BF0)

/**
 * Stateless registration UI: fields, app logo header, link back to log in.
 *
 * @param state MVI [RegisterUiState].
 * @param passwordVisible Password masking flag.
 * @param onPasswordVisibleChange Toggles password visibility.
 * @param onEvent Dispatches [RegisterUiEvent].
 * @param onLogin Navigate to login.
 * @author udit
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun RegisterScreenContent(
    state: RegisterUiState,
    passwordVisible: Boolean,
    onPasswordVisibleChange: () -> Unit,
    onEvent: (RegisterUiEvent) -> Unit,
    onLogin: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    Box(Modifier.fillMaxSize().background(TriggerScreenBackground)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .triggerKeyboardInsetPadding()
                .verticalScroll(rememberScrollState())
                .imeNestedScroll()
                .padding(horizontal = 20.dp),
        ) {
            AuthLogoHeader()
            Text(
                text = TriggerStrings.Ui.WELCOME_SIGN,
                color = MaterialTheme.colorScheme.onBackground,
                fontFamily = TriggerPacificoFamily,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            )
            Text(
                text = TriggerStrings.Ui.SIGN_UP_HERE,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            )
            Spacer(Modifier.height(20.dp))
            AuthOutlinedField(
                value = state.username,
                onValueChange = { onEvent(RegisterUiEvent.UsernameChanged(it)) },
                placeholder = TriggerStrings.Ui.USERNAME,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
                ),
                trailingIcon = {
                    if (state.usernameStatus == UsernameStatus.AVAILABLE) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = TriggerStrings.Ui.USERNAME_AVAILABLE,
                            tint = UsernameTickBlue,
                        )
                    }
                },
            )
            val usernameHint = when (state.usernameStatus) {
                UsernameStatus.AVAILABLE -> TriggerStrings.Ui.USERNAME_AVAILABLE
                UsernameStatus.CHECKING -> TriggerStrings.Ui.USERNAME_CHECKING
                UsernameStatus.TAKEN -> TriggerStrings.Errors.USERNAME_TAKEN
                UsernameStatus.INVALID ->
                    if (state.username.length < DisplayTextLimits.MIN_USERNAME_CHARS) {
                        TriggerStrings.Errors.USERNAME_TOO_SHORT
                    } else {
                        TriggerStrings.Errors.USERNAME_INVALID_RULES
                    }
                UsernameStatus.IDLE -> TriggerStrings.Ui.USERNAME_HINT
            }
            val usernameHintColor = when (state.usernameStatus) {
                UsernameStatus.AVAILABLE -> UsernameTickBlue
                UsernameStatus.TAKEN, UsernameStatus.INVALID -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            Text(
                text = usernameHint,
                color = usernameHintColor,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, top = 6.dp),
            )
            Spacer(Modifier.height(12.dp))
            AuthOutlinedField(
                value = state.email,
                onValueChange = { onEvent(RegisterUiEvent.EmailChanged(it)) },
                placeholder = TriggerStrings.Ui.EMAIL,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
                ),
            )
            Spacer(Modifier.height(12.dp))
            AuthOutlinedField(
                value = state.password,
                onValueChange = { onEvent(RegisterUiEvent.PasswordChanged(it)) },
                placeholder = TriggerStrings.Ui.PASSWORD,
                visualTransformation = if (passwordVisible) {
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
                            onEvent(RegisterUiEvent.Submit)
                        }
                    },
                ),
                trailingIcon = {
                    IconButton(onClick = onPasswordVisibleChange) {
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
                text = TriggerStrings.Ui.SIGN_UP,
                onClick = {
                    focusManager.clearFocus()
                    keyboard?.hide()
                    onEvent(RegisterUiEvent.Submit)
                },
                enabled = !state.loading,
            )
            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = TriggerStrings.Ui.ALREADY_HAVE_AN_ACCOUNT,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = TriggerStrings.Ui.LOGIN_HERE,
                    color = TriggerAccent,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .clickable(onClick = onLogin),
                )
            }
            Spacer(Modifier.height(32.dp))
        }
        AuthLoadingOverlay(state.loading)
    }
}

/**
 * Registration route: [RegisterViewModel], [RegisterUiEffect] handling, and [RegisterScreenContent].
 *
 * @param onOtp Inputs accepted — navigate to the 6-digit email verification screen.
 * @param onLogin Navigate to login.
 * @param viewModel Injected [RegisterViewModel].
 * @author udit
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RegisterRoute(
    onOtp: () -> Unit,
    onLogin: () -> Unit,
    viewModel: RegisterViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var passwordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                RegisterUiEffect.NavigateOtp -> {
                    onOtp()
                }
            }
        }
    }

    RegisterScreenContent(
        state = state,
        passwordVisible = passwordVisible,
        onPasswordVisibleChange = { passwordVisible = !passwordVisible },
        onEvent = viewModel::onEvent,
        onLogin = onLogin,
    )
}

/**
 * Compose preview for [RegisterScreenContent] with sample fields and themed Lottie placeholder.
 * @author udit
 */
@OptIn(ExperimentalLayoutApi::class)
@Preview(showBackground = true, showSystemUi = false)
@Composable
private fun RegisterScreenPreview() {
    TriggerTheme(darkTheme = true, dynamicColor = false, useBrandDarkColors = true) {
        RegisterScreenContent(
            state = RegisterUiState(
                username = "new_user",
                email = "new@example.com",
                password = "••••••••",
                loading = false,
                errorMessage = null,
            ),
            passwordVisible = false,
            onPasswordVisibleChange = {},
            onEvent = {},
            onLogin = {},
        )
    }
}
