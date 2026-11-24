package org.scrollbound.app

import android.accessibilityservice.AccessibilityService
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.VibratorManager
import android.view.accessibility.AccessibilityEvent
import java.time.LocalDate

internal fun readToday(prefs: SharedPreferences): Int =
    prefs.getInt("count.${LocalDate.now()}", 0)

class ShortsService : AccessibilityService() {
    private val prefs by lazy { getSharedPreferences("scrollbound", MODE_PRIVATE) }
    private val handler = Handler(Looper.getMainLooper())
    private var lastCandidate: Int? = null
    private var lastCounted: Int? = null
    private var pending: Runnable? = null

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.packageName?.toString() != YOUTUBE) return
        val current = candidate()
        if (current == lastCandidate) return
        cancelPending()
        lastCandidate = current
        if (current == null) return
        val confirm = Runnable {
            pending = null
            if (candidate() != current) {
                lastCandidate = null
                return@Runnable
            }
            if (current == lastCounted) return@Runnable
            lastCounted = current
            val key = "count.${LocalDate.now()}"
            val count = prefs.getInt(key, 0) + 1
            prefs.edit().putInt(key, count).apply()
            if (count == prefs.getInt("limit", 50) && prefs.getBoolean("haptics", true)) {
                getSystemService(VibratorManager::class.java).defaultVibrator.vibrate(
                    VibrationEffect.createWaveform(longArrayOf(0, 75, 85, 75, 100, 125), -1)
                )
            }
        }
        pending = confirm
        handler.postDelayed(confirm, 800)
    }

    private fun candidate(): Int? {
        val root = rootInActiveWindow ?: return null
        if (root.packageName?.toString() != YOUTUBE) return null
        if (root.findAccessibilityNodeInfosByViewId("$YOUTUBE:id/reel_recycler").none { it.isVisibleToUser }) return null
        if (root.findAccessibilityNodeInfosByViewId("$YOUTUBE:id/app_engagement_panel").any { it.isVisibleToUser }) return null
        val titles = root.findAccessibilityNodeInfosByViewId("$YOUTUBE:id/reel_title")
            .filter { it.isVisibleToUser }
            .mapNotNull { (it.text ?: it.contentDescription)?.toString()?.trim()?.takeIf(String::isNotEmpty) }
            .distinct()
        return titles.singleOrNull()?.hashCode()
    }

    override fun onInterrupt() { cancelPending(); lastCandidate = null }
    override fun onDestroy() { cancelPending(); super.onDestroy() }

    private fun cancelPending() {
        pending?.let { handler.removeCallbacks(it) }
        pending = null
    }

    private companion object { const val YOUTUBE = "com.google.android.youtube" }
}
