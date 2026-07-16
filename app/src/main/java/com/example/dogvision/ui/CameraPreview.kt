package com.example.dogvision.ui

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
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
fun MergingConesLoader(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "loader")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 750, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Canvas(modifier = modifier.size(80.dp).graphicsLayer(rotationZ = angle, scaleX = scale, scaleY = scale)) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.width * 0.3f
        
        val angleRad1 = 0.0
        val angleRad2 = 2 * Math.PI / 3
        val angleRad3 = 4 * Math.PI / 3

        val dot1 = Offset(
            center.x + radius * kotlin.math.cos(angleRad1).toFloat(),
            center.y + radius * kotlin.math.sin(angleRad1).toFloat()
        )
        val dot2 = Offset(
            center.x + radius * kotlin.math.cos(angleRad2).toFloat(),
            center.y + radius * kotlin.math.sin(angleRad2).toFloat()
        )
        val dot3 = Offset(
            center.x + radius * kotlin.math.cos(angleRad3).toFloat(),
            center.y + radius * kotlin.math.sin(angleRad3).toFloat()
        )

        drawCircle(Color(0xFFF44336), radius = 10f, center = dot1) // Red
        drawCircle(Color(0xFF4CAF50), radius = 10f, center = dot2) // Green
        drawCircle(Color(0xFF3F51B5), radius = 10f, center = dot3) // Blue
    }
}

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
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(surfaceProvider)
                        }
                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview
                            )
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
                    MergingConesLoader()
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
