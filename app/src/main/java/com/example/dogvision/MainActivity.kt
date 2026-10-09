package com.example.dogvision

import android.Manifest
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import com.example.dogvision.model.AnimalRegistry
import com.example.dogvision.model.AnimalVisionProfile
import androidx.activity.viewModels
import com.example.dogvision.ui.AnimalSelector
import com.example.dogvision.ui.AnimalVisionViewModel
import com.example.dogvision.ui.CameraPreview
import com.example.dogvision.ui.WavelengthComparison
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay

@ExperimentalCamera2Interop
class MainActivity : AppCompatActivity() {
    private val viewModel: AnimalVisionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable Wide Color Gamut (Display P3) mode to receive wide-range, rich-gamut sensor data
        window.colorMode = ActivityInfo.COLOR_MODE_WIDE_COLOR_GAMUT

        // Pre-initialize SoundManager with SoundPool for zero-latency audio playback
        SoundManager.getInstance(applicationContext)

        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    AnimalVisionApp(viewModel = viewModel)
                }
            }
        }
    }
}

@ExperimentalCamera2Interop
@Composable
fun AnimalVisionApp(viewModel: AnimalVisionViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current

    val selectedAnimal = uiState.selectedAnimal

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.setCameraPermission(isGranted)
        if (isGranted) viewModel.setCameraActive(true)
    }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        viewModel.setCameraPermission(granted)
    }

    val startAction = {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        if (!uiState.isCameraActive) {
            if (uiState.hasCameraPermission) viewModel.setCameraActive(true)
            else launcher.launch(Manifest.permission.CAMERA)
        } else {
            viewModel.setCameraActive(false)
        }
    }

    Crossfade(
        targetState = uiState.isCameraActive && uiState.hasCameraPermission,
        label = "ScreenTransition",
        modifier = Modifier.fillMaxSize()
    ) { isCameraActive ->
        if (isCameraActive) {
            val lifecycleOwner = LocalLifecycleOwner.current
            val soundManager = remember { SoundManager.getInstance(context) }
            val playAnimalAudio = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                soundManager.playAnimalSound(selectedAnimal.id)
            }

            // Periodic spontaneous burp sound for Drunk Human easter egg (pauses when backgrounded/screen locked)
            LaunchedEffect(selectedAnimal.id, lifecycleOwner) {
                if (selectedAnimal.id == "drunk_human") {
                    lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                        while (true) {
                            val delayMillis = 18_000L + (Math.random() * 12_000L).toLong()
                            delay(delayMillis)
                            soundManager.playBurpSound()
                        }
                    }
                }
            }

            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                // Background tap triggers species audio & haptic pulse
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { playAnimalAudio() }
                ) {
                    CameraPreview(
                        animal = selectedAnimal,
                        isColorFilterEnabled = uiState.isColorFilterEnabled,
                        isBlurEnabled = uiState.isBlurEnabled,
                        isSplitEnabled = uiState.isSplitEnabled,
                        splitPosition = uiState.splitPosition,
                        onSplitPositionChange = { viewModel.updateSplitPosition(it) },
                        isTorchEnabled = uiState.isTorchEnabled,
                        onTorchAvailable = { viewModel.setTorchAvailable(it) },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Top Bar: Go Back button on left, Animal Selector on right
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Go Back Icon Button
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.65f))
                            .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                            .clickable {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.setCameraActive(false)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(20.dp)) {
                            val strokeWidth = 2.5.dp.toPx()
                            val midY = size.height / 2f
                            val startX = size.width * 0.25f
                            val endX = size.width * 0.75f
                            val arrowHead = size.width * 0.28f

                            drawLine(
                                color = Color.White,
                                start = Offset(startX, midY),
                                end = Offset(endX, midY),
                                strokeWidth = strokeWidth,
                                cap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                            drawLine(
                                color = Color.White,
                                start = Offset(startX, midY),
                                end = Offset(startX + arrowHead, midY - arrowHead),
                                strokeWidth = strokeWidth,
                                cap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                            drawLine(
                                color = Color.White,
                                start = Offset(startX, midY),
                                end = Offset(startX + arrowHead, midY + arrowHead),
                                strokeWidth = strokeWidth,
                                cap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                        }
                    }

                    AnimalSelector(
                        selectedAnimal = selectedAnimal,
                        onAnimalSelected = { viewModel.selectAnimal(it) },
                        compact = true
                    )
                }
            }
        } else {
            InfoView(
                selectedAnimal = selectedAnimal,
                onAnimalSelected = { viewModel.selectAnimal(it) },
                onStartClicked = { startAction() }
            )
        }
    }
}

