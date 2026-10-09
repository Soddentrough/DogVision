package com.example.dogvision

import org.junit.Test
import org.junit.Assert.*
import kotlin.math.exp
import kotlin.math.pow

class DogVisionMathTest {

    private fun srgbToLinear(c: Float): Float {
        return c.pow(2.2f)
    }

    private fun linearToSrgb(c: Float): Float {
        return c.pow(1.0f / 2.2f)
    }

    private fun simulateDogShader(r: Float, g: Float, b: Float): Triple<Float, Float, Float> {
        val linR = srgbToLinear(r)
        val linG = srgbToLinear(g)
        val linB = srgbToLinear(b)

        val luminance = 0.2126f * linR + 0.7152f * linG + 0.0722f * linB
        val boost = 2.0f * exp(-5.0f * luminance)
        val linBoostedR = (linR * (1.0f + boost)).coerceIn(0f, 1f)
        val linBoostedG = (linG * (1.0f + boost)).coerceIn(0f, 1f)
        val linBoostedB = (linB * (1.0f + boost)).coerceIn(0f, 1f)

        val rPrime = (0.625f * linBoostedR + 0.375f * linBoostedG + 0.000f * linBoostedB).coerceIn(0f, 1f)
        val gPrime = (0.700f * linBoostedR + 0.300f * linBoostedG + 0.000f * linBoostedB).coerceIn(0f, 1f)
        val bPrime = (0.000f * linBoostedR + 0.300f * linBoostedG + 0.700f * linBoostedB).coerceIn(0f, 1f)

        val primeLum = 0.2126f * rPrime + 0.7152f * gPrime + 0.0722f * bPrime
        val desatR = (1.0f - 0.6f) * primeLum + 0.6f * rPrime
        val desatG = (1.0f - 0.6f) * primeLum + 0.6f * gPrime
        val desatB = (1.0f - 0.6f) * primeLum + 0.6f * bPrime

        return Triple(
            linearToSrgb(desatR.coerceIn(0f, 1f)),
            linearToSrgb(desatG.coerceIn(0f, 1f)),
            linearToSrgb(desatB.coerceIn(0f, 1f))
        )
    }

    private fun simulateDeerShader(r: Float, g: Float, b: Float): Triple<Float, Float, Float> {
        val linR = srgbToLinear(r)
        val linG = srgbToLinear(g)
        val linB = srgbToLinear(b)

        val luminance = 0.2126f * linR + 0.7152f * linG + 0.0722f * linB
        val boost = 4.5f * exp(-5.0f * luminance)
        val linBoostedR = (linR * (1.0f + boost)).coerceIn(0f, 1f)
        val linBoostedG = (linG * (1.0f + boost)).coerceIn(0f, 1f)
        val linBoostedB = (linB * (1.0f + boost)).coerceIn(0f, 1f)

        val sCone = linBoostedB + 1.6f * maxOf(0f, linBoostedB - linBoostedG * 0.5f)
        val mCone = 0.85f * linBoostedG + 0.12f * linBoostedR

        val deerR = (0.10f * sCone + 0.88f * mCone).coerceIn(0f, 1f)
        val deerG = (0.06f * sCone + 0.92f * mCone).coerceIn(0f, 1f)
        val deerB = (1.15f * sCone + 0.04f * mCone).coerceIn(0f, 1f)

        val primeLum = 0.2126f * deerR + 0.7152f * deerG + 0.0722f * deerB
        val desatR = (1.0f - 0.55f) * primeLum + 0.55f * deerR
        val desatG = (1.0f - 0.55f) * primeLum + 0.55f * deerG
        val desatB = (1.0f - 0.55f) * primeLum + 0.55f * deerB

        return Triple(
            linearToSrgb(desatR.coerceIn(0f, 1f)),
            linearToSrgb(desatG.coerceIn(0f, 1f)),
            linearToSrgb(desatB.coerceIn(0f, 1f))
        )
    }

    private fun simulateBeeShader(r: Float, g: Float, b: Float): Triple<Float, Float, Float> {
        val linR = srgbToLinear(r)
        val linG = srgbToLinear(g)
        val linB = srgbToLinear(b)

        val uvBee = maxOf(0f, linB - linG * 0.7f) * 2.2f + 0.20f * linB
        var beeR = 0.82f * uvBee
        var beeG = 0.92f * linG
        var beeB = 1.08f * linB + 0.30f * uvBee

        val redDiff = linR - maxOf(linG, linB)
        val redCutoff = (1.0f - ((redDiff - 0.2f) / 0.55f).coerceIn(0f, 1f))
        beeR = (beeR * redCutoff).coerceIn(0f, 1f)
        beeG = (beeG * redCutoff).coerceIn(0f, 1f)
        beeB = (beeB * redCutoff).coerceIn(0f, 1f)

        return Triple(
            linearToSrgb(beeR),
            linearToSrgb(beeG),
            linearToSrgb(beeB)
        )
    }

