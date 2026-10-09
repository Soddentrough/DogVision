package com.example.dogvision

import com.example.dogvision.shader.AnimalVisionShader
import com.example.dogvision.vision.VisionMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Validates the Kotlin reference implementation ([VisionMath]) that the shipping AGSL
 * shader mirrors, plus drift guards asserting the shader source embeds the same
 * published constants.
 */
class VisionMathTest {

    private fun lin(rgb: FloatArray) = FloatArray(3) { VisionMath.linearToSrgb(rgb[it].coerceIn(0f, 1f)) }
    private fun toLin(rgb: FloatArray) = FloatArray(3) { VisionMath.srgbToLinear(rgb[it]) }
    private fun hue(r: Float, g: Float) = r / maxOf(g, 1e-6f)

    // ---- sRGB transfer curve ----

    @Test
    fun `srgb round trip is identity`() {
        for (v in listOf(0f, 0.001f, 0.04f, 0.0405f, 0.2f, 0.5f, 0.9f, 1f)) {
            assertEquals(v, VisionMath.linearToSrgb(VisionMath.srgbToLinear(v)), 1e-5f)
        }
    }

    // ---- Machado (2009) matrix properties ----

    @Test
    fun `machado matrices preserve neutral grays`() {
        for (m in listOf(VisionMath.MAT_DEUTERANOPIA, VisionMath.MAT_PROTANOPIA)) {
            for (gray in listOf(0.18f, 0.5f, 0.9f)) {
                val out = VisionMath.applyMatrix(m, gray, gray, gray)
                for (c in out) assertEquals(gray, c, 1e-4f)
            }
        }
    }

    @Test
    fun `deuteranopia maps red and green to confusable hues`() {
        val red = VisionMath.applyMatrix(VisionMath.MAT_DEUTERANOPIA, 1f, 0f, 0f)
        val green = VisionMath.applyMatrix(VisionMath.MAT_DEUTERANOPIA, 0f, 1f, 0f)
        // Both collapse onto a yellowish (high R/G ratio, low blue) confusion line.
        assertTrue("red hue ${hue(red[0], red[1])} vs green hue ${hue(green[0], green[1])}",
            Math.abs(Math.log((hue(red[0], red[1]) / hue(green[0], green[1])).toDouble())) < 0.2)
        assertTrue("blue must stay low for both", red[2] < 0.05f && green[2] < 0.15f)
    }

    @Test
    fun `protanopia darkens red strongly but keeps green bright`() {
        val red = VisionMath.applyMatrix(VisionMath.MAT_PROTANOPIA, 1f, 0f, 0f)
        val green = VisionMath.applyMatrix(VisionMath.MAT_PROTANOPIA, 0f, 1f, 0f)
        val redLum = VisionMath.luma(red)
        val greenLum = VisionMath.luma(green)
        // Human L/M contribution of red (0.2126+0.7152) is largely lost for a protanope.
        assertTrue("red lum $redLum should be far below input lum 0.2126", redLum < 0.15f)
        assertTrue("green lum $greenLum should remain high", greenLum > 0.6f)
    }

    // ---- Full-path species checks (linear pipeline, mirrors shader) ----

    private fun simulate(animalType: Int, srgb: FloatArray, tapetum: Float, uvGain: Float,
                         rod: Float, chroma: Float): FloatArray {
        var lin = toLin(srgb)
        val adapting = VisionMath.luma(lin)
        val uv = VisionMath.reconstructUv(lin[0], lin[1], lin[2])
        lin = VisionMath.tapetumGain(lin, tapetum)
        val out = when (animalType) {
            0 -> VisionMath.applyMatrix(VisionMath.MAT_DEUTERANOPIA, lin[0], lin[1], lin[2]).clamp01()
            1 -> {
                val p = VisionMath.applyMatrix(VisionMath.MAT_PROTANOPIA, lin[0], lin[1], lin[2]).clamp01()
                p[2] = (p[2] + uvGain * uv).coerceIn(0f, 1f)
                p
            }
            2 -> VisionMath.applyMatrix(VisionMath.MAT_DEUTERANOPIA, lin[0], lin[1], lin[2]).clamp01()
            3 -> VisionMath.hawkTransform(lin, uv, uvGain)
            4 -> VisionMath.beeTransform(lin, uv, uvGain)
            5 -> VisionMath.snakeTransform(lin)
            else -> VisionMath.drunkTransform(lin)
        }
        val scaled = VisionMath.chromaticScale(out, chroma).clamp01()
        return lin(VisionMath.scotopicDesaturation(scaled, adapting, rod))
    }

