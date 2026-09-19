package com.triggerapp.feature.auth.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
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
import com.triggerapp.core.ui.triggerKeyboardInsetPadding
import com.triggerapp.feature.auth.ui.components.AuthLoadingOverlay
import com.triggerapp.feature.auth.ui.components.AuthOutlinedField
import com.triggerapp.feature.auth.ui.components.FramedAuthButton
import com.triggerapp.feature.auth.presentation.login.LoginUiEffect
import com.triggerapp.feature.auth.presentation.login.LoginUiEvent
import com.triggerapp.feature.auth.presentation.login.LoginUiState
import com.triggerapp.feature.auth.presentation.login.LoginViewModel
import kotlinx.coroutines.flow.collectLatest

/**
 * Stateless login UI: form, links to register and forgot password.
 *
 * @param state MVI [LoginUiState] from [LoginViewModel].
 * @param passwordVisible Whether the password field uses plain text or masking.
 * @param onPasswordVisibleChange Toggles password visibility.
 * @param onEvent Dispatches [LoginUiEvent] to the ViewModel.
 * @param onRegister Navigate to registration.
 * @param onForgot Navigate to password reset.
 * @author udit
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LoginScreenContent(
    state: LoginUiState,
    passwordVisible: Boolean,
    onPasswordVisibleChange: () -> Unit,
    onEvent: (LoginUiEvent) -> Unit,
    onRegister: () -> Unit,
    onForgot: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    Box(Modifier.fillMaxSize().background(TriggerScreenBackground)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .triggerKeyboardInsetPadding()
                .verticalScroll(rememberScrollState())
                .imeNestedScroll()
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(32.dp))
            Text(
                text = TriggerStrings.Ui.HI_THERE,
                color = MaterialTheme.colorScheme.onBackground,
                fontFamily = TriggerPacificoFamily,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            )
            Text(
                text = TriggerStrings.Ui.LOG_IN_TO_CONTINUE,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            )
            Spacer(Modifier.height(20.dp))
            AuthOutlinedField(
                value = state.email,
                onValueChange = { onEvent(LoginUiEvent.EmailChanged(it)) },
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
                onValueChange = { onEvent(LoginUiEvent.PasswordChanged(it)) },
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
                            onEvent(LoginUiEvent.Submit)
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
                text = TriggerStrings.Ui.LOGIN,
                onClick = {
                    focusManager.clearFocus()
                    keyboard?.hide()
                    onEvent(LoginUiEvent.Submit)
                },
                enabled = !state.loading,
            )
            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = TriggerStrings.Ui.DONT_HAVE_AN_ACCOUNT,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = TriggerStrings.Ui.REGISTER,
                    color = TriggerAccent,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .clickable(onClick = onRegister),
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable(onClick = onForgot),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = TriggerStrings.Ui.FORGET_PASSWORD_LINE.trimEnd(),
                    color = TriggerAccent,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
            }
            Spacer(Modifier.height(32.dp))
        }
        AuthLoadingOverlay(state.loading)
    }
}

/**
 * Login screen route: wires [LoginViewModel] (MVI), collects navigation [LoginUiEffect], hosts [LoginScreenContent].
 *
 * @param onSuccess Invoked after successful sign-in (e.g. navigate to home).
 * @param onRegister Navigate to registration.
 * @param onForgot Navigate to forgot password.
 * @param viewModel Injected [LoginViewModel].
 * @author udit
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LoginRoute(
    onSuccess: () -> Unit,
    onRegister: () -> Unit,
    onForgot: () -> Unit,
    viewModel: LoginViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var passwordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                LoginUiEffect.NavigateHome -> {
                    viewModel.onEvent(LoginUiEvent.ClearForm)
                    onSuccess()
                }
            }
        }
    }

    LoginScreenContent(
        state = state,
        passwordVisible = passwordVisible,
        onPasswordVisibleChange = { passwordVisible = !passwordVisible },
        onEvent = viewModel::onEvent,
        onRegister = onRegister,
        onForgot = onForgot,
    )
}

/**
 * Compose preview for [LoginScreenContent] with sample credentials and themed logo header.
 * @author udit
 */
@OptIn(ExperimentalLayoutApi::class)
@Preview(showBackground = true, showSystemUi = false)
@Composable
private fun LoginScreenPreview() {
    TriggerTheme(darkTheme = true, dynamicColor = false, useBrandDarkColors = true) {
        LoginScreenContent(
            state = LoginUiState(
                email = "you@example.com",
                password = "••••••••",
                loading = false,
                errorMessage = null,
            ),
            passwordVisible = false,
            onPasswordVisibleChange = {},
            onEvent = {},
            onRegister = {},
            onForgot = {},
        )
    }
}
