package com.example.dogvision.shader

import android.graphics.RuntimeShader
import com.example.dogvision.model.AnimalVisionProfile

/**
 * AGSL (Android Graphics Shading Language) runtime shader simulating multi-species vision.
 *
 * Color-science constants in this shader MUST stay in sync with the Kotlin reference
 * implementation [com.example.dogvision.vision.VisionMath]; unit tests guard against drift.
 *
 * Pipeline (per pixel, single pass):
 *   1. Species-specific optical sampling (acuity blur, visual streak, slit pupil,
 *      bifoveal magnification, compound-eye hex lattice, binocular diplopia & sway).
 *   2. Exact sRGB -> linear-light conversion.
 *   3. Synthetic near-UV reconstruction (camera sensors are UV-blocked; UV is inferred
 *      from violet/blue excess and disclosed as synthetic in the UI).
 *   4. Tapetum lucidum low-light collection gain (species-scaled, dim-light only).
 *   5. Photoreceptor transform: Machado (2009) severity-1.0 dichromat matrices for
 *      dog/cat/snake (deuteranopia-analogous) and deer (protanopia-analogous);
 *      cone-contrast models for the raptor tetrachromat and the honeybee trichromat;
 *      dichromat + synthetic radiance fusion for the pit viper;
 *      S-cone depression + warm amber flush for drunk human.
 *   6. Chromatic scaling (oil-droplet contrast expansion / rod-dominated pastel vision).
 *   7. Scotopic (rod) takeover at low adapting luminance.
 */
