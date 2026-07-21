package com.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.app.ui.FormatHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A beautiful, highly custom numeric calculator text field that displays starting balances / amounts
 * and seamlessly intercepts standard native touch flows to open our beautiful floating overlay instead.
 */
@Composable
fun CustomMoneyInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    placeholder: String = "0",
    autoFocus: Boolean = false,
    onDismissKeyboard: () -> Unit = {},
    testTag: String = "custom_money_input"
) {
    var showKeyboardDialog by remember { mutableStateOf(autoFocus) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .testTag("${testTag}_container")
    ) {
        // Standard Read-Only Material 3 OutlinedTextField to keep look & feel unified,
        // overlayed with a click interceptor Box to prevent standard soft-keyboard entirely!
        OutlinedTextField(
            value = if (value.isEmpty()) "" else FormatHelper.formatExpression(value),
            onValueChange = {},
            readOnly = true,
            label = { 
                val evaluated = FormatHelper.evaluateExpression(value)
                val displayLabel = if ((value.contains("+") || value.contains("-") || value.contains("×") || value.contains("÷")) && evaluated > 0.0) {
                    "$label: ${FormatHelper.formatVND(evaluated).replace(" ₫", " đ")}"
                } else {
                    label
                }
                Text(displayLabel, style = androidx.compose.ui.text.TextStyle(fontWeight = FontWeight.Bold, fontSize = 15.sp)) 
            },
            placeholder = { Text(placeholder) },
            isError = isError,
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.Calculate,
                    contentDescription = "Máy tính tài chính",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            },
            prefix = {
                Text(
                    text = "₫",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            supportingText = {
                val evaluated = FormatHelper.evaluateExpression(value)
                if ((value.contains("+") || value.contains("-") || value.contains("×") || value.contains("÷")) && evaluated > 0.0) {
                    Text(
                        text = "= ${FormatHelper.formatVND(evaluated).replace(" ₫", " đ")}",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(top = 4.dp).testTag("${testTag}_live_sum_preview")
                    )
                }
            },
            textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f),
                unfocusedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.06f),
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
            )
        )

        // Seamless overlay click capturing area
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(8.dp))
                .clickable { showKeyboardDialog = true }
                .testTag(testTag)
        )
    }

    if (showKeyboardDialog) {
        CalculatorKeyboardDialog(
            initialValue = value,
            title = label,
            onValueChange = onValueChange,
            onDismiss = {
                showKeyboardDialog = false
                onDismissKeyboard()
            }
        )
    }
}

/**
 * Professional Full-Width Custom Calculator Overlay Dialog positioned beautifully at the bottom,
 * mimicking native software keyboard behaviors accurately while fully isolating itself from
 * soft keyboard trigger conflicts.
 */
