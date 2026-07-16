package com.example.dogvision

import org.junit.Test
import org.junit.Assert.*
import kotlin.math.pow

class DogVisionMathTest {

    // Simple implementation of the math in DogVisionShader for verification
    private fun srgbToLinear(c: Float): Float {
        return if (c <= 0.04045f) c / 12.92f else ((c + 0.055f) / 1.055f).pow(2.4f)
    }

    private fun linearToSrgb(c: Float): Float {
        return if (c <= 0.0031308f) c * 12.92f else 1.055f * c.pow(1.0f / 2.4f) - 0.055f
    }

    private fun simulateDogVision(r: Float, g: Float, b: Float): Triple<Float, Float, Float> {
        val linR = srgbToLinear(r)
        val linG = srgbToLinear(g)
        val linB = srgbToLinear(b)

        // Linear RGB to LMS (Smith-Pokorny)
        val L = 0.15514f * linR + 0.54312f * linG + 0.03276f * linB
        val M = 0.06538f * linR + 0.67563f * linG + 0.04768f * linB
        val S = 0.00440f * linR + 0.01772f * linG + 0.32219f * linB

        // Deuteranopia projection: M' = 0.494207 * L + 1.24827 * S
        val mPrime = 0.494207f * L + 1.24827f * S

        // LMS back to Linear RGB
        val linRPrime = 9.42436f * L - 7.57602f * mPrime + 0.30136f * S
        val linGPrime = -0.91264f * L + 2.16235f * mPrime - 0.10651f * S
        val linBPrime = 0.01467f * L - 0.12648f * mPrime + 3.16104f * S

        return Triple(
            linearToSrgb(linRPrime.coerceIn(0f, 1f)),
            linearToSrgb(linGPrime.coerceIn(0f, 1f)),
            linearToSrgb(linBPrime.coerceIn(0f, 1f))
        )
    }

    @Test
    fun testRedBecomesYellowish() {
        val (r, g, b) = simulateDogVision(1f, 0f, 0f)
        println("Red (1,0,0) -> Dog ($r, $g, $b)")
        assertTrue("Red should have significant R component", r > 0.8f)
        assertTrue("Red should have some G component (yellowish)", g > 0.1f)
    }

    @Test
    fun testGreenBecomesYellowish() {
        val (r, g, b) = simulateDogVision(0f, 1f, 0f)
        println("Green (0,1,0) -> Dog ($r, $g, $b)")
        assertTrue("Green should look like yellow (High R and some G)", r > 0.8f && g > 0.3f)
    }

    @Test
    fun testBlueRemainsVibrant() {
        val (r, g, b) = simulateDogVision(0f, 0f, 1f)
        println("Blue (0,0,1) -> Dog ($r, $g, $b)")
        assertTrue("Blue should remain vibrant", b > 0.8f)
    }

    @Test
    fun testYellowRemainsVibrant() {
        val (r, g, b) = simulateDogVision(1f, 1f, 0f)
        println("Yellow (1,1,0) -> Dog ($r, $g, $b)")
        assertTrue("Yellow should remain vibrant", r > 0.8f && g > 0.4f)
    }
}
