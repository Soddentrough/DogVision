package com.example.dogvision

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi

object DogVisionShader {
    const val SHADER_SRC = """
        uniform shader inputBuffer;
        
        float3 srgbToLinear(float3 srgb) {
            return mix(srgb / 12.92, pow((srgb + 0.055) / 1.055, float3(2.4)), step(0.04045, srgb));
        }

        float3 linearToSrgb(float3 lin) {
            return mix(lin * 12.92, 1.055 * pow(lin, float3(1.0 / 2.4)) - 0.055, step(0.0031308, lin));
        }

        half4 main(float2 fragCoord) {
            // Note: inputBuffer has already been blurred via hardware-accelerated RenderEffect
            half4 color = inputBuffer.eval(fragCoord);
            
            float3 lin = srgbToLinear(color.rgb);
            
            // Precomputed Smith-Pokorny Deuteranopia Simulation Matrix:
            // LMS_to_RGB * Deuteranopia_Matrix * RGB_to_LMS
            float3 lin_prime;
            lin_prime.r = dot(float3(0.84095,  2.92281, -2.76374), lin);
            lin_prime.g = dot(float3(0.03561,  0.13067,  0.84045), lin);
            lin_prime.b = dot(float3(0.00579,  0.02723,  0.96602), lin);
            
            float3 srgb_prime = linearToSrgb(clamp(lin_prime, 0.0, 1.0));
            
            return half4(srgb_prime, color.a);
        }
    """

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun createShader(): RuntimeShader {
        return RuntimeShader(SHADER_SRC)
    }
}
