package com.triggerapp.feature.auth.ui.otp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.triggerapp.core.strings.TriggerStrings
import com.triggerapp.core.ui.TriggerPacificoFamily
import com.triggerapp.core.ui.triggerKeyboardInsetPadding
import com.triggerapp.core.ui.theme.TriggerAccent
import com.triggerapp.core.ui.theme.TriggerScreenBackground
import com.triggerapp.core.ui.theme.TriggerTheme
import com.triggerapp.domain.repository.OtpPurpose
import com.triggerapp.feature.auth.presentation.otp.OtpUiEffect
import com.triggerapp.feature.auth.presentation.otp.OtpUiEvent
import com.triggerapp.feature.auth.presentation.otp.OtpUiState
import com.triggerapp.feature.auth.presentation.otp.OtpViewModel
import com.triggerapp.feature.auth.ui.components.AuthLoadingOverlay
import com.triggerapp.feature.auth.ui.components.FramedAuthButton
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel

private val OtpBoxIdle = Color(0xFF3A3A3C)
private val OtpBoxError = Color(0xFFE5484D)
private val OtpBoxFilled = Color(0xFF63FFA3)
private val ResendEnabled = TriggerAccent

private const val OTP_LENGTH = 6

/**
 * One hidden numeric text field driving six visual code boxes — keeps backspace, paste and
 * keyboard behavior native while the user sees classic OTP boxes.
 *
 * @param code Entered digits so far.
 * @param enabled False while a round-trip is running.
 * @param hasError Styles boxes with the error color.
 * @param onCodeChange Emits the digit-only code (max 6 chars).
 * @author udit
 */
@Composable
internal fun OtpInputRow(
    code: String,
    enabled: Boolean,
    hasError: Boolean,
    onCodeChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        repeat(OTP_LENGTH) { index ->
            val char = code.getOrNull(index)
            val isNext = code.length == index
            val borderColor = when {
                hasError && char != null -> OtpBoxError
                char != null -> OtpBoxFilled
                isNext -> TriggerAccent
                else -> OtpBoxIdle
            }
            Box(
                modifier = Modifier
                    .size(width = 46.dp, height = 58.dp)
                    .background(Color(0xFF141414), MaterialTheme.shapes.medium)
                    .border(1.5.dp, borderColor, MaterialTheme.shapes.medium),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = char?.toString().orEmpty(),
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
    // Invisible driver field: one logical cursor so backspace deletes backwards and clipboard
    // paste fills all boxes at once.
    BasicTextField(
        value = code,
        onValueChange = { raw -> onCodeChange(raw.filter { it.isDigit() }.take(OTP_LENGTH)) },
        enabled = enabled,
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.NumberPassword,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        cursorBrush = SolidColor(Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .alpha(0.01f),
    )
}

/**
 * Stateless OTP screen UI: header, six code boxes, verify button, resend countdown, feedback.
 *
 * @param state MVI [OtpUiState].
 * @param onEvent Dispatches [OtpUiEvent].
 * @param onBack Navigate back (wrong email).
 * @author udit
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun OtpScreenContent(
    state: OtpUiState,
    onEvent: (OtpUiEvent) -> Unit,
    onBack: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(TriggerScreenBackground),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .imeNestedScroll()
                .triggerKeyboardInsetPadding()
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = TriggerStrings.Ui.BACK,
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            Text(
                text = TriggerStrings.Ui.OTP_TITLE,
                color = MaterialTheme.colorScheme.onBackground,
                fontFamily = TriggerPacificoFamily,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            )
            val subtitlePrefix = if (state.purpose == OtpPurpose.SIGN_UP) {
                TriggerStrings.Ui.OTP_SUBTITLE_SIGNUP
            } else {
                TriggerStrings.Ui.OTP_SUBTITLE_RESET
            }
            Text(
                text = subtitlePrefix + state.email + TriggerStrings.Ui.OTP_SUBTITLE_TAIL,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            Spacer(Modifier.height(28.dp))
            OtpInputRow(
                code = state.code,
                enabled = !state.busy,
                hasError = state.errorMessage != null,
                onCodeChange = { onEvent(OtpUiEvent.CodeChanged(it)) },
            )
            state.errorMessage?.let { msg ->
                Spacer(Modifier.height(12.dp))
                Text(
                    msg,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
            state.infoMessage?.let { msg ->
                Spacer(Modifier.height(12.dp))
                Text(
                    msg,
                    color = OtpBoxFilled,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
            Spacer(Modifier.height(24.dp))
            FramedAuthButton(
                text = if (state.verifying) TriggerStrings.Ui.OTP_VERIFYING else TriggerStrings.Ui.OTP_VERIFY,
                onClick = { onEvent(OtpUiEvent.Submit) },
                enabled = !state.busy && state.code.length == OTP_LENGTH,
            )
            Spacer(Modifier.height(20.dp))
            val resending = state.sending
            val countdown = state.resendSecondsLeft
            val resendText = when {
                resending -> TriggerStrings.Ui.OTP_SENDING
                countdown > 0 -> TriggerStrings.Ui.OTP_RESEND_IN + countdown + TriggerStrings.Ui.OTP_RESEND_SECONDS
                else -> TriggerStrings.Ui.OTP_RESEND
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !resending && countdown <= 0 && !state.busy) {
                        onEvent(OtpUiEvent.Resend)
                    }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = resendText,
                    color = if (!resending && countdown <= 0) {
                        ResendEnabled
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !state.busy, onClick = onBack)
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = TriggerStrings.Ui.OTP_CHANGE_EMAIL,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Spacer(Modifier.height(32.dp))
        }
        AuthLoadingOverlay(state.busy)
    }
}

/**
 * OTP route: wires [OtpViewModel], collects [OtpUiEffect], hosts [OtpScreenContent].
 *
 * @param onVerifiedHome Signup completed and signed in — go to the app shell.
 * @param onNewPassword Reset code accepted — go to the new-password screen.
 * @param onBack Navigate back.
 * @param viewModel Injected [OtpViewModel].
 * @author udit
 */
@Composable
fun OtpRoute(
    onVerifiedHome: () -> Unit,
    onNewPassword: () -> Unit,
    onBack: () -> Unit,
    viewModel: OtpViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                OtpUiEffect.NavigateHome -> onVerifiedHome()
                OtpUiEffect.NavigateNewPassword -> onNewPassword()
            }
        }
    }

    OtpScreenContent(
        state = state,
        onEvent = viewModel::onEvent,
        onBack = onBack,
    )
}