    @Test
    fun testDogPureRedBecomesYellowBrown() {
        // Pure Red: (1.0, 0.0, 0.0) -> Canine vision maps it to yellow-brown (no red dominance)
        val (r, g, b) = simulateDogShader(1f, 0f, 0f)
        println("Dog Pure Red (1.0, 0.0, 0.0) -> ($r, $g, $b)")
        assertTrue("Red and Green should be balanced (yellow hue)", kotlin.math.abs(r - g) < 0.05f)
        assertTrue("R should be moderate yellow-brown intensity", r > 0.75f && r < 0.90f)
        assertTrue("Blue component should be significantly lower than red and green", b < r - 0.25f)
    }

    @Test
    fun testDogPureGreenBecomesYellowBrown() {
        // Pure Green: (0.0, 1.0, 0.0) -> Canine vision maps it to muted yellow/gray-brown
        val (r, g, b) = simulateDogShader(0f, 1f, 0f)
        println("Dog Pure Green (0.0, 1.0, 0.0) -> ($r, $g, $b)")
        assertTrue("Green should not turn red; R and G should be balanced", kotlin.math.abs(r - g) < 0.05f)
        assertTrue("Both R and G should be visible (yellowish tone)", r > 0.50f && g > 0.50f)
    }

    @Test
    fun testDogPureBlueRemainsVibrantBlue() {
        // Pure Blue: (0.0, 0.0, 1.0) -> S-cones stimulated, perceived as bright blue
        val (r, g, b) = simulateDogShader(0f, 0f, 1f)
        println("Dog Pure Blue (0.0, 0.0, 1.0) -> ($r, $g, $b)")
        assertTrue("Blue should remain the dominant color channel", b > 0.65f)
        assertTrue("Red and Green should be much lower than blue", r < 0.25f && g < 0.25f)
    }

    @Test
    fun testDogPureYellowRemainsVibrantYellow() {
        // Pure Yellow: (1.0, 1.0, 0.0) -> Stimulates M/L-cones fully
        val (r, g, b) = simulateDogShader(1f, 1f, 0f)
        println("Dog Pure Yellow (1.0, 1.0, 0.0) -> ($r, $g, $b)")
        assertTrue("Yellow should produce high R and G response", r > 0.85f && g > 0.85f)
    }

    @Test
    fun testDeerBlazeOrangeAttenuated() {
        // Hunter Blaze Orange: High R (1.0), moderate G (0.35), zero B (0.0)
        val (r, g, b) = simulateDeerShader(1.0f, 0.35f, 0.0f)
        println("Blaze Orange (1.0, 0.35, 0.0) -> Deer ($r, $g, $b)")
        assertTrue("Blaze Orange in Deer vision should be muted brownish/grayish", r < 0.8f)
        assertTrue("Blaze Orange should have lower blue than green/red", b < r && b < g)
    }

    @Test
    fun testDeerBlueEnhanced() {
        // Blue Jeans: Low R (0.0), low G (0.2), high B (0.9)
        val (r, g, b) = simulateDeerShader(0.0f, 0.2f, 0.9f)
        println("Blue Jeans (0.0, 0.2, 0.9) -> Deer ($r, $g, $b)")
        assertTrue("Blue in Deer vision should remain highly vibrant", b > 0.8f)
    }

    @Test
    fun testBeeRedBlindness() {
        // Pure Red: (1.0, 0.0, 0.0)
        val (r, g, b) = simulateBeeShader(1.0f, 0.0f, 0.0f)
        println("Pure Red (1.0, 0.0, 0.0) -> Bee ($r, $g, $b)")
        assertTrue("Pure red should be completely dark/black to honeybee", r < 0.15f && g < 0.15f && b < 0.15f)
    }

    private fun simulateCatShader(r: Float, g: Float, b: Float): Triple<Float, Float, Float> {
        val linR = srgbToLinear(r)
        val linG = srgbToLinear(g)
        val linB = srgbToLinear(b)
        val luminance = 0.2126f * linR + 0.7152f * linG + 0.0722f * linB
        val boost = 3.5f * exp(-5.0f * luminance)
        val linBoostedR = (linR * (1.0f + boost)).coerceIn(0f, 1f)
        val linBoostedG = (linG * (1.0f + boost)).coerceIn(0f, 1f)
        val linBoostedB = (linB * (1.0f + boost)).coerceIn(0f, 1f)

        val catR = (0.65f * linBoostedG + 0.30f * linBoostedR).coerceIn(0f, 1f)
        val catG = (0.80f * linBoostedG + 0.18f * linBoostedB).coerceIn(0f, 1f)
        val catB = (0.88f * linBoostedB + 0.12f * linBoostedG).coerceIn(0f, 1f)

        val linDesatR = (1.0f - 0.40f) * luminance + 0.40f * catR
        val linDesatG = (1.0f - 0.40f) * luminance + 0.40f * catG
        val linDesatB = (1.0f - 0.40f) * luminance + 0.40f * catB

        return Triple(
            linearToSrgb(linDesatR.coerceIn(0f, 1f)),
            linearToSrgb(linDesatG.coerceIn(0f, 1f)),
            linearToSrgb(linDesatB.coerceIn(0f, 1f))
        )
    }