// Backward compatibility alias for DogVisionApp
@ExperimentalCamera2Interop
@Composable
fun DogVisionApp(viewModel: AnimalVisionViewModel = remember { AnimalVisionViewModel() }) {
    AnimalVisionApp(viewModel = viewModel)
}

@Composable
fun EmulationToggleButton(
    label: String,
    isActive: Boolean,
    activeColor: Color = Color(0xFFFFD700),
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isActive) activeColor.copy(alpha = 0.9f) else Color.Black.copy(alpha = 0.55f),
        label = "btn_bg"
    )
    val textColor by animateColorAsState(
        targetValue = if (isActive) Color.Black else Color.White,
        label = "btn_text"
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .border(
                1.dp,
                if (isActive) activeColor else Color.White.copy(alpha = 0.3f),
                RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label.uppercase(),
            color = textColor,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun InfoView(
    selectedAnimal: AnimalVisionProfile,
    onAnimalSelected: (AnimalVisionProfile) -> Unit,
    onStartClicked: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Header: App Title & Language Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                    color = Color.White
                )
                Text(
                    text = stringResource(R.string.subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            LanguageToggle()
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Animal Selector Tabs
        AnimalSelector(
            selectedAnimal = selectedAnimal,
            onAnimalSelected = onAnimalSelected,
            compact = false
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Main Scrollable Content Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(scrollState)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Active Animal Header & Subtitle
                Text(
                    text = stringResource(selectedAnimal.nameRes),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = selectedAnimal.primaryColor
                )
                Text(
                    text = selectedAnimal.scientificName,
                    style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                    color = Color.LightGray
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(selectedAnimal.subtitleRes),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Biological Spec Badges Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    SpecBadge(
                        label = stringResource(R.string.spec_acuity),
                        value = selectedAnimal.visualAcuity,
                        highlightColor = selectedAnimal.primaryColor
                    )
                    SpecBadge(
                        label = stringResource(R.string.spec_fov),
                        value = "${selectedAnimal.fieldOfViewDeg}°",
                        highlightColor = selectedAnimal.primaryColor
                    )
                    SpecBadge(
                        label = stringResource(R.string.spec_tapetum),
                        value = selectedAnimal.tapetumBoostMultiplier,
                        highlightColor = selectedAnimal.primaryColor
                    )
                    SpecBadge(
                        label = stringResource(R.string.spec_uv),
                        value = if (selectedAnimal.hasUvVision) "YES" else "NO",
                        highlightColor = if (selectedAnimal.hasUvVision) Color(0xFFE040FB) else Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Description
                Text(
                    text = stringResource(selectedAnimal.descriptionRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                // Key Facts List
                if (selectedAnimal.keyFacts.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        selectedAnimal.keyFacts.forEach { factRes ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(text = "•", color = selectedAnimal.primaryColor, fontWeight = FontWeight.Bold)
                                Text(
                                    text = stringResource(factRes),
                                    color = Color.White.copy(alpha = 0.85f),
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Wavelength & Eye Anatomy Comparison
                WavelengthComparison(
                    animal = selectedAnimal,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Start Emulation Button
        Button(
            onClick = { onStartClicked() },
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = selectedAnimal.primaryColor)
        ) {
            Text(
                text = "${stringResource(R.string.start_emulation)} (${stringResource(selectedAnimal.nameRes)})".uppercase(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = Color.Black
            )
        }
    }
}

@Composable
fun SpecBadge(label: String, value: String, highlightColor: Color) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.07f))
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = highlightColor
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
            color = Color.Gray
        )
    }
}

@Composable
fun LanguageToggle() {
    val haptics = LocalHapticFeedback.current
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
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags("en")
                AppCompatDelegate.setApplicationLocales(appLocale)
            }
        )
        LanguageButton(
            text = stringResource(R.string.lang_jp),
            isSelected = isJp,
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.Black else Color.Gray,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelSmall
        )
    }
}
