package org.scrollbound.app

import android.content.Intent
import android.os.Bundle
import android.os.VibrationEffect
import android.os.VibratorManager
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val Ink = Color(0xFF101B2A)
private val Mint = Color(0xFF96E9C2)
private val Paper = Color(0xFFF5F1E8)

private data class Step(val title: String, val hint: String, val options: List<String> = emptyList())

private val steps = listOf(
    Step("Make room for your day", "Get a clearer picture of your short-video habits and choose when to take a break."),
    Step("Where do you usually scroll?", "We’ll start by tracking YouTube Shorts.", listOf("YouTube Shorts", "Instagram Reels", "TikTok", "Somewhere else")),
    Step("When do you reach for your phone?", "Choose the part of the day you want to pay attention to.", listOf("After waking up", "Between tasks", "After dinner", "Before sleep")),
    Step("How much time goes to scrolling?", "A rough guess gives you a starting point.", listOf("Under 30 minutes", "30–60 minutes", "1–3 hours", "More than 3 hours")),
    Step("How do you feel afterward?", "Think about how a typical session ends.", listOf("Relaxed", "Distracted", "Restless", "Tired")),
    Step("What fills most of your day?", "Choose the routine that best fits right now.", listOf("School", "College", "Work", "Something else")),
    Step("What would you make more time for?", "Pick one thing you want to get back to.", listOf("Sleep", "Study", "Exercise", "Family", "Quiet time")),
    Step("Set a daily cap", "Choose how many Shorts you want to watch in a day.", listOf("25", "50", "100", "150")),
    Step("Choose how reminders feel", "Preview a short vibration sequence. You can switch haptics off anytime."),
    Step("Connect Shorts tracking", "With your consent, Android lets Scrollbound read visible YouTube interface elements to recognize Shorts. Scrollbound saves counts on this device and discards interface text. You can disable access in Android Settings."),
    Step("Your plan is ready", "Watch a few Shorts, then come back here to check your count.")
)

class MainActivity : ComponentActivity() {
    private val prefs by lazy { getSharedPreferences("scrollbound", MODE_PRIVATE) }
    private var tracking by mutableStateOf(false)
    private var count by mutableIntStateOf(0)

    override fun onResume() {
        super.onResume()
        tracking = getSystemService(AccessibilityManager::class.java)
            .getEnabledAccessibilityServiceList(-1)
            .any { it.resolveInfo.serviceInfo.packageName == packageName }
        count = readToday(prefs)
    }

    private fun pulse(limit: Boolean = false) {
        if (!prefs.getBoolean("haptics", true)) return
        val timing = if (limit) longArrayOf(0, 75, 85, 75, 100, 125) else longArrayOf(0, 20, 52, 32)
        getSystemService(VibratorManager::class.java).defaultVibrator
            .vibrate(VibrationEffect.createWaveform(timing, -1))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = Mint, onPrimary = Ink, background = Ink, onBackground = Paper, surface = Ink, onSurface = Paper)) {
                var page by rememberSaveable { mutableIntStateOf(prefs.getInt("step", 0)) }
                var finished by rememberSaveable { mutableStateOf(prefs.getBoolean("setup", false)) }
                var haptics by rememberSaveable { mutableStateOf(prefs.getBoolean("haptics", true)) }
                var cap by rememberSaveable { mutableIntStateOf(prefs.getInt("limit", 50)) }
                var selected by rememberSaveable(page) { mutableStateOf(prefs.getString("answer.$page", if (page == 7) "$cap" else null)) }
                BackHandler(!finished && page > 0) { page--; prefs.edit().putInt("step", page).apply() }
                val scroll = rememberScrollState()
                LaunchedEffect(page, finished) { scroll.scrollTo(0) }
                Column(
                    Modifier.fillMaxSize().background(Ink).safeDrawingPadding().verticalScroll(scroll).padding(26.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Scrollbound", color = Mint, style = MaterialTheme.typography.titleMedium)
                    if (finished) {
                        Text("Today", style = MaterialTheme.typography.displaySmall)
                        Text("$count", color = Mint, style = MaterialTheme.typography.displayLarge)
                        Text("Shorts watched · $cap daily cap")
                        LinearProgressIndicator(progress = { (count.toFloat() / cap).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                        Text(if (tracking) "Shorts tracking enabled" else "Shorts tracking paused")
                        if (!tracking) OutlinedButton(onClick = { page = 9; finished = false }) { Text("Connect tracking") }
                        Text("Daily limit", style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (choice in listOf(25, 50, 100, 150)) {
                                FilterChip(selected = cap == choice, onClick = {
                                    cap = choice
                                    prefs.edit().putInt("limit", cap).putString("answer.7", "$cap").apply()
                                    pulse()
                                }, label = { Text("$choice") })
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text("Vibration")
                            Switch(checked = haptics, onCheckedChange = {
                                haptics = it
                                prefs.edit().putBoolean("haptics", it).apply()
                                if (it) pulse()
                            })
                        }
                        TextButton(onClick = { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }) { Text("Android tracking settings") }
                        return@Column
                    }
                    LinearProgressIndicator(progress = { (page + 1f) / steps.size }, modifier = Modifier.fillMaxWidth())
                    val step = steps[page]
                    Text(step.title, style = MaterialTheme.typography.headlineLarge)
                    Text(step.hint, style = MaterialTheme.typography.bodyLarge)
                    for (label in step.options) {
                        FilterChip(selected = selected == label, onClick = {
                            selected = label
                            prefs.edit().putString("answer.$page", label).apply()
                            pulse()
                        }, label = { Text(label) }, modifier = Modifier.fillMaxWidth())
                    }
                    if (page == 8) {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text("Vibration")
                            Switch(checked = haptics, onCheckedChange = {
                                haptics = it
                                prefs.edit().putBoolean("haptics", it).apply()
                                if (it) pulse()
                            })
                        }
                        OutlinedButton(enabled = haptics, onClick = { pulse(true) }) { Text("Preview limit feedback") }
                    }
                    if (page == 9) {
                        Button(onClick = {
                            pulse()
                            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        }, modifier = Modifier.fillMaxWidth()) { Text(if (tracking) "Open Android settings" else "I agree · Open Android settings") }
                    }
                    Button(
                        onClick = {
                            if (page == 7) {
                                cap = selected?.toIntOrNull() ?: 50
                                prefs.edit().putInt("limit", cap).apply()
                            }
                            pulse(page == steps.lastIndex)
                            if (page == steps.lastIndex) {
                                finished = true
                                prefs.edit().putBoolean("setup", true).apply()
                            } else {
                                page++
                                prefs.edit().putInt("step", page).apply()
                            }
                        },
                        enabled = step.options.isEmpty() || selected != null,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(when (page) { 9 -> if (tracking) "Continue" else "Set up later"; steps.lastIndex -> "Open Today"; else -> "Continue" }) }
                    if (page > 0) TextButton(onClick = { page--; prefs.edit().putInt("step", page).apply() }) { Text("Back") }
                }
            }
        }
    }
}
