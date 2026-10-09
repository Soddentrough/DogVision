package com.example.dogvision.ui

import android.graphics.RenderEffect
import android.hardware.camera2.CameraCharacteristics
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlin.math.roundToInt
import androidx.core.content.ContextCompat
import com.example.dogvision.R
import com.example.dogvision.model.AnimalVisionProfile
import com.example.dogvision.shader.AnimalVisionShader

@Composable
fun CameraFocusLoader(
    primaryColor: Color = Color(0xFFFFD700),
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "focus_loader")

    val bracketOffset by infiniteTransition.animateFloat(
        initialValue = 4f,
        targetValue = 16f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bracket_offset"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    Canvas(modifier = modifier.size(100.dp)) {
        val width = size.width
        val height = size.height
        val center = Offset(width / 2, height / 2)

        val o = bracketOffset
        val len = 20f
        val stroke = 4f
        val color = primaryColor.copy(alpha = glowAlpha)

        // Top-left bracket
        drawLine(color, Offset(o, o), Offset(o + len, o), strokeWidth = stroke)
        drawLine(color, Offset(o, o), Offset(o, o + len), strokeWidth = stroke)

        // Top-right bracket
        drawLine(color, Offset(width - o, o), Offset(width - o - len, o), strokeWidth = stroke)
        drawLine(color, Offset(width - o, o), Offset(width - o, o + len), strokeWidth = stroke)

        // Bottom-left bracket
        drawLine(color, Offset(o, height - o), Offset(o + len, height - o), strokeWidth = stroke)
        drawLine(color, Offset(o, height - o), Offset(o, height - o - len), strokeWidth = stroke)

        // Bottom-right bracket
        drawLine(color, Offset(width - o, height - o), Offset(width - o - len, height - o), strokeWidth = stroke)
        drawLine(color, Offset(width - o, height - o), Offset(width - o, height - o + len), strokeWidth = stroke)

        // Central receptor point
        drawCircle(
            color = primaryColor.copy(alpha = glowAlpha),
            radius = 6f,
            center = center
        )

        // Target circle
        drawCircle(
            color = Color.White.copy(alpha = 0.15f),
            radius = width / 4,
            center = center,
            style = Stroke(width = 2f)
        )
    }
}

