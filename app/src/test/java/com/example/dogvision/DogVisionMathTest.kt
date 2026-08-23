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

        val rPrime = (0.84095f * linBoostedR + 2.92281f * linBoostedG - 2.76374f * linBoostedB).coerceIn(0f, 1f)
        val gPrime = (0.03561f * linBoostedR + 0.13067f * linBoostedG + 0.84045f * linBoostedB).coerceIn(0f, 1f)
        val bPrime = (0.00579f * linBoostedR + 0.02723f * linBoostedG + 0.96602f * linBoostedB).coerceIn(0f, 1f)

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
    fun testDogColorSimulation() {
        val (r, g, b) = simulateDogShader(1f, 0f, 0f)
        assertTrue("Red should have significant R component", r > 0.6f)
        assertTrue("Red should have some G component (yellowish)", g > 0.1f)
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

    @Test
    fun testTapetumLucidumBoostsLowLight() {
        val (r, _, _) = simulateDogShader(0.05f, 0.05f, 0.05f)
        assertTrue("Low light input should be visibly boosted by tapetum simulation", r > 0.05f)
    }
}
