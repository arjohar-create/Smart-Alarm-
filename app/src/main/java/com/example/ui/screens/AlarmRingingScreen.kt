package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.math.MathProblem
import com.example.math.MathProblemGenerator
import com.example.ui.theme.ClockDigitActive
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun AlarmRingingScreen(
    alarmLabel: String,
    mathDifficulty: String,
    targetProblemCount: Int = 1,
    onAlarmDismissed: () -> Unit,
    onSnooze: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var currentProblemIndex by remember { mutableIntStateOf(1) }
    var mathProblem by remember { mutableStateOf(MathProblemGenerator.generate(mathDifficulty)) }
    var userInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSolvedSuccess by remember { mutableStateOf(false) }

    // Shake animation on error
    val shakeOffset = remember { Animatable(0f) }

    // Pulse animation for ringing alert
    val infiniteTransition = rememberInfiniteTransition(label = "ringing_anim")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val currentTime = remember { timeFormat.format(Date()) }

    fun handleKeypad(key: String) {
        errorMessage = null
        when (key) {
            "DEL" -> {
                if (userInput.isNotEmpty()) {
                    userInput = userInput.dropLast(1)
                }
            }
            "CLEAR" -> {
                userInput = ""
            }
            "-" -> {
                if (userInput.isEmpty()) {
                    userInput = "-"
                }
            }
            "ENTER" -> {
                val entered = userInput.toIntOrNull()
                if (entered == mathProblem.solution) {
                    if (currentProblemIndex >= targetProblemCount) {
                        // All problems solved! Dismiss alarm!
                        isSolvedSuccess = true
                        coroutineScope.launch {
                            delay(800)
                            onAlarmDismissed()
                        }
                    } else {
                        currentProblemIndex++
                        userInput = ""
                        mathProblem = MathProblemGenerator.generate(mathDifficulty)
                    }
                } else {
                    errorMessage = "Incorrect! Try again."
                    coroutineScope.launch {
                        shakeOffset.animateTo(
                            targetValue = 0f,
                            animationSpec = tween(50)
                        )
                        // Simple 3-cycle shake
                        for (i in 0..2) {
                            shakeOffset.animateTo(25f, tween(40))
                            shakeOffset.animateTo(-25f, tween(40))
                        }
                        shakeOffset.animateTo(0f, tween(40))
                    }
                    userInput = ""
                }
            }
            else -> {
                if (userInput.length < 6) {
                    userInput += key
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = if (isSolvedSuccess) {
                        listOf(Color(0xFF003820), DarkBackground)
                    } else {
                        listOf(Color(0xFF380010), DarkBackground)
                    }
                )
            )
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Pulsing Alarm Icon + Ringing waves
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(if (isSolvedSuccess) NeonEmerald.copy(alpha = 0.25f) else NeonRed.copy(alpha = 0.25f))
                )

                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(if (isSolvedSuccess) NeonEmerald else NeonRed),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isSolvedSuccess) Icons.Default.Check else Icons.Default.NotificationsActive,
                        contentDescription = "Ringing alarm",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Alarm Label & Current Time
            Text(
                text = if (isSolvedSuccess) "WAKE UP SUCCESSFUL!" else "ALARM RINGING!",
                style = MaterialTheme.typography.titleLarge,
                color = if (isSolvedSuccess) NeonEmerald else NeonRed,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )

            Text(
                text = alarmLabel,
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = currentTime,
                fontSize = 18.sp,
                fontFamily = FontFamily.Monospace,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Math Challenge Card
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                modifier = Modifier
                    .testTag("math_challenge_card")
                    .fillMaxWidth()
                    .offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
                    .border(
                        2.dp,
                        if (isSolvedSuccess) NeonEmerald else NeonPurple,
                        RoundedCornerShape(24.dp)
                    )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = if (isSolvedSuccess) "GREAT JOB! YOU'RE AWAKE!" else "SOLVE TO STOP ALARM",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isSolvedSuccess) NeonEmerald else NeonAmber,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Equation Display: e.g. 15 + 7 = ?
                    Text(
                        text = "${mathProblem.expression} = ?",
                        fontSize = 44.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        color = ClockDigitActive,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // User Input Box
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth(0.75f)
                            .height(56.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF0D131D))
                            .border(
                                1.5.dp,
                                if (errorMessage != null) NeonRed else NeonCyan,
                                RoundedCornerShape(14.dp)
                            )
                    ) {
                        Text(
                            text = if (userInput.isEmpty()) "Tap numbers below" else userInput,
                            fontSize = if (userInput.isEmpty()) 16.sp else 28.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (userInput.isEmpty()) TextMuted else TextPrimary
                        )
                    }

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = NeonRed,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Numeric Keypad
            NumericKeypad(
                onKeyClick = { key -> handleKeypad(key) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Snooze Option
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onSnooze,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = NeonAmber
                    ),
                    modifier = Modifier
                        .testTag("snooze_alarm_button")
                        .height(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Snooze,
                        contentDescription = "Snooze",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Snooze 5 Min",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun NumericKeypad(
    onKeyClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("CLEAR", "0", "DEL")
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
    ) {
        for (row in rows) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                for (key in row) {
                    KeypadButton(
                        key = key,
                        onClick = { onKeyClick(key) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Submit Button
        Button(
            onClick = { onKeyClick("ENTER") },
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = NeonEmerald,
                contentColor = DarkBackground
            ),
            modifier = Modifier
                .testTag("submit_math_answer_btn")
                .fillMaxWidth()
                .height(54.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Submit",
                    tint = DarkBackground,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SUBMIT & STOP ALARM",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun KeypadButton(
    key: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isSpecial = key == "DEL" || key == "CLEAR"
    val bgColor = if (isSpecial) Color(0xFF1E293B) else DarkCard
    val textColor = when (key) {
        "DEL" -> NeonRed
        "CLEAR" -> NeonAmber
        else -> TextPrimary
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .testTag("keypad_$key")
            .height(54.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(1.dp, DarkCardBorder, RoundedCornerShape(14.dp))
            .clickable { onClick() }
    ) {
        if (key == "DEL") {
            Icon(
                imageVector = Icons.Default.Backspace,
                contentDescription = "Delete",
                tint = NeonRed,
                modifier = Modifier.size(22.dp)
            )
        } else {
            Text(
                text = key,
                fontSize = if (key.length > 2) 13.sp else 22.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = if (key.length <= 2) FontFamily.Monospace else FontFamily.SansSerif,
                color = textColor
            )
        }
    }
}
