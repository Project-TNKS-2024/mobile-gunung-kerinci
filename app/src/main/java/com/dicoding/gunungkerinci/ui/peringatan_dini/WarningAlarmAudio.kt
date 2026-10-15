package com.dicoding.gunungkerinci.ui.peringatan_dini

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator

/** Pemutar milik service: tidak bergantung pada lifecycle layar/popup. */
internal class WarningAlarmAudio(private val context: Context) {
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val vibrator = context.getSystemService(Vibrator::class.java)
    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
    private var player: MediaPlayer? = null
    private var focusRequest: AudioFocusRequest? = null
    private var prepared = false
    private var hasFocus = false
    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                hasFocus = false
                if (prepared && player?.isPlaying == true) player?.pause()
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                hasFocus = true
                if (prepared) player?.start()
            }
        }
    }

    fun start() {
        stop()
        val focus = if (Build.VERSION.SDK_INT >= 26) {
            AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(attributes).setOnAudioFocusChangeListener(focusListener)
                .build().also { focusRequest = it }.let(audioManager::requestAudioFocus)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(focusListener, AudioManager.STREAM_ALARM, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
        }
        if (focus == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            hasFocus = true
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            if (uri != null) {
                val audio = MediaPlayer()
                player = audio
                runCatching {
                    audio.setAudioAttributes(attributes)
                    audio.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)
                    audio.setDataSource(context, uri)
                    audio.isLooping = true
                    audio.setOnPreparedListener {
                        if (player === it) {
                            prepared = true
                            if (hasFocus) it.start()
                        }
                    }
                    audio.setOnErrorListener { media, _, _ ->
                        if (player === media) { media.release(); player = null; prepared = false }
                        true
                    }
                    audio.prepareAsync()
                }.onFailure { audio.release(); player = null; prepared = false }
            }
        }
        if (vibrator?.hasVibrator() == true) {
            val pattern = longArrayOf(0, 700, 350, 700, 1200)
            if (Build.VERSION.SDK_INT >= 26) vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0), attributes)
            else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, 0, attributes)
            }
        }
    }

    fun stop() {
        prepared = false
        hasFocus = false
        player?.release()
        player = null
        vibrator?.cancel()
        if (Build.VERSION.SDK_INT >= 26) focusRequest?.let(audioManager::abandonAudioFocusRequest)
        else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(focusListener)
        }
        focusRequest = null
    }
}
