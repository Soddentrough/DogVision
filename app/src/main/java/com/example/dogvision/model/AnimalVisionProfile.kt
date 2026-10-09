package com.example.dogvision.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color

enum class PupilType {
    CIRCULAR,
    HORIZONTAL_SLIT,
    VERTICAL_SLIT,
    COMPOUND_HEXAGONAL,
    TUBULAR_DOUBLE_FOVEA,
}

/**
 * A photoreceptor class. [peakWavelengthNm] is the spectral sensitivity peak and
 * [sigma] the bandwidth of the (schematic) Gaussian used for the UI plot only —
 * real cone fundamentals are asymmetric (Stockman & Sharpe 2000).
 */
data class ConeData(
    val peakWavelengthNm: Float,
    val sigma: Float,
    val color: Color,
    val label: String,
    val isUv: Boolean = false,
)

data class AnimalVisionProfile(
    val id: String,
    @param:StringRes val nameRes: Int,
    @param:StringRes val subtitleRes: Int,
    @param:StringRes val descriptionRes: Int,
    val scientificName: String,
    val visualAcuity: String,
    val fieldOfViewDeg: Int,
    val pupilType: PupilType,
    val hasTapetum: Boolean,
    val tapetumBoostMultiplier: String,
    val hasUvVision: Boolean,
    val cones: List<ConeData>,
    val spectrumBarColors: List<Color>,
    val shaderTypeIndex: Int,
    /** Tapetum lucidum low-light collection gain fed to the shader (0 = no tapetum). */
    val tapetumFactor: Float,
    /** Synthetic UV reconstruction gain (0 = species not UV-sensitive). */
    val uvSensitivityGain: Float,
    /** Weight of achromatic rod pathway takeover in dim light (0..1). */
    val rodDominance: Float = 0.5f,
    /** Cone-contrast scaling around luma; >1 models oil-droplet sharpening, <1 pastel/rod-saturated vision. */
    val chromaticStrength: Float = 1.0f,
    val desaturationFactor: Float = 1.0f,
    val primaryColor: Color = Color(0xFFFFD700),
    @param:DrawableRes val iconRes: Int? = null,
    val emoji: String = "🐾",
    @param:StringRes val keyFacts: List<Int> = emptyList(),
)
