package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.FinanceViewModel

@Composable
fun PinLockScreen(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val isLocked by viewModel.isAppUnlocked.collectAsState()
    val correctPinHash by viewModel.savedPinHash.collectAsState()

    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    if (!isLocked) {
        Surface(
            modifier = modifier.fillMaxSize().testTag("pin_lock_screen"),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                // Header
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Pin Locked",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "NHẬP MÃ PIN",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Vui lòng nhập mã PIN bảo mật để tiếp tục",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }

                // Password Indicator Dots
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(4) { index ->
                            val isFilled = index < enteredPin.length
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isFilled) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
                                    )
                            )
                        }
                    }

                    if (errorMessage.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Keyboard Numpad
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    val keys = listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9"),
                        listOf("Xóa", "0", "Back")
                    )

                    for (row in keys) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (key in row) {
                                KeypadButton(
                                    label = key,
                                    onClick = {
                                        when (key) {
                                            "Xóa" -> {
                                                enteredPin = ""
                                                errorMessage = ""
                                            }
                                            "Back" -> {
                                                if (enteredPin.isNotEmpty()) {
                                                    enteredPin = enteredPin.dropLast(1)
                                                    errorMessage = ""
                                                }
                                            }
                                            else -> {
                                                if (enteredPin.length < 4) {
                                                    enteredPin += key
                                                    errorMessage = ""
                                                    if (enteredPin.length == 4) {
                                                        val isOk = viewModel.unlockApp(enteredPin)
                                                        if (!isOk) {
                                                            errorMessage = "Mã PIN không đúng, thử lại!"
                                                            enteredPin = ""
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KeypadButton(
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(
                if (label == "Xóa" || label == "Back") Color.Transparent
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
            .clickable { onClick() }
            .testTag("keypad_$label"),
        contentAlignment = Alignment.Center
    ) {
        if (label == "Back") {
            Icon(
                imageVector = Icons.Default.Backspace,
                contentDescription = "Backspace",
                tint = MaterialTheme.colorScheme.onSurface
            )
        } else {
            Text(
                text = label,
                fontSize = if (label == "Xóa") 15.sp else 24.sp,
                fontWeight = FontWeight.Bold,
                color = if (label == "Xóa") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
