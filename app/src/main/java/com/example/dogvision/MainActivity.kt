package com.example.dogvision

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import androidx.core.content.ContextCompat
import com.example.dogvision.ui.CameraPreview
import com.example.dogvision.ui.EyeDiagram
import com.example.dogvision.ui.WavelengthComparison

class MainActivity : AppCompatActivity() {
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
    var showCamera by rememberSaveable { mutableStateOf(false) }
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
            var isColorFilterEnabled by rememberSaveable { mutableStateOf(true) }
            var isBlurEnabled by rememberSaveable { mutableStateOf(true) }

            val playBark = {
                val barks = listOf(R.raw.dog_bark_1, R.raw.dog_bark_2, R.raw.dog_bark_3)
                val randomBark = barks.random()
                try {
                    val mediaPlayer = MediaPlayer.create(context, randomBark)
                    mediaPlayer?.let { mp ->
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                            val pitch = (0.85f + Math.random() * 0.5f).toFloat()
                            mp.playbackParams = mp.playbackParams.setPitch(pitch)
                        }
                        mp.setOnCompletionListener { activeMp ->
                            activeMp.release()
                        }
                        mp.start()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                // Clicking the background plays a random dog bark sound
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { playBark() }
                ) {
                    CameraPreview(
                        isColorFilterEnabled = isColorFilterEnabled,
                        isBlurEnabled = isBlurEnabled,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                // Subtle transparent floating controls column at the bottom center
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 40.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {} // Intercept and consume clicks to avoid triggering barks
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
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

                    // Return Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(alpha = 0.15f))
                            .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                            .clickable { showCamera = false }
                            .padding(horizontal = 32.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.button_return).uppercase(),
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
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
        // Language Toggle at Top Right
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopEnd) {
            LanguageToggle()
        }

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
                color = Color(0xFFFFD700), // Highlight in yellow
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.description),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }

        // Content Area - Combined comparison
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            WavelengthComparison(modifier = Modifier.fillMaxWidth())
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


@Composable
fun LanguageToggle() {
    val currentLocale = AppCompatDelegate.getApplicationLocales().toLanguageTags()
    val isJp = currentLocale.contains("ja")

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E1E1E))
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        LanguageButton(
            text = stringResource(R.string.lang_en),
            isSelected = !isJp,
            onClick = {
                val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags("en")
                AppCompatDelegate.setApplicationLocales(appLocale)
            }
        )
        LanguageButton(
            text = stringResource(R.string.lang_jp),
            isSelected = isJp,
            onClick = {
                val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags("ja")
                AppCompatDelegate.setApplicationLocales(appLocale)
            }
        )
    }
}

@Composable
fun LanguageButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) Color(0xFFFFD700) else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.Black else Color.Gray,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelMedium
        )
    }
}