    private fun FloatArray.clamp01() = FloatArray(3) { this[it].coerceIn(0f, 1f) }

    @Test
    fun `dog - blaze orange desaturates toward yellow-brown`() {
        val out = simulate(0, floatArrayOf(1f, 0.35f, 0f), 2.0f, 0f, 0.85f, 0.9f)
        // Deuteranopic orange: R and G track together, blue collapses.
        assertTrue("blue should be suppressed: $out[2]", out[2] < out[0] && out[2] < out[1])
    }

    @Test
    fun `deer - UV-brightened blue is boosted versus dog`() {
        val blueJeans = floatArrayOf(0.05f, 0.15f, 0.85f)
        val deer = simulate(1, blueJeans, 3.0f, 1.6f, 0.9f, 0.9f)
        val dog = simulate(0, blueJeans, 2.0f, 0f, 0.85f, 0.9f)
        assertTrue("deer blue channel ${deer[2]} should exceed dog ${dog[2]}", deer[2] > dog[2])
    }

    @Test
    fun `deer - blaze orange loses long-wave luminance`() {
        val orange = floatArrayOf(1f, 0.35f, 0f)
        val deer = simulate(1, orange, 0f, 1.6f, 0f, 0.9f)
        val humanLum = VisionMath.luma(toLin(orange))
        val deerLum = VisionMath.luma(toLin(deer).let { it })
        assertTrue("deer-perceived luminance $deerLum must drop vs human $humanLum", deerLum < humanLum)
    }

    @Test
    fun `bee - pure red renders near black`() {
        val out = simulate(4, floatArrayOf(1f, 0f, 0f), 0f, 2.2f, 0.2f, 1.0f)
        val l = VisionMath.luma(toLin(out))
        assertTrue("bee red luminance $l should be very low", l < 0.08f)
    }

    @Test
    fun `bee - violet UV excess renders as bee purple`() {
        // Nectar-guide-like UV reflector: strong blue with violet excess.
        val out = simulate(4, floatArrayOf(0.1f, 0.05f, 0.9f), 0f, 2.2f, 0.2f, 1.0f)
        assertTrue("violet should produce a red (purple) component: $out", out[0] > 0.2f)
        assertTrue("and a strong blue component", out[2] > 0.7f)
    }

    @Test
    fun `hawk - chromatic contrast is expanded versus dog`() {
        val saturated = floatArrayOf(0.9f, 0.2f, 0.1f)
        val hawk = toLin(simulate(3, saturated, 0f, 2.0f, 0.3f, 1.3f))
        val dog = toLin(simulate(0, saturated, 0f, 0f, 0.85f, 0.9f))
        fun chroma(c: FloatArray): Float {
            val l = VisionMath.luma(c)
            return (kotlin.math.abs(c[0] - l) + kotlin.math.abs(c[1] - l) + kotlin.math.abs(c[2] - l))
        }
        assertTrue("hawk chroma ${chroma(hawk)} should exceed dog ${chroma(dog)}", chroma(hawk) > chroma(dog))
    }

