package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.Event
import com.example.ui.FinanceViewModel
import com.example.ui.FormatHelper
import java.util.*

import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke

import androidx.compose.foundation.border
import androidx.compose.ui.graphics.drawscope.clipRect

@Composable
fun StripedProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    stripeColor: Color = Color.White.copy(alpha = 0.3f)
) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val progressWidth = width * progress.coerceIn(0f, 1f)

        // Draw track
        drawRoundRect(
            color = trackColor,
            size = size,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(height / 2, height / 2)
        )

        // Draw progress
        if (progress > 0f) {
            drawRoundRect(
                color = color,
                size = size.copy(width = progressWidth),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(height / 2, height / 2)
            )

            // Draw stripes inside progress
            clipRect(right = progressWidth) {
                val stripeWidth = 10.dp.toPx()
                val gap = 10.dp.toPx()
                val numStripes = (progressWidth / (stripeWidth + gap)).toInt() + 2

                for (i in -1..numStripes) {
                    val startX = i * (stripeWidth + gap)
                    drawLine(
                        color = stripeColor,
                        start = Offset(startX, height),
                        end = Offset(startX + stripeWidth, 0f),
                        strokeWidth = stripeWidth,
                        cap = StrokeCap.Square
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventManagementScreen(
    viewModel: FinanceViewModel,
    onBack: () -> Unit
) {
    val events by viewModel.allEvents.collectAsState()
    val transactions by viewModel.dailyTransactions.collectAsState()

    var showAddEventDialog by remember { mutableStateOf(false) }
    var eventToEdit by remember { mutableStateOf<Event?>(null) }
    var eventToDelete by remember { mutableStateOf<Event?>(null) }
    var eventToView by remember { mutableStateOf<Event?>(null) }

    Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text("Quản lý sự kiện", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddEventDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Event")
                    }
                },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (events.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Chưa có sự kiện nào.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(events) { event ->
                            val eventTransactions = transactions.filter { it.eventId == event.id }
                            val totalSpent = eventTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
                            
                            val now = System.currentTimeMillis()
                            val isUpcoming = now < event.startDate
                            val isEnded = event.endDate != null && now > (event.endDate + 86400000L - 1)
                            val status = when {
                                isEnded -> "Đã kết thúc"
                                isUpcoming -> "Sắp diễn ra"
                                else -> "Đang diễn ra"
                            }
                            val statusColor = when {
                                isEnded -> Color(0xFF9E9E9E)
                                isUpcoming -> Color(0xFFFF9800)
                                else -> Color(0xFF4CAF50)
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = event.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 18.sp,
                                                color = try { Color(android.graphics.Color.parseColor(event.colorHex)) } catch (e: Exception) { MaterialTheme.colorScheme.onSurface }
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(50))
                                                    .background(statusColor.copy(alpha = 0.1f))
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = status,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = statusColor
                                                )
                                            }
                                        }
                                        Row {
                                            IconButton(onClick = { eventToView = event }, modifier = Modifier.size(36.dp)) {
                                                Icon(Icons.Default.Info, contentDescription = "Chi tiết", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                                            }
                                            IconButton(onClick = { eventToEdit = event }, modifier = Modifier.size(36.dp)) {
                                                Icon(Icons.Default.Edit, contentDescription = "Sửa", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                            }
                                            IconButton(onClick = { eventToDelete = event }, modifier = Modifier.size(36.dp)) {
                                                Icon(Icons.Default.Delete, contentDescription = "Xóa", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                            }
                                        }
                                    }

                                    if (event.description.isNotBlank()) {
                                        Text(
                                            text = event.description,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    val startStr = FormatHelper.formatDate(event.startDate)
                                    val endStr = event.endDate?.let { FormatHelper.formatDate(it) } ?: "Không giới hạn"
                                    Text(
                                        text = "$startStr - $endStr",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.outline
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Progress Bar
                                    val limit = event.limitAmount ?: 0.0
                                    val progress = if (limit > 0) (totalSpent / limit).toFloat() else if (isEnded) 1f else 1f
                                    val safeProgress = progress.coerceIn(0f, 1f)
                                    
                                    val progressColor = when {
                                        isEnded -> MaterialTheme.colorScheme.error
                                        limit > 0 && progress >= 0.9f -> MaterialTheme.colorScheme.error
                                        else -> try { Color(android.graphics.Color.parseColor(event.colorHex)) } catch (e: Exception) { Color(0xFF4CAF50) }
                                    }

                                    StripedProgressIndicator(
                                        progress = safeProgress,
                                        modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)),
                                        color = progressColor,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )

                                    if (limit > 0) {
                                        Text(
                                            text = "${FormatHelper.formatVND(totalSpent)} / ${FormatHelper.formatVND(limit)}",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(top = 4.dp).align(Alignment.End)
                                        )
                                    } else {
                                        Text(
                                            text = "Đã chi: ${FormatHelper.formatVND(totalSpent)}",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(top = 4.dp).align(Alignment.End)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

    if (eventToView != null) {
        val event = eventToView!!
        val eventTransactions = transactions.filter { it.eventId == event.id }
        val limit = event.limitAmount ?: 0.0
        val spent = eventTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        
        AlertDialog(
            onDismissRequest = { eventToView = null },
            title = { Text(text = event.name, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (event.description.isNotBlank()) {
                        Text(event.description, fontSize = 14.sp)
                    }
                    val startStr = FormatHelper.formatDate(event.startDate)
                    val endStr = event.endDate?.let { FormatHelper.formatDate(it) } ?: "Không giới hạn"
                    Text("Thời gian: $startStr - $endStr", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)

                    if (limit > 0) {
                        Text("Tiến độ chi tiêu:", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                        val progress = (spent / limit).toFloat().coerceIn(0f, 1f)
                        StripedProgressIndicator(
                            progress = progress,
                            modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                            color = if (progress >= 0.9f) MaterialTheme.colorScheme.error else try { androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(event.colorHex)) } catch (e: Exception) { MaterialTheme.colorScheme.primary },
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Text(
                            text = "${FormatHelper.formatVND(spent)} / ${FormatHelper.formatVND(limit)}",
                            fontSize = 12.sp,
                            modifier = Modifier.align(Alignment.End)
                        )
                    } else {
                        Text("Đã chi: ${FormatHelper.formatVND(spent)}", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                    }

                    if (eventTransactions.isNotEmpty()) {
                        Text("Lịch sử giao dịch liên quan:", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp))
                        androidx.compose.foundation.lazy.LazyColumn(
                            modifier = Modifier.heightIn(max = 200.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(eventTransactions.sortedByDescending { it.timestamp }) { tx ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(tx.categoryName, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                        Text(FormatHelper.formatDate(tx.timestamp), fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                    }
                                    Text(
                                        text = "${if (tx.type == "EXPENSE") "-" else "+"}${FormatHelper.formatVND(tx.amount)}",
                                        color = if (tx.type == "EXPENSE") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                                androidx.compose.material3.HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            }
                        }
                    } else {
                        Text("Chưa có giao dịch.", fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, fontSize = 13.sp, color = MaterialTheme.colorScheme.outline)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { eventToView = null }) {
                    Text("Đóng")
                }
            }
        )
    }

    if (showAddEventDialog || eventToEdit != null) {
        val editingEvent = eventToEdit
        var name by remember { mutableStateOf(editingEvent?.name ?: "") }
        var description by remember { mutableStateOf(editingEvent?.description ?: "") }
        var startDate by remember { mutableStateOf(editingEvent?.startDate ?: System.currentTimeMillis()) }
        var endDate by remember { mutableStateOf(editingEvent?.endDate) }
        var limitAmountStr by remember { mutableStateOf(editingEvent?.limitAmount?.let { String.format(java.util.Locale.US, "%,d", it.toLong()).replace(',', '.') } ?: "") }
        var selectedColor by remember { mutableStateOf(editingEvent?.colorHex ?: "#FF9800") }

        val colors = listOf(
            "#F44336", "#E91E63", "#9C27B0", "#673AB7", "#3F51B5",
            "#2196F3", "#03A9F4", "#00BCD4", "#009688", "#4CAF50",
            "#8BC34A", "#CDDC39", "#FFEB3B", "#FFC107", "#FF9800",
            "#FF5722", "#795548", "#9E9E9E", "#607D8B"
        )

        var showStartDatePicker by remember { mutableStateOf(false) }
        var showEndDatePicker by remember { mutableStateOf(false) }

        val startDateState = rememberDatePickerState(initialSelectedDateMillis = startDate)
        val endDateState = rememberDatePickerState(initialSelectedDateMillis = endDate ?: System.currentTimeMillis())

        if (showStartDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showStartDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        startDateState.selectedDateMillis?.let { startDate = it }
                        showStartDatePicker = false
                    }) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { showStartDatePicker = false }) { Text("Hủy") }
                }
            ) {
                DatePicker(state = startDateState)
            }
        }

        if (showEndDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showEndDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        endDateState.selectedDateMillis?.let { endDate = it }
                        showEndDatePicker = false
                    }) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { showEndDatePicker = false }) { Text("Hủy") }
                }
            ) {
                DatePicker(state = endDateState)
            }
        }

        AlertDialog(
            onDismissRequest = { 
                showAddEventDialog = false
                eventToEdit = null 
            },
            title = { Text(if (editingEvent != null) "Sửa sự kiện" else "Thêm sự kiện mới", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Tên sự kiện (*)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Mô tả") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = limitAmountStr,
                        onValueChange = { str ->
                            val raw = str.filter { c -> c.isDigit() }
                            if (raw.isNotEmpty()) {
                                try {
                                    val formatted = String.format(java.util.Locale.US, "%,d", raw.toLong()).replace(',', '.')
                                    limitAmountStr = formatted
                                } catch (e: Exception) {
                                    // ignore overflow
                                }
                            } else {
                                limitAmountStr = ""
                            }
                        },
                        label = { Text("Hạn mức chi tiêu") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Text("Màu sắc", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(colors) { hex ->
                            val colorValue = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { Color.Black }
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(colorValue)
                                    .clickable { selectedColor = hex }
                                    .border(
                                        2.dp,
                                        if (selectedColor == hex) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        androidx.compose.foundation.shape.CircleShape
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Bắt đầu: ${FormatHelper.formatDate(startDate)}", fontSize = 14.sp)
                        TextButton(onClick = { showStartDatePicker = true }) {
                            Text("Chọn ngày")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (endDate != null) "Kết thúc: ${FormatHelper.formatDate(endDate!!)}" else "Kết thúc: Không", fontSize = 14.sp)
                        Row {
                            if (endDate != null) {
                                IconButton(onClick = { endDate = null }) {
                                    Icon(Icons.Default.Clear, "Clear")
                                }
                            }
                            TextButton(onClick = { showEndDatePicker = true }) {
                                Text("Chọn")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isBlank()) {
                            viewModel.showWarningNotification("Vui lòng nhập tên sự kiện")
                            return@Button
                        }
                        // Validate dates
                        if (endDate != null && endDate!! < startDate) {
                            viewModel.showWarningNotification("Ngày kết thúc phải sau ngày bắt đầu")
                            return@Button
                        }
                        val limit = limitAmountStr.replace(".", "").toDoubleOrNull()
                        
                        if (editingEvent != null) {
                            viewModel.updateEvent(editingEvent.copy(
                                name = name,
                                description = description,
                                startDate = startDate,
                                endDate = endDate,
                                limitAmount = limit,
                                colorHex = selectedColor
                            ))
                            viewModel.showSuccessNotification("Cập nhật thành công")
                        } else {
                            viewModel.addEvent(
                                name = name,
                                description = description,
                                startDate = startDate,
                                endDate = endDate,
                                limitAmount = limit,
                                colorHex = selectedColor
                            )
                            viewModel.showSuccessNotification("Thêm thành công")
                        }
                        showAddEventDialog = false
                        eventToEdit = null
                    }
                ) {
                    Text("Lưu")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showAddEventDialog = false 
                    eventToEdit = null
                }) {
                    Text("Hủy")
                }
            }
        )
    }

    if (eventToDelete != null) {
        val event = eventToDelete!!
        AlertDialog(
            onDismissRequest = { eventToDelete = null },
            title = { Text("Xóa sự kiện?", fontWeight = FontWeight.Bold) },
            text = { Text("Bạn có chắc chắn muốn xóa sự kiện '${event.name}'? Các giao dịch sẽ KHÔNG bị xóa, nhưng sẽ không còn gắn với sự kiện này nữa.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteEvent(event)
                        viewModel.showSuccessNotification("Đã xóa sự kiện")
                        eventToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Xóa", color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(onClick = { eventToDelete = null }) {
                    Text("Thoát", color = MaterialTheme.colorScheme.onSurface)
                }
            }
        )
    }
}
