package com.example.dogvision.vision

import kotlin.math.max
import kotlin.math.pow

/**
 * Single source of truth for the color-vision mathematics used by the AGSL shader
 * ([com.example.dogvision.shader.AnimalVisionShader]).
 *
 * All transforms operate on *linear-light* RGB in the sRGB primaries unless stated otherwise.
 *
 * Scientific basis:
 *  - Machado, Oliveira & Fernandes (2009), "A reality for color vision deficiencies",
 *    IEEE TVCG 15(6): severity-1.0 simulation matrices for protanopia / deuteranopia,
 *    defined in linear RGB. Grayscale-preserving by construction (each row sums to 1).
 *  - Stockman & Sharpe (2000), "The spectral sensitivities of the human L, M and S cones
 *    detected directly in the fovea", Vision Research 40:17.
 *  - Neitz, Geist & Jacobs (1989), "Color vision in the dog", Vis. Neurosci. 3:119:
 *    dogs are dichromats with S (~429 nm) and L-like (~555 nm) cones, i.e. functionally
 *    analogous to human deuteranopia (missing middle-wavelength cone).
 *  - Guo & Yan (2015) / Gu et al. (2016), "Colour vision in cats", Evolution 70:2981:
 *    cats are S/M-L dichromats with low cone density; functionally deuteranopia-like
 *    with strong rod (achromatic) dominance.
 *  - Deer (Odocoileus virginianus): dichromats with S (~452-455 nm, UV-sensitive lens)
 *    and M (~537 nm) cones and no L cone => functionally analogous to human protanopia
 *    (missing long-wavelength cone), plus near-UV transmission through a UV-transparent
 *    ocular medium (Knop et al. 2022, UGA Deer Lab).
 *  - Peitsch, Bode, Giurfa & Menzel (1992), J. Comp. Physiol. A 170:23: honeybee
 *    photoreceptors peak at ~345 nm (UV), ~440 nm (blue), ~540 nm (green); negligible
 *    sensitivity above ~580 nm ("red blindness").
 *  - Tucker (2000), J. Exp. Biol. 203:3745; Lind, Kelber et al.: raptor bifoveal
 *    tetrachromacy with carotenoid oil droplets narrowing cone spectral bandwidth,
 *    which increases chromatic (cone-contrast) signal.
 *
 * IMPORTANT LIMITATION (disclosed to users): phone camera lenses carry UV-blocking
 * filters, so real UV reflectance never reaches the sensor. The "UV" signal here is a
 * *synthetic reconstruction* derived from violet/blue channel excess. Likewise the pit
 * viper "thermal" channel is a heuristic radiance proxy, not real 8-12 um IR.
 */
object VisionMath {

    /** Rec.709 / sRGB relative luminance weights. */
    val LUMA = floatArrayOf(0.2126f, 0.7152f, 0.0722f)

    /**
     * Machado et al. (2009) severity-1.0 deuteranopia matrix (missing M cone), linear sRGB.
     * Row-major. Used for dog, cat and snake (S + L dichromats).
     */
    val MAT_DEUTERANOPIA = arrayOf(
        floatArrayOf(0.367322f, 0.860646f, -0.227968f),
        floatArrayOf(0.280085f, 0.672501f, 0.047413f),
        floatArrayOf(-0.011818f, 0.042940f, 0.968879f),
    )

    /**
     * Machado et al. (2009) severity-1.0 protanopia matrix (missing L cone), linear sRGB.
     * Row-major. Used for deer (S + M dichromat, no L cone).
     */
    val MAT_PROTANOPIA = arrayOf(
        floatArrayOf(0.152262f, 1.052986f, -0.205244f),
        floatArrayOf(0.114503f, 0.786281f, 0.099216f),
        floatArrayOf(-0.003882f, -0.048105f, 1.051987f),
    )

    /** Exact CIE sRGB electro-optical transfer function (inverse of the OETF). */
    fun srgbToLinear(c: Float): Float =
        if (c <= 0.04045f) c / 12.92f else ((c + 0.055f) / 1.055f).pow(2.4f)

    /** Exact inverse of [srgbToLinear]. */
    fun linearToSrgb(l: Float): Float =
        if (l <= 0.0031308f) l * 12.92f else 1.055f * l.pow(1f / 2.4f) - 0.055f

    fun luma(rgb: FloatArray): Float =
        LUMA[0] * rgb[0] + LUMA[1] * rgb[1] + LUMA[2] * rgb[2]

    fun applyMatrix(m: Array<FloatArray>, r: Float, g: Float, b: Float): FloatArray =
        floatArrayOf(
            m[0][0] * r + m[0][1] * g + m[0][2] * b,
            m[1][0] * r + m[1][1] * g + m[1][2] * b,
            m[2][0] * r + m[2][1] * g + m[2][2] * b,
        )

    /**
     * Synthetic UV reconstruction: near-UV reflectance (laundry brighteners, UV plumage)
     * leaks into the camera's blue channel as a violet/blue excess over green and red.
     * Returns a 0..1-ish UV salience signal.
     */
    fun reconstructUv(r: Float, g: Float, b: Float): Float =
        max(0f, b - 0.65f * g - 0.30f * r)

    /**
     * Tapetum lucidum light collection: multiplicative exposure-like gain that acts only
     * in dim conditions (retroreflection is negligible against bright adapting light).
     * [boost] is the species tapetum gain (0 = none, e.g. diurnal birds).
     */
    fun tapetumGain(lin: FloatArray, boost: Float): FloatArray {
        val lum = luma(lin)
        val gain = 1f + boost * Math.exp(-5.0 * lum).toFloat()
        return floatArrayOf(
            (lin[0] * gain).coerceIn(0f, 1f),
            (lin[1] * gain).coerceIn(0f, 1f),
            (lin[2] * gain).coerceIn(0f, 1f),
        )
    }

