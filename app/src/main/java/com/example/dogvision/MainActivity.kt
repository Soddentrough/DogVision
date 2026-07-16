package com.example.dogvision

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.dogvision.ui.CameraPreview
import com.example.dogvision.ui.EyeDiagram
import com.example.dogvision.ui.WavelengthComparison

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) { // Force Dark Theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black // Pure black background
                ) {
                    DogVisionApp()
                }
            }
        }
    }
}

@Composable
fun DogVisionApp() {
    var showCamera by remember { mutableStateOf(false) }
    var hasCameraPermission by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (isGranted) showCamera = true
    }

    LaunchedEffect(Unit) {
        hasCameraPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    val startAction = {
        if (!showCamera) {
            if (hasCameraPermission) showCamera = true
            else launcher.launch(Manifest.permission.CAMERA)
        } else {
            showCamera = false
        }
    }

    Crossfade(
        targetState = showCamera && hasCameraPermission,
        label = "ScreenTransition",
        modifier = Modifier.fillMaxSize()
    ) { isCameraActive ->
        if (isCameraActive) {
            var isColorFilterEnabled by remember { mutableStateOf(true) }
            var isBlurEnabled by remember { mutableStateOf(true) }

            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                // Clicking the background closes the camera preview
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { showCamera = false }
                ) {
                    CameraPreview(
                        isColorFilterEnabled = isColorFilterEnabled,
                        isBlurEnabled = isBlurEnabled,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                // Subtle transparent floating buttons at the bottom center
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 80.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {} // Consume click
                        ),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EmulationToggleButton(
                        label = stringResource(R.string.lens),
                        isActive = isBlurEnabled,
                        onClick = { isBlurEnabled = !isBlurEnabled }
                    )
                    EmulationToggleButton(
                        label = stringResource(R.string.receptors),
                        isActive = isColorFilterEnabled,
                        onClick = { isColorFilterEnabled = !isColorFilterEnabled }
                    )
                }

                // Small subtle exit text
                Text(
                    text = stringResource(R.string.tap_background_to_return),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 36.dp),
                    color = Color.White.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        } else {
            InfoView(onStartClicked = { startAction() })
        }
    }
}

@Composable
fun EmulationToggleButton(label: String, isActive: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (isActive) Color(0xFFFFD700).copy(alpha = 0.85f)
                else Color.Black.copy(alpha = 0.5f)
            )
            .border(
                1.dp,
                if (isActive) Color(0xFFFFD700) else Color.White.copy(alpha = 0.3f),
                RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label.uppercase(),
            color = if (isActive) Color.Black else Color.White,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun InfoView(onStartClicked: () -> Unit) {
    var selectedTab by remember { mutableStateOf(0) }
    
    val infiniteTransition = rememberInfiniteTransition(label = "dog_float")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dog_offset"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(8.dp))
            Image(
                painter = painterResource(id = R.drawable.ic_dog_cute),
                contentDescription = "Cute Dog Logo",
                modifier = Modifier
                    .size(110.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .offset(y = offsetY.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Black),
                color = Color.White
            )
            Text(
                text = stringResource(R.string.subtitle),
                style = MaterialTheme.typography.titleMedium,
                color = Color.Gray
            )
        }

        // Custom Capsule Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E1E1E), RoundedCornerShape(16.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selectedTab == 0) Color(0xFFFFD700) else Color.Transparent)
                    .clickable { selectedTab = 0 }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.tab_sensitivity),
                    color = if (selectedTab == 0) Color.Black else Color.Gray,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selectedTab == 1) Color(0xFFFFD700) else Color.Transparent)
                    .clickable { selectedTab = 1 }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.tab_anatomy),
                    color = if (selectedTab == 1) Color.Black else Color.Gray,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Content Area based on Selected Tab
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Crossfade(targetState = selectedTab, label = "TabContentTransition") { tab ->
                when (tab) {
                    0 -> {
                        WavelengthComparison(modifier = Modifier.fillMaxWidth())
                    }
                    1 -> {
                        EyeDiagram(modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }

        Button(
            onClick = { onStartClicked() },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700))
        ) {
            Text(
                text = stringResource(R.string.start_emulation),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }
    }
}

