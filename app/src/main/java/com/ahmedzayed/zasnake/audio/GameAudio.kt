package com.ahmedzayed.zasnake.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.SoundPool
import android.speech.tts.TextToSpeech
import java.util.Locale
import android.os.Build
import android.os.SystemClock
import com.ahmedzayed.zasnake.R
import com.ahmedzayed.zasnake.game.AppSettings

/** Local, offline sound assets. Short effects use SoundPool, long looping music uses MediaPlayer. */
enum class SoundEffect { EAT, GOLDEN, SCISSORS, TURN, LOSE, RECORD, RECORD_EN, LEVEL, CLAIM }

class GameAudio(context: Context) {
    private var settings = AppSettings()
    private var released = false
    private var shouldPlayMusic = false
    private var lastTurnAt = 0L

    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val soundPool = SoundPool.Builder().setMaxStreams(4).setAudioAttributes(attributes).build()
    private val soundIds = mutableMapOf<SoundEffect, Int>()
    private val loaded = mutableSetOf<Int>()
    private var player: MediaPlayer? = null
    private var hasFocus = false
    private var speech: TextToSpeech? = null
    private var speechReady = false

    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                hasFocus = true
                if (shouldPlayMusic) startMusic()
            }
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                hasFocus = false
                pauseMusic()
            }
        }
    }
    private val focusRequest: AudioFocusRequest? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(attributes)
            .setOnAudioFocusChangeListener(focusListener)
            .build()
    } else null

    init {
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (!released && status == 0) loaded.add(sampleId)
        }
        soundIds[SoundEffect.EAT] = soundPool.load(context, R.raw.za_eat, 1)
        soundIds[SoundEffect.GOLDEN] = soundPool.load(context, R.raw.za_golden, 1)
        soundIds[SoundEffect.SCISSORS] = soundPool.load(context, R.raw.za_scissors, 1)
        soundIds[SoundEffect.TURN] = soundPool.load(context, R.raw.za_turn, 1)
        soundIds[SoundEffect.LOSE] = soundPool.load(context, R.raw.za_lose, 1)
        soundIds[SoundEffect.RECORD] = soundPool.load(context, R.raw.za_record, 1)
        soundIds[SoundEffect.RECORD_EN] = soundPool.load(context, R.raw.za_record_voice_en, 1)
        soundIds[SoundEffect.LEVEL] = soundPool.load(context, R.raw.za_level, 1)
        soundIds[SoundEffect.CLAIM] = soundPool.load(context, R.raw.za_claim, 1)
    }

    fun applySettings(next: AppSettings) {
        if (released) return
        settings = next
        player?.let { p -> runCatching { p.setVolume(musicGain(), musicGain()) } }
        if (!settings.musicEnabled || settings.musicVolume == 0) {
            syncMusic(false)
        } else if (shouldPlayMusic) {
            syncMusic(true)
        }
    }

    fun play(effect: SoundEffect) {
        if (released || !settings.soundEnabled || settings.sfxVolume == 0) return
        if (effect == SoundEffect.TURN) {
            val now = SystemClock.elapsedRealtime()
            if (now - lastTurnAt < 90) return
            lastTurnAt = now
        }
        val sample = soundIds[effect] ?: return
        if (sample !in loaded) return
        val gain = (settings.sfxVolume.coerceIn(0, 100) / 100f *
            if (effect == SoundEffect.SCISSORS) 1.12f else 1f).coerceAtMost(1f)
        runCatching { soundPool.play(sample, gain, gain, 1, 0, 1f) }
    }

    /** Prefer device-local speech; if no offline voice exists, use the original record chime. */
    fun announceRecord(language: String) {
        if (released || !settings.soundEnabled || settings.sfxVolume == 0) return
        if (language != "ar") {
            play(SoundEffect.RECORD_EN) // Offline, works on the first new record.
            return
        }
        val locale = Locale("ar")
        val engine = speech
        if (!speechReady || engine == null) {
            play(SoundEffect.RECORD)
            if (engine == null) {
                speech = TextToSpeech(appContext) { result ->
                    speechReady = result == TextToSpeech.SUCCESS
                }
            }
            return
        }
        val localVoice = engine.voices?.firstOrNull { voice ->
            !voice.isNetworkConnectionRequired && voice.locale.language == locale.language
        }
        if (localVoice == null) {
            play(SoundEffect.RECORD)
            return
        }
        engine.voice = localVoice
        engine.setSpeechRate(1.08f)
        val text = if (language == "ar") "برافو! رقم قياسي جديد!" else "Bravo! New Record!"
        engine.speak(text, TextToSpeech.QUEUE_FLUSH,
            android.os.Bundle().apply {
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, settings.sfxVolume.coerceIn(0, 100) / 100f)
            }, "record_voice")
    }

    fun syncMusic(activeRound: Boolean) {
        if (released) return
        shouldPlayMusic = activeRound && settings.musicEnabled && settings.musicVolume > 0
        if (!shouldPlayMusic) {
            pauseMusic()
            abandonFocus()
            return
        }
        if (!hasFocus && !requestFocus()) return
        startMusic()
    }

    private fun musicGain(): Float = settings.musicVolume.coerceIn(0, 100) / 100f

    private fun startMusic() {
        if (released || !hasFocus || !shouldPlayMusic) return
        runCatching {
            if (player == null) {
                player = MediaPlayer.create(appContext, R.raw.za_ambient_loop)?.apply {
                    isLooping = true
                }
            }
            player?.apply {
                setVolume(musicGain(), musicGain())
                if (!isPlaying) start()
            }
        }
    }

    private val appContext = context.applicationContext

    private fun pauseMusic() {
        runCatching { player?.let { if (it.isPlaying) it.pause() } }
    }

    @Suppress("DEPRECATION")
    private fun requestFocus(): Boolean {
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.requestAudioFocus(focusRequest!!)
        } else {
            audioManager.requestAudioFocus(focusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        }
        hasFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        return hasFocus
    }

    @Suppress("DEPRECATION")
    private fun abandonFocus() {
        if (!hasFocus) return
        hasFocus = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.abandonAudioFocusRequest(focusRequest!!)
        } else {
            audioManager.abandonAudioFocus(focusListener)
        }
    }

    fun release() {
        if (released) return
        syncMusic(false)
        released = true
        runCatching { player?.release() }
        player = null
        runCatching { speech?.stop(); speech?.shutdown() }
        speech = null
        speechReady = false
        runCatching { soundPool.release() }
        loaded.clear()
    }
}
