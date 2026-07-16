package com.example.dogvision.ui

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.dogvision.DogVisionShader

@Composable
fun CameraPreview(
    isColorFilterEnabled: Boolean,
    isBlurEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    
    val shader = remember { 
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            DogVisionShader.createShader()
        } else null
    }

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
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(ctx))
            }
        },
        modifier = modifier.fillMaxSize(),
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
}
