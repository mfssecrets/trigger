package com.triggerapp.feature.profile.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.triggerapp.core.strings.TriggerStrings
import com.triggerapp.core.ui.theme.TriggerScreenBackground
import com.triggerapp.feature.profile.verification.FaceLivenessAnalyzer
import com.triggerapp.feature.profile.verification.LivenessPhase
import com.triggerapp.feature.profile.presentation.VerificationStage
import com.triggerapp.feature.profile.presentation.VerificationUiEvent
import com.triggerapp.feature.profile.presentation.VerificationViewModel
import java.util.concurrent.Executors
import org.koin.androidx.compose.koinViewModel

private val MintAccent = Color(0xFF63FFA3)
private val AppBarBlack = Color.Black
private val CardContainer = Color(0xFF242C35)
private val CardBorder = Color(0xFF384351)
private val MutedText = Color(0xFFAFACAC)
private val WarnColor = Color(0xFFFFD54F)
private val ErrorColor = Color(0xFFFF5252)

/**
 * Realtime face verification: CameraX preview + ML Kit liveness challenge
 * (centre -> blink -> smile) followed by an on-device gender classification.
 * Nothing is uploaded during the challenge; only the final boolean/label/confidence
 * result is written to the user's profile.
 *
 * @param onBack Navigate up (back arrow, Done, or Cancel).
 * @param viewModel MVI [VerificationViewModel].
 * @author udit
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerificationRoute(
    onBack: () -> Unit,
    viewModel: VerificationViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = TriggerScreenBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = TriggerStrings.Ui.VERIFY_PROFILE,
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
    ) { padding ->
        when (state.stage) {
            VerificationStage.LOADING_STATUS -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = Color.White)
            }

            VerificationStage.INTRO -> IntroContent(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                alreadyVerified = state.alreadyVerified,
                detectedLabel = state.existing?.genderLabel.orEmpty(),
                confidence = state.existing?.confidence ?: 0.0,
                loadError = state.loadError,
                onStart = { viewModel.onEvent(VerificationUiEvent.Start) },
                onRetryLoad = { viewModel.onEvent(VerificationUiEvent.RetryLoad) },
            )
            VerificationStage.LIVE,
            VerificationStage.ANALYZING,
            VerificationStage.SAVING,
            -> LiveContent(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                stage = state.stage,
                livenessPhase = state.livenessPhase,
                multipleFacesWarning = state.multipleFacesWarning,
                onBack = onBack,
                onPhaseChanged = { viewModel.onEvent(VerificationUiEvent.LivenessPhaseChanged(it)) },
                onMultipleFaces = { viewModel.onEvent(VerificationUiEvent.MultipleFaces) },
                onFaceCaptured = { viewModel.onEvent(VerificationUiEvent.FaceCaptured(it)) },
            )

            VerificationStage.SUCCESS -> SuccessContent(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                detectedLabel = state.result?.genderLabel.orEmpty(),
                confidence = state.result?.confidence ?: 0.0,
                onDone = onBack,
            )

            VerificationStage.FAILED -> FailedContent(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                message = state.error ?: TriggerStrings.Errors.VERIFICATION_FAILED,
                onRetry = { viewModel.onEvent(VerificationUiEvent.Retry) },
                onBack = onBack,
            )
        }
    }
}

@Composable
private fun IntroContent(
    modifier: Modifier = Modifier,
    alreadyVerified: Boolean,
    detectedLabel: String,
    confidence: Double,
    loadError: String?,
    onStart: () -> Unit,
    onRetryLoad: () -> Unit,
) {
    Column(
        modifier = modifier
            .background(TriggerScreenBackground)
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(20.dp))
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = CardContainer,
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    text = TriggerStrings.Ui.VERIFY_INTRO_TITLE,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = TriggerStrings.Ui.VERIFY_INTRO_BODY,
                    color = MutedText,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                )
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (alreadyVerified) Icons.Filled.Verified else Icons.Outlined.Verified,
                        contentDescription = null,
                        tint = if (alreadyVerified) MintAccent else WarnColor,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (alreadyVerified) {
                            if (detectedLabel.isNotBlank()) {
                                "${TriggerStrings.Ui.VERIFIED_BADGE} · ${TriggerStrings.Ui.VERIFY_DETECTED_GENDER}: $detectedLabel (${(confidence * 100).toInt()}%)"
                            } else {
                                TriggerStrings.Ui.VERIFIED_BADGE
                            }
                        } else {
                            TriggerStrings.Ui.NOT_VERIFIED
                        },
                        color = if (alreadyVerified) MintAccent else WarnColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                    )
                }
            }
        }
        if (loadError != null) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = loadError,
                color = ErrorColor,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onRetryLoad) {
                Text(TriggerStrings.Ui.TRY_AGAIN, color = Color.White)
            }
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onStart,
            colors = ButtonDefaults.buttonColors(
                containerColor = MintAccent,
                contentColor = Color.Black,
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            Text(
                text = if (alreadyVerified) TriggerStrings.Ui.VERIFY_REDO else TriggerStrings.Ui.VERIFY_START,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
            )
        }
    }
}

/**
 * Camera stage: binds the front camera while the stage is LIVE, streams frames through
 * [FaceLivenessAnalyzer], and draws the circular face guide over the preview.
 * @author udit
 */
