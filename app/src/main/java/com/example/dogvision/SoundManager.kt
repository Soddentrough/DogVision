package com.example.dogvision

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import kotlin.random.Random

/**
 * Low-latency species vocalization playback backed by [SoundPool].
 *
 * Each species has its own recorded/synthesized asset; small pitch jitter keeps
 * repeated playback from sounding mechanical.
 *
 * Lifecycle: app-scoped singleton. Deliberately NOT released from Activity
 * onDestroy() — releasing a process singleton on a configuration change would
 * permanently kill audio for the remaining process lifetime. The OS reclaims
 * the SoundPool at process death; [release] exists for explicit teardown tests.
 */
class SoundManager private constructor(context: Context) {
    private val soundPool: SoundPool
    private val speciesSounds: Map<String, List<Int>>

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(audioAttributes)
            .build()

        val appContext = context.applicationContext
        speciesSounds = mapOf(
            "dog" to listOf(R.raw.dog_bark_1, R.raw.dog_bark_2, R.raw.dog_bark_3),
            "deer" to listOf(R.raw.deer_bleat_1),
            "cat" to listOf(R.raw.cat_meow_1),
            "hawk" to listOf(R.raw.bird_chirp_1),
            "bird" to listOf(R.raw.bird_chirp_1),
            "bee" to listOf(R.raw.bee_buzz_1),
            "snake" to listOf(R.raw.snake_hiss_1),
            "drunk_human" to listOf(R.raw.burp_1, R.raw.burp_2, R.raw.burp_3),
        ).mapValues { (_, resIds) -> resIds.map { soundPool.load(appContext, it, 1) } }
    }

    fun playAnimalSound(animalId: String = "dog") {
        try {
            val ids = speciesSounds[animalId] ?: speciesSounds["dog"] ?: return
            val soundId = ids.random()
            val pitch = when (animalId) {
                "deer" -> 0.92f + Random.nextFloat() * 0.16f
                "cat" -> 0.90f + Random.nextFloat() * 0.25f
                "hawk", "bird" -> 0.88f + Random.nextFloat() * 0.30f
                "bee" -> 0.90f + Random.nextFloat() * 0.20f
                "snake" -> 0.88f + Random.nextFloat() * 0.24f
                "drunk_human" -> 0.90f + Random.nextFloat() * 0.20f
                else -> 0.85f + Random.nextFloat() * 0.30f // dog
            }
            soundPool.play(soundId, 1.0f, 1.0f, 1, 0, pitch)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playBurpSound() {
        playAnimalSound("drunk_human")
    }

    fun playRandomBark() {
        playAnimalSound("dog")
    }

    fun release() {
        try {
            soundPool.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        synchronized(SoundManager::class.java) {
            instance = null
        }
    }

    companion object {
        @Volatile
        private var instance: SoundManager? = null

        fun getInstance(context: Context): SoundManager =
            instance ?: synchronized(this) {
                instance ?: SoundManager(context.applicationContext).also { instance = it }
            }
    }
}
