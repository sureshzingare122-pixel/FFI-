package com.example.util

import android.media.AudioManager
import android.media.ToneGenerator
import android.view.HapticFeedbackConstants
import android.view.View
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class FeedbackEngine {
    private val audioScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val toneGenerator: ToneGenerator? = try {
        ToneGenerator(AudioManager.STREAM_MUSIC, 45)
    } catch (_: Throwable) {
        null
    }

    fun onHit(view: View?, hapticEnabled: Boolean, soundEnabled: Boolean, centerZone: Boolean = false) {
        if (hapticEnabled && view != null) {
            view.performHapticFeedback(
                if (centerZone) HapticFeedbackConstants.CONTEXT_CLICK else HapticFeedbackConstants.KEYBOARD_TAP
            )
        }
        if (soundEnabled) {
            playTone(if (centerZone) ToneGenerator.TONE_PROP_ACK else ToneGenerator.TONE_PROP_BEEP, 55)
        }
    }

    fun onMiss(view: View?, hapticEnabled: Boolean, soundEnabled: Boolean) {
        if (hapticEnabled && view != null) {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }
        if (soundEnabled) {
            playTone(ToneGenerator.TONE_PROP_NACK, 65)
        }
    }

    fun onSessionComplete(view: View?, hapticEnabled: Boolean, soundEnabled: Boolean) {
        if (hapticEnabled && view != null) {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
        if (soundEnabled) {
            playTone(ToneGenerator.TONE_PROP_BEEP2, 120)
        }
    }

    fun onActionTap(view: View?, hapticEnabled: Boolean, soundEnabled: Boolean) {
        if (hapticEnabled && view != null) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
        if (soundEnabled) {
            playTone(ToneGenerator.TONE_PROP_BEEP, 30)
        }
    }

    private fun playTone(toneType: Int, durationMs: Int) {
        val generator = toneGenerator ?: return
        audioScope.launch {
            try {
                generator.startTone(toneType, durationMs)
            } catch (_: Throwable) {
                // Safe no-op when audio hardware is unavailable (e.g., headless tests)
            }
        }
    }

    fun release() {
        try {
            toneGenerator?.release()
        } catch (_: Throwable) {
            // Ignore
        }
    }
}
