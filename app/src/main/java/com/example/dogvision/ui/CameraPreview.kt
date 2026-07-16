package com.example.dogvision.ui

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
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
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.dogvision.DogVisionShader
import com.example.dogvision.R

@Composable
fun CameraFocusLoader(modifier: Modifier = Modifier) {
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
        val color = Color(0xFFFFD700).copy(alpha = glowAlpha) // Theme yellow
        
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
        drawLine(color, Offset(width - o, height - o), Offset(width - o, height - o - len), strokeWidth = stroke)
        
        // Central dot (dog's blue vision receptor representation)
        drawCircle(
            color = Color(0xFF00BCD4).copy(alpha = glowAlpha),
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

@androidx.camera.camera2.interop.ExperimentalCamera2Interop
@Composable
fun CameraPreview(
    isColorFilterEnabled: Boolean,
    isBlurEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var isCameraReady by remember { mutableStateOf(false) }
    
    val shader = remember { 
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            DogVisionShader.createShader()
        } else null
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    
                    // Setup and bind camera provider listener once during creation
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        
                        // Strategy: Find the lens with the widest Field of View (shortest focal length)
                        // This best approximates a dog's ~240 degree panoramic vision.
                        val cameraSelector = selectBestCamera(cameraProvider.availableCameraInfos)
                        
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(surfaceProvider)
                        }
                        try {
                            cameraProvider.unbindAll()
                            val camera = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview
                            )
                            
                            // Observe zoom state to retrieve the minimum zoom ratio (e.g., 0.5x)
                            // and force the logical camera to switch to the physical ultra-wide lens
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
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { previewView ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && shader != null) {
                    // Compute RenderEffect dynamically based on active toggles
                    val blurEffect = if (isBlurEnabled) {
                        RenderEffect.createBlurEffect(6f, 6f, Shader.TileMode.CLAMP)
                    } else null

                    val shaderEffect = if (isColorFilterEnabled) {
                        RenderEffect.createRuntimeShaderEffect(shader, "inputBuffer")
                    } else null

                    val combinedEffect = when {
                        shaderEffect != null && blurEffect != null -> {
                            RenderEffect.createChainEffect(shaderEffect, blurEffect)
                        }
                        shaderEffect != null -> shaderEffect
                        blurEffect != null -> blurEffect
                        else -> null
                    }
                    previewView.setRenderEffect(combinedEffect)
                }
            }
        )

        if (!isCameraReady) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CameraFocusLoader()
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

@androidx.camera.camera2.interop.ExperimentalCamera2Interop
private fun selectBestCamera(cameraInfos: List<androidx.camera.core.CameraInfo>): CameraSelector {
    // Look for back-facing cameras
    val backCameras = cameraInfos.filter { 
        it.lensFacing == CameraSelector.LENS_FACING_BACK 
    }
    
    if (backCameras.isEmpty()) return CameraSelector.DEFAULT_BACK_CAMERA

    // Strategy: We want the absolute widest field of view.
    // Modern devices often have a "Logical" camera (0) and multiple physical cameras (0, 1, 2...).
    // CameraX usually presents these as separate CameraInfos.
    
    val bestInfo = backCameras.minByOrNull { info ->
        val camera2Info = Camera2CameraInfo.from(info)
        val focalLengths = camera2Info.getCameraCharacteristic(
            CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS
        )
        val sensorSize = camera2Info.getCameraCharacteristic(
            CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE
        )
        
        // FOV = 2 * atan(sensorSize / (2 * focalLength))
        // Shorter focal length always means wider FOV for the same sensor size.
        // However, ultra-wide sensors are often smaller.
        // Let's use a "Horizontal FOV" approximation: sensorWidth / focalLength
        val hFovFactor = if (focalLengths != null && sensorSize != null && focalLengths.isNotEmpty()) {
            sensorSize.width / focalLengths[0]
        } else 0f
        
        android.util.Log.d("DogVisionCamera", "Camera: ${info.cameraSelector}, Focal: ${focalLengths?.firstOrNull()}, SensorW: ${sensorSize?.width}, FovFactor: $hFovFactor")
        
        // We want to MAXIMIZE hFovFactor, so we MINIMIZE negative hFovFactor
        -hFovFactor
    }

    return bestInfo?.cameraSelector ?: CameraSelector.DEFAULT_BACK_CAMERA
}