    private fun simulateBirdShader(r: Float, g: Float, b: Float): Triple<Float, Float, Float> {
        val linR = srgbToLinear(r)
        val linG = srgbToLinear(g)
        val linB = srgbToLinear(b)
        val luminance = 0.2126f * linR + 0.7152f * linG + 0.0722f * linB
        val uvSignal = maxOf(0f, linB - 0.5f * (linR + linG)) * 2.0f + 0.15f * luminance
        val uvGlowR = 0.38f * uvSignal
        val uvGlowG = 0.05f * uvSignal
        val uvGlowB = 0.68f * uvSignal

        val meanColor = (linR + linG + linB) / 3.0f
        val hyperR = (meanColor + (linR - meanColor) * 1.35f).coerceIn(0f, 1f)
        val hyperG = (meanColor + (linG - meanColor) * 1.35f).coerceIn(0f, 1f)
        val hyperB = (meanColor + (linB - meanColor) * 1.35f).coerceIn(0f, 1f)

        val finalR = (hyperR + uvGlowR).coerceIn(0f, 1f)
        val finalG = (hyperG + uvGlowG).coerceIn(0f, 1f)
        val finalB = (hyperB + uvGlowB).coerceIn(0f, 1f)

        return Triple(
            linearToSrgb(finalR),
            linearToSrgb(finalG),
            linearToSrgb(finalB)
        )
    }

    private fun simulateSnakeShader(r: Float, g: Float, b: Float): Triple<Float, Float, Float> {
        val linR = srgbToLinear(r)
        val linG = srgbToLinear(g)
        val linB = srgbToLinear(b)
        val luminance = 0.2126f * linR + 0.7152f * linG + 0.0722f * linB

        val snakeVisR = (0.75f * linG + 0.20f * linR).coerceIn(0f, 1f)
        val snakeVisG = (0.85f * linG + 0.15f * linB).coerceIn(0f, 1f)
        val snakeVisB = (0.90f * linB).coerceIn(0f, 1f)

        val heat = (1.4f * luminance + 0.7f * maxOf(0f, linR - linG) - 0.20f).coerceIn(0f, 1f)
        val (thermalR, thermalG, thermalB) = when {
            heat < 0.33f -> {
                val f = heat / 0.33f
                Triple((1f - f) * 0.04f + f * 0.65f, 0.0f, (1f - f) * 0.28f + f * 0.55f)
            }
            heat < 0.66f -> {
                val f = (heat - 0.33f) / 0.33f
                Triple((1f - f) * 0.65f + f * 1.0f, f * 0.45f, (1f - f) * 0.55f)
            }
            else -> {
                val f = (heat - 0.66f) / 0.34f
                Triple(1.0f, (1f - f) * 0.45f + f * 1.0f, f * 0.85f)
            }
        }

        val finalR = (1f - 0.55f) * snakeVisR + 0.55f * thermalR
        val finalG = (1f - 0.55f) * snakeVisG + 0.55f * thermalG
        val finalB = (1f - 0.55f) * snakeVisB + 0.55f * thermalB

        return Triple(
            linearToSrgb(finalR.coerceIn(0f, 1f)),
            linearToSrgb(finalG.coerceIn(0f, 1f)),
            linearToSrgb(finalB.coerceIn(0f, 1f))
        )
    }

    @Test
    fun testTapetumLucidumBoostsLowLight() {
        val (r, _, _) = simulateDogShader(0.05f, 0.05f, 0.05f)
        assertTrue("Low light input should be visibly boosted by tapetum simulation", r > 0.05f)
    }

    @Test
    fun testCatPastelSoftening() {
        // Pure Red in Cat vision: desaturated with rod luminance dominance
        val (r, g, b) = simulateCatShader(1.0f, 0.0f, 0.0f)
        println("Cat Pure Red -> ($r, $g, $b)")
        assertTrue("Cat vision should show muted red response", r < 0.85f)
        assertTrue("Cat vision should include significant green/rod component", g > 0.30f)
    }

    @Test
    fun testBirdHyperChromaAndUvGlow() {
        // Pure Blue with high UV excitation
        val (r, g, b) = simulateBirdShader(0.0f, 0.0f, 1.0f)
        println("Bird Pure Blue -> ($r, $g, $b)")
        assertTrue("Bird vision should maintain strong blue", b > 0.80f)
        assertTrue("Bird UV false-color should induce violet/magenta R component", r > 0.40f)
    }

    @Test
    fun testSnakeThermalOverlayResponse() {
        // High thermal heat input (bright red warm body)
        val (r, g, b) = simulateSnakeShader(1.0f, 0.2f, 0.0f)
        println("Snake Warm Body -> ($r, $g, $b)")
        assertTrue("Snake thermal overlay should produce high radiant heat (red/orange)", r > 0.70f)
    }
}
