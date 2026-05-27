package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ColorSliderPicker(
    initialColorHex: String,
    onColorChanged: (String) -> Unit
) {
    var hue by remember { mutableStateOf(0f) }
    var saturation by remember { mutableStateOf(1f) }
    var value by remember { mutableStateOf(1f) }

    LaunchedEffect(initialColorHex) {
        try {
            if (initialColorHex.startsWith("#") && (initialColorHex.length == 7 || initialColorHex.length == 9)) {
                val parsed = android.graphics.Color.parseColor(initialColorHex)
                val hsv = FloatArray(3)
                android.graphics.Color.colorToHSV(parsed, hsv)
                hue = hsv[0]
                saturation = hsv[1]
                value = hsv[2]
            }
        } catch (_: Exception) {}
    }

    val activeColor = remember(hue, saturation, value) {
        val hsv = floatArrayOf(hue, saturation, value)
        Color(android.graphics.Color.HSVToColor(hsv))
    }

    LaunchedEffect(hue, saturation, value) {
        val argb = android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value))
        val hex = String.format("#%06X", 0xFFFFFF and argb)
        onColorChanged(hex)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Dải màu (Hue)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .clip(RoundedCornerShape(6.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp))
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                            hue = fraction * 360f
                        }
                    }
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            val fraction = (change.position.x / size.width).coerceIn(0f, 1f)
                            hue = fraction * 360f
                        }
                    }
            ) {
                val colors = listOf(
                    Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red
                )
                drawRect(
                    brush = Brush.linearGradient(
                        colors = colors,
                        start = Offset(0f, 0f),
                        end = Offset(size.width, 0f)
                    )
                )

                val thumbX = (hue / 360f) * size.width
                drawCircle(
                    color = Color.White,
                    radius = 8.dp.toPx(),
                    center = Offset(thumbX, size.height / 2),
                    style = Stroke(width = 2.dp.toPx())
                )
                drawCircle(
                    color = Color.Black,
                    radius = 9.dp.toPx(),
                    center = Offset(thumbX, size.height / 2),
                    style = Stroke(width = 1.dp.toPx())
                )
            }
        }

        Text("Độ bão hòa & Độ sáng (Saturation / Sáng tối)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(6.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp))
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(hue) {
                        detectTapGestures { offset ->
                            val fracX = (offset.x / size.width).coerceIn(0f, 1f)
                            val fracY = (offset.y / size.height).coerceIn(0f, 1f)
                            saturation = fracX
                            value = 1f - fracY
                        }
                    }
                    .pointerInput(hue) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            val fracX = (change.position.x / size.width).coerceIn(0f, 1f)
                            val fracY = (change.position.y / size.height).coerceIn(0f, 1f)
                            saturation = fracX
                            value = 1f - fracY
                        }
                    }
            ) {
                val pureHueColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f)))
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(Color.White, pureHueColor),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, 0f)
                    )
                )
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black),
                        startY = 0f,
                        endY = size.height
                    )
                )

                val thumbX = saturation * size.width
                val thumbY = (1f - value) * size.height
                drawCircle(
                    color = Color.White,
                    radius = 8.dp.toPx(),
                    center = Offset(thumbX, thumbY),
                    style = Stroke(width = 2.dp.toPx())
                )
                drawCircle(
                    color = Color.Black,
                    radius = 9.dp.toPx(),
                    center = Offset(thumbX, thumbY),
                    style = Stroke(width = 1.dp.toPx())
                )
            }
        }

        val argb = android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value))
        val hexString = String.format("#%06X", 0xFFFFFF and argb)

        var hexInput by remember { mutableStateOf(hexString) }
        LaunchedEffect(hexString) {
            if (hexInput != hexString) {
                hexInput = hexString
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(activeColor)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            )

            Column {
                Text("Mã màu chọn", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(hexString, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
