package com.example.dogvision.ui

import androidx.lifecycle.ViewModel
import com.example.dogvision.model.AnimalRegistry
import com.example.dogvision.model.AnimalVisionProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class AnimalVisionUiState(
    val selectedAnimal: AnimalVisionProfile = AnimalRegistry.DOG,
    val isCameraActive: Boolean = false,
    val hasCameraPermission: Boolean = false,
    val isColorFilterEnabled: Boolean = true,
    val isBlurEnabled: Boolean = true,
    val isSplitEnabled: Boolean = false,
    val splitPosition: Float = 0.5f,
    val isTorchEnabled: Boolean = false,
    val isTorchAvailable: Boolean = false
)

class AnimalVisionViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AnimalVisionUiState())
    val uiState: StateFlow<AnimalVisionUiState> = _uiState.asStateFlow()

    fun selectAnimal(profile: AnimalVisionProfile) {
        _uiState.update { it.copy(selectedAnimal = profile) }
    }

    fun selectAnimalById(id: String) {
        val profile = AnimalRegistry.getById(id)
        selectAnimal(profile)
    }

    fun setCameraActive(active: Boolean) {
        _uiState.update {
            it.copy(
                isCameraActive = active,
                // Turn off torch when camera is closed
                isTorchEnabled = if (!active) false else it.isTorchEnabled
            )
        }
    }

    fun setCameraPermission(granted: Boolean) {
        _uiState.update { it.copy(hasCameraPermission = granted) }
    }

    fun toggleColorFilter() {
        _uiState.update { it.copy(isColorFilterEnabled = !it.isColorFilterEnabled) }
    }

    fun toggleBlur() {
        _uiState.update { it.copy(isBlurEnabled = !it.isBlurEnabled) }
    }

    fun toggleSplitMode() {
        _uiState.update { it.copy(isSplitEnabled = !it.isSplitEnabled) }
    }

    fun updateSplitPosition(fraction: Float) {
        _uiState.update { it.copy(splitPosition = fraction.coerceIn(0.05f, 0.95f)) }
    }

    fun toggleTorch() {
        _uiState.update {
            if (it.isTorchAvailable) it.copy(isTorchEnabled = !it.isTorchEnabled)
            else it
        }
    }

    fun setTorchAvailable(available: Boolean) {
        _uiState.update {
            it.copy(
                isTorchAvailable = available,
                isTorchEnabled = if (!available) false else it.isTorchEnabled
            )
        }
    }
}