@Composable
private fun LiveContent(
    modifier: Modifier = Modifier,
    stage: VerificationStage,
    livenessPhase: LivenessPhase,
    multipleFacesWarning: Boolean,
    onBack: () -> Unit,
    onPhaseChanged: (LivenessPhase) -> Unit,
    onMultipleFaces: () -> Unit,
    onFaceCaptured: (android.graphics.Bitmap) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var cameraError by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) cameraError = true
    }
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val previewView = remember { PreviewView(context) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val analyzer = remember {
        FaceLivenessAnalyzer(
            onPhaseChanged = onPhaseChanged,
            onFaceCaptured = onFaceCaptured,
            onMultipleFaces = onMultipleFaces,
        )
    }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    // Bind only while LIVE; unbind as soon as the frame is captured / saving starts.
    DisposableEffect(stage, hasCameraPermission) {
        if (stage == VerificationStage.LIVE && hasCameraPermission) {
            val future = ProcessCameraProvider.getInstance(context)
            future.addListener(
                {
                    try {
                        val provider = future.get()
                        cameraProvider = provider
                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }
                        val analysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also { it.setAnalyzer(cameraExecutor, analyzer) }
                        provider.unbindAll()
                        provider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_FRONT_CAMERA,
                            preview,
                            analysis,
                        )
                        cameraError = false
                    } catch (_: Exception) {
                        cameraError = true
                    }
                },
                ContextCompat.getMainExecutor(context),
            )
        }
        onDispose {
            runCatching { cameraProvider?.unbindAll() }
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
            analyzer.release()
        }
    }

    Box(modifier.fillMaxSize()) {
        if (hasCameraPermission && !cameraError) {
            AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
            FaceGuideOverlay(Modifier.fillMaxSize())
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (multipleFacesWarning) {
                Text(
                    text = TriggerStrings.Ui.VERIFY_MULTIPLE_FACES,
                    color = WarnColor,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.Black.copy(alpha = 0.72f),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (stage == VerificationStage.LIVE) {
                        Text(
                            text = when (livenessPhase) {
                                LivenessPhase.CENTER_FACE -> TriggerStrings.Ui.VERIFY_PROMPT_CENTER
                                LivenessPhase.BLINK -> TriggerStrings.Ui.VERIFY_PROMPT_BLINK
                                LivenessPhase.SMILE -> TriggerStrings.Ui.VERIFY_PROMPT_SMILE
                            },
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                        )
                    } else {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MintAccent,
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = TriggerStrings.Ui.VERIFY_ANALYZING,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                        )
                    }
                }
            }
        }

        if (cameraError) {
            Surface(
                color = Color(0xFF5C3A2E),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp),
                ) {
                    Text(
                        text = TriggerStrings.Errors.VERIFICATION_CAMERA_FAILED,
                        color = Color.White,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onBack) {
                        Text(TriggerStrings.Ui.BACK, color = Color.White)
                    }
                }
            }
        }
    }
}

/**
 * Dark scrim with a circular hole plus accent ring marking where the face should sit.
 * @author udit
 */
@Composable
private fun FaceGuideOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val radius = size.minDimension * 0.36f
        val centre = Offset(size.width / 2f, size.height * 0.42f)
        val hole = Rect(
            left = centre.x - radius,
            top = centre.y - radius,
            right = centre.x + radius,
            bottom = centre.y + radius,
        )
        val path = Path().apply {
            fillType = PathFillType.EvenOdd
            addRect(Rect(Offset.Zero, size))
            addOval(hole)
        }
        drawPath(path, Color.Black.copy(alpha = 0.55f))
        drawCircle(
            color = MintAccent,
            radius = radius,
            center = centre,
            style = Stroke(width = 2.dp.toPx()),
        )
    }
}

@Composable
private fun SuccessContent(
    modifier: Modifier = Modifier,
    detectedLabel: String,
    confidence: Double,
    onDone: () -> Unit,
) {
    Column(
        modifier = modifier
            .background(TriggerScreenBackground)
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        Icon(
            imageVector = Icons.Filled.Verified,
            contentDescription = null,
            tint = MintAccent,
            modifier = Modifier.size(72.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = TriggerStrings.Ui.VERIFY_SUCCESS_TITLE,
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = TriggerStrings.Ui.VERIFY_SUCCESS_BODY,
            color = MutedText,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
        )
        if (detectedLabel.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = "${TriggerStrings.Ui.VERIFY_DETECTED_GENDER}: $detectedLabel (${(confidence * 100).toInt()}%) · ${TriggerStrings.Ui.VERIFY_STATUS_VERIFIED}",
                color = MutedText,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.weight(1f))
        Button(
            onClick = onDone,
            colors = ButtonDefaults.buttonColors(
                containerColor = MintAccent,
                contentColor = Color.Black,
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(48.dp),
        ) {
            Text(TriggerStrings.Ui.VERIFY_DONE, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun FailedContent(
    modifier: Modifier = Modifier,
    message: String,
    onRetry: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = modifier
            .background(TriggerScreenBackground)
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        Icon(
            imageVector = Icons.Outlined.Verified,
            contentDescription = null,
            tint = ErrorColor,
            modifier = Modifier.size(64.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = message,
            color = Color.White,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.weight(1f))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = MintAccent,
                contentColor = Color.Black,
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            Text(TriggerStrings.Ui.TRY_AGAIN, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        }
        Spacer(Modifier.height(8.dp))
        FilledTonalButton(
            onClick = onBack,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            Text(TriggerStrings.Ui.BACK, color = Color.White, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(16.dp))
    }
}