@Composable
fun CalculatorKeyboardDialog(
    initialValue: String,
    title: String,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var rawExpr by remember { mutableStateOf(initialValue) }
    val formattedExpr = remember(rawExpr) { FormatHelper.formatExpression(rawExpr) }

    // Controlled entry and exit states
    var isVisible by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        isVisible = true
    }

    // Gentle function to initiate animated dismissal
    val animateAndDismiss = {
        isVisible = false
        coroutineScope.launch {
            delay(280)
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = { animateAndDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable { animateAndDismiss() },
            contentAlignment = Alignment.BottomCenter
        ) {
            // Smooth entering slide-up animation container
            AnimatedVisibility(
                visible = isVisible,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(durationMillis = 300, easing = LinearOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(durationMillis = 200)),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(durationMillis = 280, easing = FastOutLinearInEasing)
                ) + fadeOut(animationSpec = tween(durationMillis = 200)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = false) {} // Prevent click-through
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 52.dp), // Elevated bottom padding to lift keyboard from gestural indicator beautifully
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Header Area
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = title.uppercase(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (formattedExpr.isEmpty()) "0 ₫" else "$formattedExpr ₫",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                
                                val evaluatedVal = remember(rawExpr) { FormatHelper.evaluateExpression(rawExpr) }
                                if ((rawExpr.contains("+") || rawExpr.contains("-") || rawExpr.contains("×") || rawExpr.contains("÷")) && evaluatedVal > 0.0) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "= ${FormatHelper.formatVND(evaluatedVal).replace(" ₫", " đ")}",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.testTag("kb_live_sum_val")
                                    )
                                }
                            }
                            
                            // Delete (Backspace) Button
                            IconButton(
                                onClick = {
                                    if (rawExpr.isNotEmpty()) {
                                        var temp = rawExpr
                                        if (temp.endsWith(" ")) {
                                            temp = temp.dropLast(1)
                                        }
                                        if (temp.isNotEmpty() && (temp.endsWith("+") || temp.endsWith("-") || temp.endsWith("×") || temp.endsWith("÷"))) {
                                            temp = temp.dropLast(1)
                                        }
                                        if (temp.endsWith(" ")) {
                                            temp = temp.dropLast(1)
                                        }
                                        rawExpr = if (temp.isNotEmpty()) temp.dropLast(1) else ""
                                        onValueChange(rawExpr)
                                    }
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        RoundedCornerShape(50.dp)
                                    )
                                    .clip(RoundedCornerShape(50.dp))
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                                    contentDescription = "Backspace",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // Custom Grid Layout
                        val keyRows = listOf(
                            listOf("1", "2", "3", "+"),
                            listOf("4", "5", "6", "-"),
                            listOf("7", "8", "9", "×"),
                            listOf("0", "00", "000", "÷")
                        )

                        keyRows.forEach { rowKeys ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowKeys.forEach { key ->
                                    val isOp = key in listOf("+", "-", "×", "÷")
                                    val isMultiZero = key in listOf("00", "000")

                                    val bgColor = when {
                                        isOp -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                                        isMultiZero -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                    }

                                    val textColor = when {
                                        isOp -> MaterialTheme.colorScheme.onPrimaryContainer
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(58.dp) // Adjusted height slightly to make buttons squarish like the sketch
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(bgColor)
                                            .clickable {
                                                when (key) {
                                                    "+", "-", "×", "÷" -> {
                                                        val cleanStr = rawExpr.trim()
                                                        if (cleanStr.isNotEmpty()) {
                                                            val lastChar = cleanStr.last()
                                                            rawExpr = if (lastChar == '+' || lastChar == '-' || lastChar == '×' || lastChar == '÷') {
                                                                cleanStr.dropLast(1).trim() + " " + key + " "
                                                            } else {
                                                                cleanStr + " " + key + " "
                                                            }
                                                            onValueChange(rawExpr)
                                                        }
                                                    }
                                                    "00", "000" -> {
                                                        val cleanStr = rawExpr.trim()
                                                        if (cleanStr.isNotEmpty() && cleanStr.last().isDigit()) {
                                                            rawExpr += key
                                                            onValueChange(rawExpr)
                                                        }
                                                    }
                                                    else -> {
                                                        rawExpr = if (rawExpr == "0") key else rawExpr + key
                                                        onValueChange(rawExpr)
                                                    }
                                                }
                                            }
                                            .testTag("kb_key_$key"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (key == "⌫") {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.Backspace,
                                                contentDescription = "Backspace",
                                                tint = textColor,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        } else {
                                            Text(
                                                text = key,
                                                fontSize = 20.sp, // Made font slightly larger
                                                fontWeight = FontWeight.Bold,
                                                color = textColor
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Row containing Clear (30%) and Done (70%) buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Clear (XÓA) Button - 30% width
                            Button(
                                onClick = {
                                    rawExpr = ""
                                    onValueChange("")
                                },
                                modifier = Modifier
                                    .weight(0.3f)
                                    .height(56.dp)
                                    .testTag("kb_key_Clear"),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                ),
                                elevation = ButtonDefaults.buttonElevation(
                                    defaultElevation = 4.dp,
                                    pressedElevation = 8.dp
                                )
                            ) {
                                Text(
                                    text = "XÓA",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.0.sp
                                )
                            }

                            // Done (XONG) Button - 70% width
                            Button(
                                onClick = {
                                    if (rawExpr.isNotEmpty()) {
                                        val res = FormatHelper.evaluateExpression(rawExpr)
                                        rawExpr = if (res % 1.0 == 0.0) {
                                            res.toLong().toString()
                                        } else {
                                            String.format("%.2f", res)
                                        }
                                        onValueChange(rawExpr)
                                    }
                                    animateAndDismiss()
                                },
                                modifier = Modifier
                                    .weight(0.7f)
                                    .height(56.dp)
                                    .testTag("kb_key_Done"),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                elevation = ButtonDefaults.buttonElevation(
                                    defaultElevation = 4.dp,
                                    pressedElevation = 8.dp
                                )
                            ) {
                                Text(
                                    text = "XONG",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
