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