@ExperimentalCamera2Interop
@Composable
fun CameraPreview(
    animal: AnimalVisionProfile,
    isColorFilterEnabled: Boolean,
    isBlurEnabled: Boolean,
    isSplitEnabled: Boolean,
    splitPosition: Float,
    onSplitPositionChange: (Float) -> Unit,
    isTorchEnabled: Boolean = false,
    onTorchAvailable: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val density = LocalDensity.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var isCameraReady by remember { mutableStateOf(false) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var boundCamera by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }

    val shader = remember { AnimalVisionShader.createShader() }
    var elapsedTimeSec by remember { mutableFloatStateOf(0f) }

    val currentAnimal by rememberUpdatedState(animal)
    val currentBlur by rememberUpdatedState(isBlurEnabled)
    val currentColorFilter by rememberUpdatedState(isColorFilterEnabled)
    val currentSplitEnabled by rememberUpdatedState(isSplitEnabled)
    val currentSplitPosition by rememberUpdatedState(splitPosition)

    fun applyShaderTo(pv: PreviewView, t: Float) {
        val w = if (containerSize.width > 0) containerSize.width.toFloat() else pv.width.toFloat().coerceAtLeast(1f)
        val h = if (containerSize.height > 0) containerSize.height.toFloat() else pv.height.toFloat().coerceAtLeast(1f)
        shader.setFloatUniform("splitPos", currentSplitPosition)
        shader.setFloatUniform("isSplitMode", if (currentSplitEnabled) 1.0f else 0.0f)
        shader.setFloatUniform("isBlurEnabled", if (currentBlur) 1.0f else 0.0f)
        shader.setFloatUniform("isColorFilter", if (currentColorFilter) 1.0f else 0.0f)
        shader.setFloatUniform("screenWidth", w)
        shader.setFloatUniform("screenHeight", h)
        shader.setFloatUniform("time", t)

        AnimalVisionShader.applyProfile(shader, currentAnimal, density.density)

        val effect = RenderEffect.createRuntimeShaderEffect(shader, "inputBuffer")
        pv.setRenderEffect(effect)
    }

    // High-frequency frame animation when the selected profile uses time (Drunk Human)
    LaunchedEffect(animal.id, previewViewRef) {
        val pv = previewViewRef ?: return@LaunchedEffect
        if (animal.id == "drunk_human") {
            val startNanos = System.nanoTime()
            while (true) {
                withFrameNanos { frameNanos ->
                    val t = ((frameNanos - startNanos) / 1_000_000_000.0).toFloat()
                    elapsedTimeSec = t
                    applyShaderTo(pv, t)
                }
            }
        } else {
            elapsedTimeSec = 0f
            applyShaderTo(pv, 0f)
        }
    }

    // Toggle flashlight / torch on active camera
    LaunchedEffect(isTorchEnabled, boundCamera) {
        boundCamera?.let { cam ->
            if (cam.cameraInfo.hasFlashUnit()) {
                try {
                    cam.cameraControl.enableTorch(isTorchEnabled)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    // Unbind camera provider when leaving composition to prevent resource leak
    DisposableEffect(lifecycleOwner) {
        onDispose {
            try {
                cameraProviderFuture.get()?.unbindAll()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            boundCamera = null
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { containerSize = it }
    ) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    previewViewRef = this

                    addOnLayoutChangeListener { _, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
                        val w = (right - left).toFloat().coerceAtLeast(1f)
                        val h = (bottom - top).toFloat().coerceAtLeast(1f)
                        val oldW = (oldRight - oldLeft).toFloat()
                        val oldH = (oldBottom - oldTop).toFloat()
                        if (w > 1f && h > 1f && (w != oldW || h != oldH)) {
                            applyShaderTo(this, elapsedTimeSec)
                        }
                    }

                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val cameraSelector = selectBestCamera(cameraProvider.availableCameraInfos)

                        val matchingInfos = cameraSelector.filter(cameraProvider.availableCameraInfos)
                        val selectedCameraInfo = matchingInfos.firstOrNull()
                        val hlgSupported = if (selectedCameraInfo != null) {
                            val candidates = setOf(
                                androidx.camera.core.DynamicRange.SDR,
                                androidx.camera.core.DynamicRange.HLG_10_BIT
                            )
                            selectedCameraInfo.querySupportedDynamicRanges(candidates)
                                .contains(androidx.camera.core.DynamicRange.HLG_10_BIT)
                        } else false

                        val previewBuilder = Preview.Builder()
                        if (hlgSupported) {
                            previewBuilder.setDynamicRange(androidx.camera.core.DynamicRange.HLG_10_BIT)
                        }

                        val preview = previewBuilder.build().also {
                            it.setSurfaceProvider(surfaceProvider)
                        }
                        try {
                            cameraProvider.unbindAll()
                            val camera = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview
                            )
                            boundCamera = camera
                            onTorchAvailable(camera.cameraInfo.hasFlashUnit())

                            val zoomStateLiveData = camera.cameraInfo.zoomState
                            zoomStateLiveData.observe(lifecycleOwner, object : androidx.lifecycle.Observer<androidx.camera.core.ZoomState> {
                                override fun onChanged(value: androidx.camera.core.ZoomState) {
                                    val minZoom = value.minZoomRatio
                                    if (minZoom < 1.0f) {
                                        camera.cameraControl.setZoomRatio(minZoom)
                                    }
                                    zoomStateLiveData.removeObserver(this)
                                }
                            })

                            isCameraReady = true
                            post {
                                applyShaderTo(this, elapsedTimeSec)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { previewView ->
                previewViewRef = previewView
                applyShaderTo(previewView, elapsedTimeSec)
            }
        )

        // Interactive Split Slider Overlay
        if (isSplitEnabled && containerSize.width > 0) {
            val dividerX = (containerSize.width * splitPosition).coerceIn(0f, containerSize.width.toFloat())

            // Floating Header Badges
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 60.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Human Badge (Left)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = stringResource(R.string.human_label).uppercase(),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Animal Badge (Right)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(animal.primaryColor.copy(alpha = 0.85f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${stringResource(animal.nameRes)} (${animal.visualAcuity})".uppercase(),
                        color = Color.Black,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Draggable Divider Line & Handle (Touch bounded to handle area, relative dragging)
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                // Vertical Line
                Box(
                    modifier = Modifier
                        .offset {
                            val lineWidthHalfPx = with(density) { 1.5.dp.toPx() }
                            IntOffset(x = (dividerX - lineWidthHalfPx).roundToInt(), y = 0)
                        }
                        .fillMaxHeight()
                        .width(3.dp)
                        .background(Color.White.copy(alpha = 0.85f))
                )

                // Draggable Touch Strip and Center Handle
                val handleTouchWidthPx = with(density) { 64.dp.toPx() }
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset {
                            val halfTouchPx = handleTouchWidthPx / 2f
                            IntOffset(x = (dividerX - halfTouchPx).roundToInt(), y = 0)
                        }
                        .width(64.dp)
                        .fillMaxHeight()
                        .pointerInput(containerSize.width) {
                            detectHorizontalDragGestures { change, dragAmount ->
                                change.consume()
                                if (containerSize.width > 0) {
                                    val deltaFraction = dragAmount / containerSize.width.toFloat()
                                    onSplitPositionChange((splitPosition + deltaFraction).coerceIn(0.05f, 0.95f))
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(animal.primaryColor)
                            .border(2.dp, Color.White.copy(alpha = 0.85f), CircleShape)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(16.dp)) {
                            val w = size.width
                            val h = size.height
                            drawLine(Color.Black, Offset(w * 0.3f, h * 0.5f), Offset(w * 0.7f, h * 0.5f), strokeWidth = 3f)
                            drawLine(Color.Black, Offset(w * 0.3f, h * 0.5f), Offset(w * 0.45f, h * 0.25f), strokeWidth = 3f)
                            drawLine(Color.Black, Offset(w * 0.3f, h * 0.5f), Offset(w * 0.45f, h * 0.75f), strokeWidth = 3f)
                            drawLine(Color.Black, Offset(w * 0.7f, h * 0.5f), Offset(w * 0.55f, h * 0.25f), strokeWidth = 3f)
                            drawLine(Color.Black, Offset(w * 0.7f, h * 0.5f), Offset(w * 0.55f, h * 0.75f), strokeWidth = 3f)
                        }
                    }
                }
            }
        }

        if (!isCameraReady) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CameraFocusLoader(primaryColor = animal.primaryColor)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.calibrating_vision),
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@ExperimentalCamera2Interop
private fun selectBestCamera(cameraInfos: List<androidx.camera.core.CameraInfo>): CameraSelector {
    val backCameras = cameraInfos.filter {
        it.lensFacing == CameraSelector.LENS_FACING_BACK
    }

    if (backCameras.isEmpty()) return CameraSelector.DEFAULT_BACK_CAMERA

    val bestInfo = backCameras.minByOrNull { info ->
        val camera2Info = Camera2CameraInfo.from(info)
        val focalLengths = camera2Info.getCameraCharacteristic(
            CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS
        )
        val sensorSize = camera2Info.getCameraCharacteristic(
            CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE
        )

        val hFovFactor = if (focalLengths != null && sensorSize != null && focalLengths.isNotEmpty()) {
            sensorSize.width / focalLengths[0]
        } else 0f

        -hFovFactor
    }

    return bestInfo?.cameraSelector ?: CameraSelector.DEFAULT_BACK_CAMERA
}
