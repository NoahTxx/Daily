package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object HapticUtils {

    /**
     * Crisp double-tick vibration for completing all tasks or finishing countdown.
     */
    fun vibrateSuccess(context: Context) {
        try {
            val vibrator = getVibrator(context) ?: return
            if (!vibrator.hasVibrator()) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 40, 50, 60)
                val amplitudes = intArrayOf(0, 180, 0, 255)
                val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
                vibrateWithAttributes(vibrator, effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(80L)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Subtle, crisp click vibration for individual habit toggles, counters, and UI switches.
     */
    fun vibrateTick(context: Context) {
        try {
            val vibrator = getVibrator(context) ?: return
            if (!vibrator.hasVibrator()) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // 25ms at default amplitude ensures motor overcomes inertia on all devices (Nothing Phone, Pixel, Samsung)
                val effect = VibrationEffect.createOneShot(25L, VibrationEffect.DEFAULT_AMPLITUDE)
                vibrateWithAttributes(vibrator, effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(25L)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Short pulse for each elapsed second while holding down confirm buttons.
     */
    fun vibrateHoldTick(context: Context) {
        try {
            val vibrator = getVibrator(context) ?: return
            if (!vibrator.hasVibrator()) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createOneShot(18L, 200)
                vibrateWithAttributes(vibrator, effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(18L)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.O)
    private fun vibrateWithAttributes(vibrator: Vibrator, effect: VibrationEffect) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val attributes = VibrationAttributes.Builder()
                .setUsage(VibrationAttributes.USAGE_TOUCH)
                .build()
            vibrator.vibrate(effect, attributes)
        } else {
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .build()
            @Suppress("DEPRECATION")
            vibrator.vibrate(effect, audioAttributes)
        }
    }

    private fun getVibrator(context: Context): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
}
