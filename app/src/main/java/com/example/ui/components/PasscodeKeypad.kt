package com.example.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.KeypadButtonBg
import com.example.ui.theme.KeypadButtonBorder
import com.example.ui.theme.ShieldNavy
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.ThreatCrimson
import kotlin.math.roundToInt

@Composable
fun PasscodeKeypad(
    pinLength: Int = 4,
    currentInput: String,
    onDigitClick: (Char) -> Unit,
    onBackspaceClick: () -> Unit,
    onBiometricClick: (() -> Unit)? = null,
    isBiometricAvailable: Boolean = false,
    isError: Boolean = false,
    errorMessage: String? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val shakeOffset = remember { Animatable(0f) }

    LaunchedEffect(isError) {
        if (isError) {
            vibrateDevice(context, isError = true)
            shakeOffset.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 400
                    0f at 0
                    -25f at 50
                    25f at 100
                    -20f at 150
                    20f at 200
                    -10f at 250
                    10f at 300
                    0f at 400
                }
            )
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
    ) {
        // PIN Dot indicators
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(vertical = 16.dp)
                .testTag("pin_dots_row")
        ) {
            for (i in 0 until pinLength) {
                val isFilled = i < currentInput.length
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isError -> ThreatCrimson
                                isFilled -> CyberCyan
                                else -> Color.Transparent
                            }
                        )
                        .border(
                            width = 2.dp,
                            color = when {
                                isError -> ThreatCrimson
                                isFilled -> CyberCyan
                                else -> KeypadButtonBorder
                            },
                            shape = CircleShape
                        )
                )
            }
        }

        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = ThreatCrimson,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        } else {
            Spacer(modifier = Modifier.height(28.dp))
        }

        // Numeric Keypad Grid (1-9, Biometric/Blank, 0, Backspace)
        val rows = listOf(
            listOf('1', '2', '3'),
            listOf('4', '5', '6'),
            listOf('7', '8', '9')
        )

        rows.forEach { rowDigits ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                rowDigits.forEach { digit ->
                    KeypadDigitButton(
                        digit = digit,
                        onClick = {
                            vibrateDevice(context, isError = false)
                            onDigitClick(digit)
                        }
                    )
                }
            }
        }

        // Bottom Row: Biometric, 0, Backspace
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            // Biometric button (or empty space)
            if (isBiometricAvailable && onBiometricClick != null) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(KeypadButtonBg)
                        .border(1.dp, CyberCyan.copy(alpha = 0.6f), CircleShape)
                        .clickable {
                            vibrateDevice(context, isError = false)
                            onBiometricClick()
                        }
                        .testTag("biometric_auth_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = "Biometric Unlock",
                        tint = CyberCyan,
                        modifier = Modifier.size(34.dp)
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(72.dp))
            }

            // 0 Digit
            KeypadDigitButton(
                digit = '0',
                onClick = {
                    vibrateDevice(context, isError = false)
                    onDigitClick('0')
                }
            )

            // Backspace Button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(KeypadButtonBg)
                    .border(1.dp, KeypadButtonBorder, CircleShape)
                    .clickable {
                        vibrateDevice(context, isError = false)
                        onBackspaceClick()
                    }
                    .testTag("keypad_backspace_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Backspace,
                    contentDescription = "Backspace",
                    tint = TextPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun KeypadDigitButton(
    digit: Char,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(KeypadButtonBg)
            .border(1.dp, KeypadButtonBorder, CircleShape)
            .clickable(onClick = onClick)
            .testTag("keypad_digit_$digit")
    ) {
        Text(
            text = digit.toString(),
            color = TextPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun vibrateDevice(context: Context, isError: Boolean) {
    try {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = if (isError) {
                VibrationEffect.createWaveform(longArrayOf(0, 70, 70, 70), -1)
            } else {
                VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE)
            }
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(if (isError) 150 else 25)
        }
    } catch (e: Exception) {
        // Vibrator permission or hardware absent
    }
}
