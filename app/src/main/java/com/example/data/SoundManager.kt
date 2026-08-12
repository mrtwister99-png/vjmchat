package com.example.data

import android.content.Context
import android.media.MediaPlayer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object SoundManager {
    private var player: MediaPlayer? = null
    
    fun play(context: Context, rawId: Int) {
        try {
            player?.stop()
            player?.release()
            player = MediaPlayer.create(context, rawId)
            player?.setOnCompletionListener { it.release(); player = null }
            player?.start()
        } catch (e: Exception) {
            player = null
        }
    }
    // secret = ticho - žádný zvuk, jen vibrace
    fun playSilent() { }

    fun vibrateSecret(context: Context) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(VibratorManager::class.java)
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Vibrator::class.java)
            }
            if (vibrator == null) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 120, 80, 120, 80, 120), -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 120, 80, 120, 80, 120), -1)
            }
        } catch (_: Exception) {}
    }
}