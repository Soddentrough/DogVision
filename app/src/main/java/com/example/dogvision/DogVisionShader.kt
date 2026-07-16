package com.example.dogvision

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi

object DogVisionShader {
    const val SHADER_SRC = """
        uniform shader inputBuffer;
        
        float3 srgbToLinear(float3 srgb) {
            return pow(srgb, float3(2.2));
        }

        float3 linearToSrgb(float3 lin) {
            return pow(lin, float3(1.0 / 2.2));
        }

        half4 main(float2 fragCoord) {
            // Note: inputBuffer has already been blurred via hardware-accelerated RenderEffect
            half4 color = inputBuffer.eval(fragCoord);
            
            float3 lin = srgbToLinear(color.rgb);
            
            // Calculate human linear luminance
            float luminance = dot(lin, float3(0.2126, 0.7152, 0.0722));
            
            // 1. Simulate Tapetum Lucidum & Rod Sensitivity (Luminance Boost in low light)
            // Dogs see about 4-5x better in the dark. We apply an exponential exposure boost to dark pixels.
            float boost = 2.0 * exp(-5.0 * luminance);
            float3 lin_boosted = clamp(lin * (1.0 + boost), 0.0, 1.0);
            
            // 2. Perform Deuteranopia simulation on the boosted colors
            float3 lin_prime;
            lin_prime.r = dot(float3(0.84095,  2.92281, -2.76374), lin_boosted);
            lin_prime.g = dot(float3(0.03561,  0.13067,  0.84045), lin_boosted);
            lin_prime.b = dot(float3(0.00579,  0.02723,  0.96602), lin_boosted);
            lin_prime = clamp(lin_prime, 0.0, 1.0);
            
            // 3. Simulate Scotopic (rod-only) transition in low light
            // In very low light, cones stop responding and vision fades to monochrome.
            float boosted_lum = dot(lin_boosted, float3(0.2126, 0.7152, 0.0722));
            float color_weight = smoothstep(0.01, 0.15, luminance); // Fades out color in low light
            float3 final_lin = mix(float3(boosted_lum), lin_prime, color_weight);
            
            float3 srgb_prime = linearToSrgb(final_lin);
            
            return half4(srgb_prime, color.a);
        }
    """

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun createShader(): RuntimeShader {
        return RuntimeShader(SHADER_SRC)
    }
}
