package com.example.dogvision

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import java.util.concurrent.atomic.AtomicBoolean

class SoundManager private constructor(context: Context) {
    private val soundPool: SoundPool
    private val barkSoundIds = mutableListOf<Int>()
    private val burpSoundIds = mutableListOf<Int>()
    private val isLoaded = AtomicBoolean(false)

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(audioAttributes)
            .build()

        val barkResIds = listOf(R.raw.dog_bark_1, R.raw.dog_bark_2, R.raw.dog_bark_3)
        val burpResIds = listOf(R.raw.burp_1, R.raw.burp_2, R.raw.burp_3)
        val totalSounds = barkResIds.size + burpResIds.size
        var loadCount = 0

        soundPool.setOnLoadCompleteListener { _, _, status ->
            if (status == 0) {
                loadCount++
                if (loadCount >= totalSounds) {
                    isLoaded.set(true)
                }
            }
        }

        for (resId in barkResIds) {
            val id = soundPool.load(context.applicationContext, resId, 1)
            barkSoundIds.add(id)
        }

        for (resId in burpResIds) {
            val id = soundPool.load(context.applicationContext, resId, 1)
            burpSoundIds.add(id)
        }
    }

    fun playAnimalSound(animalId: String = "dog") {
        try {
            if (animalId == "drunk_human") {
                playBurpSound()
                return
            }

            if (barkSoundIds.isEmpty()) return
            val randomSoundId = barkSoundIds.random()
            val pitch = when (animalId) {
                "deer" -> 0.70f + (Math.random() * 0.2f).toFloat() // Lower pitch grunt/bleat tone
                "cat" -> 1.30f + (Math.random() * 0.3f).toFloat()  // Higher pitch
                "bird" -> 1.50f + (Math.random() * 0.4f).toFloat() // High pitch chirp/cry
                "bee" -> 0.60f + (Math.random() * 0.15f).toFloat() // Low frequency buzz tone
                "snake" -> 0.80f + (Math.random() * 0.2f).toFloat()
                else -> (0.85f + Math.random() * 0.5f).toFloat()    // Dog bark pitch
            }
            soundPool.play(randomSoundId, 1.0f, 1.0f, 1, 0, pitch)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Plays authentic real recorded burp sound effects with subtle natural pitch variation.
     */
    fun playBurpSound() {
        try {
            if (burpSoundIds.isEmpty()) return
            val randomBurpId = burpSoundIds.random()
            val pitch = (0.90f + Math.random() * 0.20f).toFloat() // 0.90 - 1.10 natural pitch
            soundPool.play(randomBurpId, 1.0f, 1.0f, 1, 0, pitch)
        } catch (e: Exception) {
            e.printStackTrace()
        }
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

        fun getInstance(context: Context): SoundManager {
            return instance ?: synchronized(this) {
                instance ?: SoundManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