object AnimalVisionShader {
    const val SHADER_SRC = """
        uniform shader inputBuffer;
        uniform float splitPos;          // 0..1 horizontal partition
        uniform float isSplitMode;       // 1 = enabled, 0 = disabled
        uniform float isBlurEnabled;     // 1 = optics enabled, 0 = disabled
        uniform float isColorFilter;     // 1 = receptor transform enabled, 0 = disabled
        uniform float screenWidth;
        uniform float screenHeight;
        uniform float density;           // Display pixel density scale factor
        uniform float animalType;        // 0=Dog, 1=Deer, 2=Cat, 3=Hawk/Bird, 4=Bee, 5=Snake, 6=Drunk Human
        uniform float tapetumBoost;      // tapetum lucidum collection gain (0 = none)
        uniform float uvGain;            // synthetic UV reconstruction gain (0 = none)
        uniform float rodDominance;      // scotopic achromatic takeover weight
        uniform float chromaticStrength; // cone-contrast scaling around luma
        uniform float desaturation;      // Retinal cone density desaturation weight
        uniform float time;              // Time in seconds for animated effects

        const float3 LUMA = float3(0.2126, 0.7152, 0.0722);

        // Exact CIE sRGB transfer curves.
        float3 srgbToLinear(float3 srgb) {
            return mix(srgb / 12.92, pow((srgb + 0.055) / 1.055, float3(2.4)),
                       step(float3(0.04045), srgb));
        }

        float3 linearToSrgb(float3 lin) {
            return mix(lin * 12.92, 1.055 * pow(lin, float3(1.0 / 2.4)) - 0.055,
                       step(float3(0.0031308), lin));
        }

        // ---------------------------------------------------------------
        // 1. Species optics
        // ---------------------------------------------------------------

        // Binomial 3x3 Gaussian (weights sum to exactly 1.0). ~20/75 acuity (dog),
        // ~20/150 optics (snake).
        half4 gaussianBlur(float2 fc, float r) {
            half4 c = inputBuffer.eval(fc) * 0.25;
            c += (inputBuffer.eval(fc + float2(r, 0.0)) +
                  inputBuffer.eval(fc - float2(r, 0.0))) * 0.125;
            c += (inputBuffer.eval(fc + float2(0.0, r)) +
                  inputBuffer.eval(fc - float2(0.0, r))) * 0.125;
            c += (inputBuffer.eval(fc + float2(r, r)) +
                  inputBuffer.eval(fc - float2(r, r))) * 0.0625;
            c += (inputBuffer.eval(fc + float2(r, -r)) +
                  inputBuffer.eval(fc - float2(r, -r))) * 0.0625;
            return c;
        }

        // Deer: horizontal visual streak. Sharp along the horizon (predator detection),
        // blurred vertically. 1D Gaussian [0.4, 0.25, 0.05] vertical (sums to 1.0).
        half4 deerStreak(float2 fc, float s) {
            half4 c = inputBuffer.eval(fc) * 0.40;
            c += (inputBuffer.eval(fc + float2(0.0, 3.0 * s)) +
                  inputBuffer.eval(fc - float2(0.0, 3.0 * s))) * 0.25;
            c += (inputBuffer.eval(fc + float2(0.0, 6.0 * s)) +
                  inputBuffer.eval(fc - float2(0.0, 6.0 * s))) * 0.05;
            return c;
        }

        // Cat: vertical stenopaeic slit pupil -> horizontal astigmatic smear (sums to 1.0).
        half4 catSlit(float2 fc, float s) {
            half4 c = inputBuffer.eval(fc) * 0.40;
            c += (inputBuffer.eval(fc + float2(3.5 * s, 0.0)) +
                  inputBuffer.eval(fc - float2(3.5 * s, 0.0))) * 0.25;
            c += (inputBuffer.eval(fc + float2(7.0 * s, 0.0)) +
                  inputBuffer.eval(fc - float2(7.0 * s, 0.0))) * 0.05;
            return c;
        }

        // Raptor bifoveal optics (Tucker 2000): the central deep fovea acts as a
        // negative lens giving ~2x retinal magnification with higher receptor density;
        // periphery stays wide and unmagnified.
        half4 bifoveal(float2 fc, float w, float h) {
            float2 center = float2(w * 0.5, h * 0.5);
            float dist = length(fc - center);
            float foveaRadius = min(w, h) * 0.36;

            if (dist < foveaRadius) {
                float foveaFactor = smoothstep(foveaRadius, 0.0, dist);
                float zoom = 1.0 + 1.15 * foveaFactor;
                float2 sc = center + (fc - center) / zoom;

                half4 c = inputBuffer.eval(sc);
                half4 neighbors = (inputBuffer.eval(sc + float2(1.5, 0.0)) +
                                   inputBuffer.eval(sc - float2(1.5, 0.0)) +
                                   inputBuffer.eval(sc + float2(0.0, 1.5)) +
                                   inputBuffer.eval(sc - float2(0.0, 1.5))) * 0.25;
                // Unsharp mask: 20/5 foveal acuity cue.
                return clamp(c + (c - neighbors) * 0.85, half4(0.0), half4(1.0));
            }
            return inputBuffer.eval(fc);
        }

        // Bee: hexagonal ommatidia lattice (each facet samples its center).
        float2 hexGridSample(float2 coord, float r) {
            float2 s = float2(r * 1.7320508, r * 1.5);
            float2 p1 = floor(coord / s) * s;
            float2 p2 = floor((coord - s * 0.5) / s) * s + s * 0.5;
            float2 c1 = p1 + s * 0.5;
            float2 c2 = p2 + s * 0.5;
            return (length(coord - c1) < length(coord - c2)) ? c1 : c2;
        }

        // Drunk Human (6): Diplopia (double vision), ciliary accommodation blur, vestibular sway, chromatic aberration & tunnel vision
        half4 sampleDrunkVision(float2 fragCoord, float w, float h, float t) {
            float2 center = float2(w * 0.5, h * 0.5);
            float s = max(1.0, density);

            // 1. Vestibular ataxia sway: slow erratic drift + rotational wobble
            float swayX = (sin(t * 1.2) * 20.0 + cos(t * 0.65) * 12.0) * s;
            float swayY = (cos(t * 0.95) * 16.0 + sin(t * 1.8) * 8.0) * s;
            float angle = sin(t * 0.75) * 0.035; // ~2 degree rotational wobble

            // Apply rotation around center
            float2 relCoord = fragCoord - center;
            float cosA = cos(angle);
            float sinA = sin(angle);
            float2 rotatedCoord = float2(
                relCoord.x * cosA - relCoord.y * sinA,
                relCoord.x * sinA + relCoord.y * cosA
            ) + center + float2(swayX, swayY);

            // 2. Binocular Diplopia (Double Vision) convergence offset
            // Distinct secondary ghost image that drifts with vergence instability
            float separation = (22.0 + 12.0 * sin(t * 0.85)) * s;
            float2 eyeOffset = float2(separation, sin(t * 1.4) * 5.0 * s);

            // 3. Chromatic Aberration + Ciliary Accommodation Blur for both eye images
            float distFromCenter = length(fragCoord - center) / max(w, h);
            float ca = 1.0 + 0.02 * (1.0 + 0.5 * sin(t * 1.1)) * distFromCenter;

            // Eye 1 sampling (primary image with chromatic dispersion)
            float2 p1 = rotatedCoord - eyeOffset * 0.5;
            float2 p1_r = center + (p1 - center) * ca;
            float2 p1_b = center + (p1 - center) / ca;

            half4 eye1 = half4(
                inputBuffer.eval(p1_r).r,
                inputBuffer.eval(p1).g,
                inputBuffer.eval(p1_b).b,
                1.0
            );
            // Accommodation blur (multi-tap disk blur, scaled by display density)
            float blurRad = 4.0 * s;
            eye1 = eye1 * 0.36 +
                   (inputBuffer.eval(p1 + float2(blurRad, 0.0)) + inputBuffer.eval(p1 - float2(blurRad, 0.0))) * 0.18 +
                   (inputBuffer.eval(p1 + float2(0.0, blurRad)) + inputBuffer.eval(p1 - float2(0.0, blurRad))) * 0.14;

            // Eye 2 sampling (secondary ghost image)
            float2 p2 = rotatedCoord + eyeOffset * 0.5;
            float2 p2_r = center + (p2 - center) * ca;
            float2 p2_b = center + (p2 - center) / ca;

            half4 eye2 = half4(
                inputBuffer.eval(p2_r).r,
                inputBuffer.eval(p2).g,
                inputBuffer.eval(p2_b).b,
                1.0
            );
            eye2 = eye2 * 0.36 +
                   (inputBuffer.eval(p2 + float2(blurRad, 0.0)) + inputBuffer.eval(p2 - float2(blurRad, 0.0))) * 0.18 +
                   (inputBuffer.eval(p2 + float2(0.0, blurRad)) + inputBuffer.eval(p2 - float2(0.0, blurRad))) * 0.14;

            // Blend the two binocular ghost images
            half4 blended = mix(eye1, eye2, 0.45);

            // 4. Pulsing Tunnel Vision Vignette
            float vignettePulse = 0.04 * sin(t * 2.2); // ~75 BPM pulse
            float normDist = length(fragCoord - center) / (min(w, h) * 0.75);
            float vignette = 1.0 - smoothstep(0.70 + vignettePulse, 1.35 + vignettePulse, normDist);

            return half4(blended.rgb * vignette, blended.a);
        }

        half4 sampleOptics(float2 fc) {
            if (isBlurEnabled < 0.5) {
                return inputBuffer.eval(fc);
            }
            float s = max(1.0, density);
            if (animalType < 0.5) {
                return gaussianBlur(fc, 3.0 * s);   // Dog 20/75
            } else if (animalType < 1.5) {
                return deerStreak(fc, s);           // Deer streak
            } else if (animalType < 2.5) {
                return catSlit(fc, s);              // Cat slit
            } else if (animalType < 3.5) {
                return bifoveal(fc, screenWidth, screenHeight); // Hawk / Bird
            } else if (animalType < 4.5) {
                return inputBuffer.eval(hexGridSample(fc, 12.0 * s)); // Bee
            } else if (animalType < 5.5) {
                return gaussianBlur(fc, 4.0 * s);   // Snake
            } else {
                return sampleDrunkVision(fc, screenWidth, screenHeight, time); // Drunk Human
            }
        }

        // ---------------------------------------------------------------
        // 2. Receptor transforms (linear-light; mirrors VisionMath.kt)
        // ---------------------------------------------------------------

        // Machado et al. (2009) severity-1.0 deuteranopia (missing M cone), linear sRGB.
        float3 machadoDeuteranopia(float3 lin) {
            return float3(
                dot(float3( 0.367322,  0.860646, -0.227968), lin),
                dot(float3( 0.280085,  0.672501,  0.047413), lin),
                dot(float3(-0.011818,  0.042940,  0.968879), lin));
        }

        // Machado et al. (2009) severity-1.0 protanopia (missing L cone), linear sRGB.
        float3 machadoProtanopia(float3 lin) {
            return float3(
                dot(float3( 0.152262,  1.052986, -0.205244), lin),
                dot(float3( 0.114503,  0.786281,  0.099216), lin),
                dot(float3(-0.003882, -0.048105,  1.051987), lin));
        }

        // Honeybee UV(345)/B(440)/G(540) trichromat rendered into human-visible colors.
        // G receptor has negligible long-wave sensitivity -> red renders near-black.
        float3 beeRender(float3 lin, float uv) {
            float g = 0.75 * lin.g + 0.05 * lin.r;
            float b = lin.b;
            float u = uvGain * uv;
            return clamp(float3(0.55 * u, g, 0.85 * b + 0.65 * u), 0.0, 1.0);
        }

        // Raptor UV/S/M/L tetrachromat: approximate cone excitations, re-render to the
        // human-visible gamut. Oil-droplet contrast expansion is applied globally.
        float3 hawkRender(float3 lin, float uv) {
            float cUv = uvGain * uv + 0.15 * lin.b;
            float cS  = 0.85 * lin.b;
            float cM  = 0.90 * lin.g;
            float cL  = 0.75 * lin.r + 0.25 * lin.g;
            return clamp(float3(0.75 * cL,
                                0.35 * cL + 0.75 * cM,
                                0.85 * cS + 0.70 * cUv), 0.0, 1.0);
        }

        // Ironbow-style colormap for the synthetic pit-organ radiance overlay.
        float3 ironbow(float t) {
            if (t < 0.33) return mix(float3(0.04, 0.0, 0.28), float3(0.65, 0.0, 0.55), t / 0.33);
            if (t < 0.66) return mix(float3(0.65, 0.0, 0.55), float3(1.0, 0.45, 0.0), (t - 0.33) / 0.33);
            return mix(float3(1.0, 0.45, 0.0), float3(1.0, 1.0, 0.85), min((t - 0.66) / 0.34, 1.0));
        }

        // Pit viper: S+L dichromat backdrop fused with a synthetic 8-12um radiance proxy.
        float3 snakeRender(float3 lin) {
            float3 vis = clamp(machadoDeuteranopia(lin), 0.0, 1.0);
            float heat = clamp(1.25 * dot(vis, LUMA) +
                               0.9 * max(0.0, lin.r - max(lin.g, lin.b)) - 0.18, 0.0, 1.0);
            return mix(vis, ironbow(heat), 0.5);
        }

        // Drunk Human: acute S-cone depression, warm amber shift, glare bloom
        float3 drunkRender(float3 lin, float lum) {
            float glare = smoothstep(0.65, 1.0, lum) * 0.35;
            float3 warm = clamp(float3(lin.r * 1.08, lin.g * 1.02, lin.b * 0.85), 0.0, 1.0);
            float l = dot(warm, LUMA);
            float3 base = warm * 0.85 + float3(l * 0.15);
            return clamp(base + float3(glare * 0.4, glare * 0.3, glare * 0.1), 0.0, 1.0);
        }

        float3 chromaticScale(float3 c, float k) {
            float l = dot(c, LUMA);
            return l + (c - float3(l)) * k;
        }

        half4 main(float2 fc) {
            // Split comparison: left side stays unprocessed human trichromatic feed.
            if (isSplitMode > 0.5 && fc.x < splitPos * screenWidth) {
                return inputBuffer.eval(fc);
            }

            half4 sampled = sampleOptics(fc);
            if (isColorFilter < 0.5) {
                return sampled;
            }

            float3 lin = srgbToLinear(float3(sampled.rgb));
            float adaptingLum = dot(lin, LUMA);

            // Synthetic UV reconstruction (camera sensors are UV-blocked).
            float uv = max(0.0, lin.b - 0.65 * lin.g - 0.30 * lin.r);

            // Tapetum lucidum collection gain, dim light only.
            float gain = 1.0 + tapetumBoost * exp(-5.0 * adaptingLum);
            lin = clamp(lin * gain, 0.0, 1.0);

            float3 outc;
            if (animalType < 0.5) {
                outc = clamp(machadoDeuteranopia(lin), 0.0, 1.0);          // Dog
            } else if (animalType < 1.5) {
                float3 p = clamp(machadoProtanopia(lin), 0.0, 1.0);        // Deer
                p.b += uvGain * uv;                                        // UV-open S cone
                outc = clamp(p, 0.0, 1.0);
            } else if (animalType < 2.5) {
                outc = clamp(machadoDeuteranopia(lin), 0.0, 1.0);          // Cat
            } else if (animalType < 3.5) {
                outc = hawkRender(lin, uv);                                // Hawk / Bird
            } else if (animalType < 4.5) {
                outc = beeRender(lin, uv);                                 // Bee
            } else if (animalType < 5.5) {
                outc = snakeRender(lin);                                   // Snake
            } else {
                outc = drunkRender(lin, adaptingLum);                      // Drunk Human
            }

            outc = chromaticScale(outc, chromaticStrength);

            // Scotopic takeover: below cone threshold vision is rod-driven and achromatic.
            float t = 1.0 - smoothstep(0.003, 0.05, min(adaptingLum, dot(outc, LUMA)));
            float w = clamp(rodDominance * t, 0.0, 1.0);
            outc = mix(outc, float3(dot(outc, LUMA)), w);

            return half4(linearToSrgb(clamp(outc, 0.0, 1.0)), sampled.a);
        }
    """

    fun createShader(): RuntimeShader = RuntimeShader(SHADER_SRC)

    fun applyProfile(shader: RuntimeShader, profile: AnimalVisionProfile, density: Float = 1.0f) {
        shader.setFloatUniform("animalType", profile.shaderTypeIndex.toFloat())
        shader.setFloatUniform("tapetumBoost", profile.tapetumFactor)
        shader.setFloatUniform("uvGain", profile.uvSensitivityGain)
        shader.setFloatUniform("rodDominance", profile.rodDominance)
        shader.setFloatUniform("chromaticStrength", profile.chromaticStrength)
        shader.setFloatUniform("desaturation", profile.desaturationFactor)
        shader.setFloatUniform("density", density)
    }
}