    /**
     * Scotopic takeover: below cone thresholds vision is rod-driven and achromatic.
     * [rodDominance] scales the effect (1.0 = extremely rod-dominated cat, low for bees).
     */
    fun scotopicDesaturation(rgb: FloatArray, adaptingLum: Float, rodDominance: Float): FloatArray {
        val t = 1f - smoothstep(0.003f, 0.05f, minOf(adaptingLum, luma(rgb)))
        val w = (rodDominance * t).coerceIn(0f, 1f)
        val l = luma(rgb)
        return floatArrayOf(
            l + (rgb[0] - l) * (1f - w),
            l + (rgb[1] - l) * (1f - w),
            l + (rgb[2] - l) * (1f - w),
        )
    }

    /** Global chromatic scaling around achromatic luma. k > 1 expands cone contrast. */
    fun chromaticScale(rgb: FloatArray, k: Float): FloatArray {
        val l = luma(rgb)
        return floatArrayOf(
            l + (rgb[0] - l) * k,
            l + (rgb[1] - l) * k,
            l + (rgb[2] - l) * k,
        )
    }

    /**
     * Honeybee (Apis mellifera) color rendering.
     * Cone space: UV ~345 nm (synthetic), B ~440 nm (maps to camera blue),
     * G ~540 nm (maps to camera green with a small long-wave leak; red-blind).
     * Rendered back into human-visible colors: G -> green, B -> blue, UV -> violet.
     */
    fun beeTransform(lin: FloatArray, uv: Float, uvGain: Float): FloatArray {
        val g = 0.75f * lin[1] + 0.05f * lin[0]
        val b = lin[2]
        val u = uvGain * uv
        return floatArrayOf(
            (0.55f * u).coerceIn(0f, 1f),
            g.coerceIn(0f, 1f),
            (0.85f * b + 0.65f * u).coerceIn(0f, 1f),
        )
    }

    /**
     * Raptor (tetrachromat) color rendering. Approximate UV/S/M/L cone excitations from
     * camera RGB + synthetic UV, then re-render into the human-visible gamut.
     * Oil-droplet contrast expansion is applied globally via [chromaticScale].
     */
    fun hawkTransform(lin: FloatArray, uv: Float, uvGain: Float): FloatArray {
        val cUv = uvGain * uv + 0.15f * lin[2]
        val cS = 0.85f * lin[2]
        val cM = 0.9f * lin[1]
        val cL = 0.75f * lin[0] + 0.25f * lin[1]
        return floatArrayOf(
            (0.75f * cL).coerceIn(0f, 1f),
            (0.35f * cL + 0.75f * cM).coerceIn(0f, 1f),
            (0.85f * cS + 0.70f * cUv).coerceIn(0f, 1f),
        )
    }

    /**
     * Pit viper: S+L dichromatic backdrop (deuteranopia-analogous) fused with a
     * *synthetic* false-color radiance proxy for the pit organ (8-12 um thermal IR).
     * Heat is estimated from luminance plus long-wave (thermal-radiance-correlated) excess.
     */
    fun snakeTransform(lin: FloatArray): FloatArray {
        val raw = applyMatrix(MAT_DEUTERANOPIA, lin[0], lin[1], lin[2])
        val vis = floatArrayOf(raw[0].coerceIn(0f, 1f), raw[1].coerceIn(0f, 1f), raw[2].coerceIn(0f, 1f))
        val heat = (1.25f * luma(vis) + 0.9f * max(0f, lin[0] - max(lin[1], lin[2])) - 0.18f)
            .coerceIn(0f, 1f)
        val ir = ironbow(heat)
        val w = 0.5f
        return floatArrayOf(
            vis[0] * (1f - w) + ir[0] * w,
            vis[1] * (1f - w) + ir[1] * w,
            vis[2] * (1f - w) + ir[2] * w,
        )
    }

    /**
     * Drunk human: acute S-cone (blue-yellow) channel depression, warm amber
     * "beer goggles" shift and mild contrast flattening.
     */
    fun drunkTransform(lin: FloatArray): FloatArray {
        val warm = floatArrayOf(
            (lin[0] * 1.08f).coerceIn(0f, 1f),
            (lin[1] * 1.02f).coerceIn(0f, 1f),
            (lin[2] * 0.85f).coerceIn(0f, 1f),
        )
        val l = luma(warm)
        return floatArrayOf(
            warm[0] * 0.85f + l * 0.15f,
            warm[1] * 0.85f + l * 0.15f,
            warm[2] * 0.85f + l * 0.15f,
        )
    }

    /** Ironbow-style colormap used for the synthetic pit-organ radiance overlay. */
    fun ironbow(t: Float): FloatArray = when {
        t < 0.33f -> lerp3(0.04f, 0.0f, 0.28f, 0.65f, 0.0f, 0.55f, t / 0.33f)
        t < 0.66f -> lerp3(0.65f, 0.0f, 0.55f, 1.0f, 0.45f, 0.0f, (t - 0.33f) / 0.33f)
        else -> lerp3(1.0f, 0.45f, 0.0f, 1.0f, 1.0f, 0.85f, ((t - 0.66f) / 0.34f).coerceAtMost(1f))
    }

    private fun lerp3(
        r0: Float, g0: Float, b0: Float,
        r1: Float, g1: Float, b1: Float,
        t: Float,
    ): FloatArray = floatArrayOf(
        r0 + (r1 - r0) * t,
        g0 + (g1 - g0) * t,
        b0 + (b1 - b0) * t,
    )

    private fun smoothstep(edge0: Float, edge1: Float, x: Float): Float {
        val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }
}