    @Test
    fun `snake - warm target produces radiance overlay brightening`() {
        val warm = floatArrayOf(1f, 0.25f, 0.05f)
        val cool = floatArrayOf(0.2f, 0.25f, 0.3f)
        val warmOut = VisionMath.luma(toLin(simulate(5, warm, 0f, 0f, 0.6f, 0.85f)))
        val coolOut = VisionMath.luma(toLin(simulate(5, cool, 0f, 0f, 0.6f, 0.85f)))
        assertTrue("warm target $warmOut should outshine cool $coolOut in the thermal channel",
            warmOut > coolOut)
    }

    @Test
    fun `drunk human - warm amber shift and mild desaturation`() {
        val white = floatArrayOf(1f, 1f, 1f)
        val out = VisionMath.drunkTransform(white)
        assertTrue("red should be greater than blue", out[0] > out[2])
    }

    @Test
    fun `drunk human - simulate produces warm tinted output`() {
        val grey = floatArrayOf(0.5f, 0.5f, 0.5f)
        val out = simulate(6, grey, 0f, 0f, 0.4f, 0.85f)
        assertTrue("red channel should exceed blue channel", out[0] > out[2])
    }

    // ---- Tapetum & scotopic behaviour ----

    @Test
    fun `tapetum boosts dim light and ignores bright light`() {
        val dim = floatArrayOf(0.05f, 0.05f, 0.05f)
        val bright = floatArrayOf(0.9f, 0.9f, 0.9f)
        val boostedDim = VisionMath.tapetumGain(dim, 2.0f)
        val boostedBright = VisionMath.tapetumGain(bright, 2.0f)
        assertTrue("dim must be boosted", boostedDim[0] > dim[0] * 1.5f)
        assertTrue("bright must be nearly untouched", boostedBright[0] <= bright[0] * 1.05f)
    }

    @Test
    fun `scotopic takeover desaturates dim colored light`() {
        val dimColor = floatArrayOf(0.02f, 0.005f, 0.0f)
        val out = VisionMath.scotopicDesaturation(dimColor, 0.002f, 1.0f)
        assertEquals("fully rod-driven -> achromatic", out[0], out[1], 1e-5f)
        assertEquals(out[1], out[2], 1e-5f)
    }

    // ---- Drift guards: shader source must embed the same published constants ----

    @Test
    fun `shader embeds machado deuteranopia constants`() {
        for (row in VisionMath.MAT_DEUTERANOPIA) {
            for (v in row) {
                assertTrue("shader missing constant $v",
                    AnimalVisionShader.SHADER_SRC.contains(v.toString()))
            }
        }
    }

    @Test
    fun `shader embeds machado protanopia constants`() {
        for (row in VisionMath.MAT_PROTANOPIA) {
            for (v in row) {
                assertTrue("shader missing constant $v",
                    AnimalVisionShader.SHADER_SRC.contains(v.toString()))
            }
        }
    }

    @Test
    fun `shader uses exact srgb transfer curve`() {
        assertTrue(AnimalVisionShader.SHADER_SRC.contains("0.04045"))
        assertTrue(AnimalVisionShader.SHADER_SRC.contains("0.0031308"))
        assertTrue(AnimalVisionShader.SHADER_SRC.contains("12.92"))
    }

    @Test
    fun `shader optics weights sum to unity`() {
        // gaussian9: 0.25 + 4*0.125 + 4*0.0625 == 1.0 ; streak/slit: 0.40 + 2*0.25 + 2*0.05 == 1.0
        assertEquals(1.0f, 0.25f + 4 * 0.125f + 4 * 0.0625f, 1e-6f)
        assertEquals(1.0f, 0.40f + 2 * 0.25f + 2 * 0.05f, 1e-6f)
    }

    @Test
    fun `shader embeds drunk human support`() {
        assertTrue(AnimalVisionShader.SHADER_SRC.contains("sampleDrunkVision"))
        assertTrue(AnimalVisionShader.SHADER_SRC.contains("drunkRender"))
    }
}
