package com.example.dogvision

import android.graphics.RuntimeShader
import com.example.dogvision.shader.AnimalVisionShader

/**
 * Backward compatibility alias for DogVisionShader, delegating to [AnimalVisionShader].
 */
object DogVisionShader {
    val SHADER_SRC: String get() = AnimalVisionShader.SHADER_SRC

    fun createShader(): RuntimeShader {
        return AnimalVisionShader.createShader()
    }
}
