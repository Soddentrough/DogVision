package com.example.dogvision.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color

enum class PupilType {
    CIRCULAR,
    HORIZONTAL_SLIT,
    VERTICAL_SLIT,
    COMPOUND_HEXAGONAL,
    TUBULAR_DOUBLE_FOVEA
}

data class ConeData(
    val peakWavelengthNm: Float,
    val sigma: Float,
    val color: Color,
    val label: String,
    val isUv: Boolean = false
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
    val tapetumFactor: Float,
    val uvSensitivityGain: Float,
    val desaturationFactor: Float,
    val primaryColor: Color = Color(0xFFFFD700),
    @param:DrawableRes val iconRes: Int? = null,
    val keyFacts: List<Int> = emptyList()
)
