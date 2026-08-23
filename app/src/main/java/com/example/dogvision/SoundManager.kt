package com.example.dogvision

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import java.util.concurrent.atomic.AtomicBoolean

class SoundManager private constructor(context: Context) {
    private val soundPool: SoundPool
    private val soundIds = mutableListOf<Int>()
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

        val rawResIds = listOf(R.raw.dog_bark_1, R.raw.dog_bark_2, R.raw.dog_bark_3)
        var loadCount = 0
        soundPool.setOnLoadCompleteListener { _, _, status ->
            if (status == 0) {
                loadCount++
                if (loadCount >= rawResIds.size) {
                    isLoaded.set(true)
                }
            }
        }

        for (resId in rawResIds) {
            val id = soundPool.load(context.applicationContext, resId, 1)
            soundIds.add(id)
        }
    }

    fun playAnimalSound(animalId: String = "dog") {
        if (soundIds.isEmpty()) return
        val randomSoundId = soundIds.random()
        val pitch = when (animalId) {
            "deer" -> 0.70f + (Math.random() * 0.2f).toFloat() // Lower pitch grunt/bleat tone
            "cat" -> 1.30f + (Math.random() * 0.3f).toFloat()  // Higher pitch
            "bird" -> 1.50f + (Math.random() * 0.4f).toFloat() // High pitch chirp/cry
            "bee" -> 0.60f + (Math.random() * 0.15f).toFloat() // Low frequency buzz tone
            "snake" -> 0.80f + (Math.random() * 0.2f).toFloat()
            else -> (0.85f + Math.random() * 0.5f).toFloat()    // Dog bark pitch
        }
        soundPool.play(randomSoundId, 1.0f, 1.0f, 1, 0, pitch)
    }

    fun playRandomBark() {
        playAnimalSound("dog")
    }

    fun release() {
        soundPool.release()
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
