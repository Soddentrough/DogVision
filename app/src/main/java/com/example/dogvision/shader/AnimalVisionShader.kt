package com.example.dogvision.shader

import android.graphics.RuntimeShader
import com.example.dogvision.model.AnimalVisionProfile

object AnimalVisionShader {
    const val SHADER_SRC = """
        uniform shader inputBuffer;
        uniform float splitPos;          // 0.0 to 1.0 (horizontal partition)
        uniform float isSplitMode;       // 1.0 = enabled, 0.0 = disabled
        uniform float isBlurEnabled;     // 1.0 = enabled, 0.0 = disabled
        uniform float isColorFilter;     // 1.0 = enabled, 0.0 = disabled
        uniform float screenWidth;
        uniform float screenHeight;
        uniform float animalType;        // 0=Dog, 1=Deer, 2=Cat, 3=Bird, 4=Bee, 5=Snake
        uniform float tapetumBoost;      // Gain factor for low-light tapetum retroreflection
        uniform float uvGain;            // Near-UV spectral excitation gain
        uniform float desaturation;      // Retinal cone density desaturation weight

        float3 srgbToLinear(float3 srgb) {
            return pow(srgb, float3(2.2));
        }

        float3 linearToSrgb(float3 lin) {
            return pow(lin, float3(1.0 / 2.2));
        }

        // Isotropic blur for dog / snake
        half4 sampleIsotropicBlur(float2 fragCoord, float radius) {
            float2 o = float2(radius, radius);
            half4 c = inputBuffer.eval(fragCoord) * 0.20;
            c += (inputBuffer.eval(fragCoord + float2(o.x, 0.0)) + inputBuffer.eval(fragCoord - float2(o.x, 0.0))) * 0.15;
            c += (inputBuffer.eval(fragCoord + float2(0.0, o.y)) + inputBuffer.eval(fragCoord - float2(0.0, o.y))) * 0.15;
            c += (inputBuffer.eval(fragCoord + o) + inputBuffer.eval(fragCoord - o)) * 0.10;
            c += (inputBuffer.eval(fragCoord + float2(o.x, -o.y)) + inputBuffer.eval(fragCoord + float2(-o.x, o.y))) * 0.10;
            return c;
        }

        // Horizontal visual streak for deer: sharper along horizontal horizon, blurrier vertically
        half4 sampleDeerStreak(float2 fragCoord) {
            half4 c = inputBuffer.eval(fragCoord) * 0.30;
            c += (inputBuffer.eval(fragCoord + float2(0.0, 3.0)) + inputBuffer.eval(fragCoord - float2(0.0, 3.0))) * 0.22;
            c += (inputBuffer.eval(fragCoord + float2(0.0, 6.0)) + inputBuffer.eval(fragCoord - float2(0.0, 6.0))) * 0.13;
            return c;
        }

        // Vertical slit pupil blur for cat
        half4 sampleCatSlit(float2 fragCoord) {
            half4 c = inputBuffer.eval(fragCoord) * 0.30;
            c += (inputBuffer.eval(fragCoord + float2(3.5, 0.0)) + inputBuffer.eval(fragCoord - float2(3.5, 0.0))) * 0.22;
            c += (inputBuffer.eval(fragCoord + float2(7.0, 0.0)) + inputBuffer.eval(fragCoord - float2(7.0, 0.0))) * 0.13;
            return c;
        }

        // Bifoveal center-surround sampling for Raptor / Bird
        // Simulates the deep central fovea (fovea centralis profunda) optical negative-lens magnification (~2x)
        // surrounded by the wide panoramic peripheral field (~280-300 deg FOV)
        half4 sampleBifovealBird(float2 fragCoord, float w, float h) {
            float2 center = float2(w * 0.5, h * 0.5);
            float dist = length(fragCoord - center);
            float foveaRadius = min(w, h) * 0.36;

            if (dist < foveaRadius) {
                // Optical foveal pit negative-lens magnification (~2.0x at center, smoothly tapering)
                float foveaFactor = smoothstep(foveaRadius, 0.0, dist);
                float zoom = 1.0 + 1.15 * foveaFactor;
                float2 sampleCoord = center + (fragCoord - center) / zoom;

                // Deep foveal 20/5 ultra-acuity unsharp mask sharpening
                half4 c = inputBuffer.eval(sampleCoord);
                half4 neighbors = (inputBuffer.eval(sampleCoord + float2(1.5, 0.0)) +
                                   inputBuffer.eval(sampleCoord - float2(1.5, 0.0)) +
                                   inputBuffer.eval(sampleCoord + float2(0.0, 1.5)) +
                                   inputBuffer.eval(sampleCoord - float2(0.0, 1.5))) * 0.25;
                half4 sharpened = clamp(c + (c - neighbors) * 0.85, half4(0.0), half4(1.0));

                // Subtle feathered foveal boundary ring cue
                float ringDist = abs(dist - foveaRadius);
                float ring = smoothstep(2.5, 0.0, ringDist) * 0.35;
                return mix(sharpened, half4(0.85, 0.25, 0.95, 1.0), ring);
            } else {
                // Wide unmagnified panoramic periphery
                return inputBuffer.eval(fragCoord);
            }
        }

        // Hexagonal lattice coordinates for Bee compound eye ommatidia
        float2 hexGridSample(float2 coord, float r) {
            float2 s = float2(r * 1.7320508, r * 1.5);
            float2 p1 = floor(coord / s) * s;
            float2 p2 = floor((coord - s * 0.5) / s) * s + s * 0.5;
            float2 c1 = p1 + s * 0.5;
            float2 c2 = p2 + s * 0.5;
            return (length(coord - c1) < length(coord - c2)) ? c1 : c2;
        }

        half4 main(float2 fragCoord) {
            // Split comparison mode: keep left side as unedited human trichromatic feed
            if (isSplitMode > 0.5 && fragCoord.x < splitPos * screenWidth) {
                return inputBuffer.eval(fragCoord);
            }

            // 1. Animal Optical Acuity & Retinal Geometry Filtering
            half4 color;
            if (isBlurEnabled > 0.5) {
                if (animalType < 0.5) {
                    // Dog (0): 20/75 isotropic blur
                    color = sampleIsotropicBlur(fragCoord, 3.0);
                } else if (animalType < 1.5) {
                    // Deer (1): 20/60 horizontal visual streak
                    color = sampleDeerStreak(fragCoord);
                } else if (animalType < 2.5) {
                    // Cat (2): 20/100 vertical stenopaeic blur
                    color = sampleCatSlit(fragCoord);
                } else if (animalType < 3.5) {
                    // Bird (3): Bifoveal center-surround deep fovea magnification + wide periphery
                    color = sampleBifovealBird(fragCoord, screenWidth, screenHeight);
                } else if (animalType < 4.5) {
                    // Bee (4): Compound ommatidia hexagonal pixelation
                    float2 hexCoord = hexGridSample(fragCoord, 12.0);
                    color = inputBuffer.eval(hexCoord);
                } else {
                    // Snake (5): Pit viper blurred visual background
                    color = sampleIsotropicBlur(fragCoord, 4.0);
                }
            } else {
                color = inputBuffer.eval(fragCoord);
            }

            // If color filter is disabled, output optical sampling directly
            if (isColorFilter < 0.5) {
                return color;
            }

            float3 lin = srgbToLinear(color.rgb);
            float luminance = dot(lin, float3(0.2126, 0.7152, 0.0722));

            // 2. Tapetum Lucidum & Scotopic Boost (Active for crepuscular / nocturnal species)
            float boost = tapetumBoost * exp(-5.0 * luminance);
            float3 lin_boosted = clamp(lin * (1.0 + boost), 0.0, 1.0);
            float boosted_lum = dot(lin_boosted, float3(0.2126, 0.7152, 0.0722));

            // 3. Photoreceptor Spectral Transformation
            float3 final_lin;

            if (animalType < 0.5) {
                // === DOG: Canine Deuteranopia (435nm S, 555nm M/L) ===
                float3 lin_prime;
                lin_prime.r = dot(float3(0.84095,  2.92281, -2.76374), lin_boosted);
                lin_prime.g = dot(float3(0.03561,  0.13067,  0.84045), lin_boosted);
                lin_prime.b = dot(float3(0.00579,  0.02723,  0.96602), lin_boosted);
                lin_prime = clamp(lin_prime, 0.0, 1.0);

                float3 lin_desat = mix(float3(luminance), lin_prime, desaturation);
                float color_weight = smoothstep(0.01, 0.15, luminance);
                final_lin = mix(float3(boosted_lum), lin_desat, color_weight);

            } else if (animalType < 1.5) {
                // === DEER: S-455nm (UV Open), M-537nm (Green), Zero L-Cone ===
                // Blue/UV excitation: No UV filter in lens allows high short-wavelength transmission
                float s_cone = lin_boosted.b + uvGain * max(0.0, lin_boosted.b - lin_boosted.g * 0.5);
                // M-cone excitation: 537nm peak aligns with Green Bayer CFA; red response drops off sharply
                float m_cone = 0.85 * lin_boosted.g + 0.12 * lin_boosted.r;

                float3 deer_rgb;
                deer_rgb.r = 0.10 * s_cone + 0.88 * m_cone;
                deer_rgb.g = 0.06 * s_cone + 0.92 * m_cone;
                deer_rgb.b = 1.15 * s_cone + 0.04 * m_cone;
                deer_rgb = clamp(deer_rgb, 0.0, 1.0);

                float3 lin_desat = mix(float3(dot(deer_rgb, float3(0.2126, 0.7152, 0.0722))), deer_rgb, desaturation);
                float color_weight = smoothstep(0.008, 0.12, luminance);
                final_lin = mix(float3(boosted_lum), lin_desat, color_weight);

            } else if (animalType < 2.5) {
                // === CAT: 25:1 Rod-to-Cone Ratio, Pastel Daylight, Intense Night Tapetum ===
                float3 cat_rgb;
                cat_rgb.r = 0.65 * lin_boosted.g + 0.30 * lin_boosted.r;
                cat_rgb.g = 0.80 * lin_boosted.g + 0.18 * lin_boosted.b;
                cat_rgb.b = 0.88 * lin_boosted.b + 0.12 * lin_boosted.g;
                cat_rgb = clamp(cat_rgb, 0.0, 1.0);

                float3 lin_desat = mix(float3(luminance), cat_rgb, desaturation);
                float color_weight = smoothstep(0.015, 0.20, luminance);
                final_lin = mix(float3(boosted_lum * float3(0.85, 1.05, 0.95)), lin_desat, color_weight);

            } else if (animalType < 3.5) {
                // === BIRD / RAPTOR: Avian Tetrachromat + Oil Droplets + UV False-Color ===
                // Extract short-wavelength blue/violet differential for UV channel
                float uv_signal = max(0.0, lin_boosted.b - 0.5 * (lin_boosted.r + lin_boosted.g)) * uvGain + 0.15 * luminance;
                float3 uv_glow = float3(0.38 * uv_signal, 0.05 * uv_signal, 0.68 * uv_signal);

                // Carotenoid oil droplets eliminate spectral overlap -> expand color discrimination
                float3 mean_color = float3((lin_boosted.r + lin_boosted.g + lin_boosted.b) / 3.0);
                float3 hyper_chroma = clamp(mean_color + (lin_boosted - mean_color) * 1.35, 0.0, 1.0);
                final_lin = clamp(hyper_chroma + uv_glow, 0.0, 1.0);

            } else if (animalType < 4.5) {
                // === BEE: UV-B-G Trichromat, Red-Blind (>580nm -> Black) ===
                float uv_bee = max(0.0, lin_boosted.b - lin_boosted.g * 0.7) * uvGain + 0.20 * lin_boosted.b;

                float3 bee_rgb;
                bee_rgb.r = 0.82 * uv_bee; // Render UV nectar guides in "bee-purple"
                bee_rgb.g = 0.92 * lin_boosted.g;
                bee_rgb.b = 1.08 * lin_boosted.b + 0.30 * uv_bee;

                // Attenuate long-wavelength red
                float red_cutoff = 1.0 - smoothstep(0.2, 0.75, lin_boosted.r - max(lin_boosted.g, lin_boosted.b));
                bee_rgb *= red_cutoff;
                final_lin = clamp(bee_rgb, 0.0, 1.0);

            } else {
                // === SNAKE: Dichromatic Background + Pit Organ Thermal IR Overlay ===
                float3 snake_vis;
                snake_vis.r = 0.75 * lin_boosted.g + 0.20 * lin_boosted.r;
                snake_vis.g = 0.85 * lin_boosted.g + 0.15 * lin_boosted.b;
                snake_vis.b = 0.90 * lin_boosted.b;

                // Thermal infrared radiant heat sensing (8-12um simulated response)
                float heat = clamp(1.4 * luminance + 0.7 * max(0.0, lin_boosted.r - lin_boosted.g) - 0.20, 0.0, 1.0);
                float3 thermal_map;
                if (heat < 0.33) {
                    thermal_map = mix(float3(0.04, 0.0, 0.28), float3(0.65, 0.0, 0.55), heat / 0.33);
                } else if (heat < 0.66) {
                    thermal_map = mix(float3(0.65, 0.0, 0.55), float3(1.0, 0.45, 0.0), (heat - 0.33) / 0.33);
                } else {
                    thermal_map = mix(float3(1.0, 0.45, 0.0), float3(1.0, 1.0, 0.85), (heat - 0.66) / 0.34);
                }
                final_lin = mix(snake_vis, thermal_map, 0.55);
            }

            float3 srgb_prime = linearToSrgb(final_lin);
            return half4(srgb_prime, color.a);
        }
    """

    fun createShader(): RuntimeShader {
        return RuntimeShader(SHADER_SRC)
    }

    fun applyProfile(shader: RuntimeShader, profile: AnimalVisionProfile) {
        shader.setFloatUniform("animalType", profile.shaderTypeIndex.toFloat())
        shader.setFloatUniform("tapetumBoost", profile.tapetumFactor)
        shader.setFloatUniform("uvGain", profile.uvSensitivityGain)
        shader.setFloatUniform("desaturation", profile.desaturationFactor)
    }
}
