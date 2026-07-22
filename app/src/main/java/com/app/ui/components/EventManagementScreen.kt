package com.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.app.data.Event
import com.app.ui.FinanceViewModel
import com.app.ui.FormatHelper
import java.util.*

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

data class EventStatusStyle(
    val text: String,
    val dotColor: Color,
    val backgroundColor: Color,
    val textColor: Color
)

fun getEventStatusStyle(event: Event, totalSpent: Double, now: Long = System.currentTimeMillis()): EventStatusStyle {
    val isUpcoming = now < event.startDate
    val isEnded = event.endDate != null && now > (event.endDate + 86400000L - 1)
    val limit = event.limitAmount ?: 0.0
    val isEndingSoon = !isEnded && !isUpcoming && (
        (event.endDate != null && (event.endDate - now) <= 3 * 86400000L) ||
        (limit > 0 && (totalSpent / limit) >= 0.8)
    )

    return when {
        isEnded -> EventStatusStyle(
            text = "Đã qua",
            dotColor = Color.White,
            backgroundColor = Color(0xFF757575),
            textColor = Color.White
        )
        isUpcoming -> EventStatusStyle(
            text = "Chưa diễn ra",
            dotColor = Color.White,
            backgroundColor = Color(0xFFE0E0E0),
            textColor = Color(0xFF616161)
        )
        isEndingSoon -> EventStatusStyle(
            text = "Sắp kết thúc",
            dotColor = Color(0xFFFF9800),
            backgroundColor = Color(0xFFFFF3E0),
            textColor = Color(0xFFE65100)
        )
        else -> EventStatusStyle(
            text = "Đang diễn ra",
            dotColor = Color(0xFF4CAF50),
            backgroundColor = Color(0xFFE8F5E9),
            textColor = Color(0xFF2E7D32)
        )
    }
}

@Composable
fun EventStatusChip(statusStyle: EventStatusStyle) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(statusStyle.backgroundColor)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(statusStyle.dotColor)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = statusStyle.text,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = statusStyle.textColor
        )
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
    var showBottomSheetEvent by remember { mutableStateOf<Event?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddEventDialog = true },
                containerColor = Color(0xFF00E676),
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Thêm sự kiện", tint = Color.White)
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (events.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Chưa có sự kiện nào.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(events) { event ->
                        val eventTransactions = transactions.filter { it.eventId == event.id }
                        val totalSpent = eventTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
                        val statusStyle = getEventStatusStyle(event, totalSpent)
                        val limit = event.limitAmount ?: 0.0
                        val eventColor = try { Color(android.graphics.Color.parseColor(event.colorHex)) } catch (e: Exception) { Color(0xFFFF9800) }
                        val now = System.currentTimeMillis()
                        val isPast = statusStyle.text == "Đã qua" || (event.endDate != null && now > event.endDate)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(if (isPast) Modifier.alpha(0.55f) else Modifier),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                                // Top Row: Status chip + 3 dots menu
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    EventStatusChip(statusStyle = statusStyle)
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .clickable { showBottomSheetEvent = event },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = "Menu",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Event Name
                                Text(
                                    text = event.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 19.sp,
                                    color = eventColor
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                // Group 1: Group thời gian + Line (Màu đồng nhất với bên ngoài, không dùng box màu xám)
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val startStr = FormatHelper.formatDate(event.startDate)
                                        val endStr = event.endDate?.let { FormatHelper.formatDate(it) } ?: "Vô thời hạn"
                                        Text(
                                            text = "$startStr - $endStr",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        // Pill hiển thị thời gian (Dùng eventColor)
                                        val remainingText = if (event.endDate == null) {
                                            "Vô thời hạn"
                                        } else if (now < event.startDate) {
                                            val diffMillis = event.endDate - now
                                            val diffDays = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diffMillis)
                                            if (diffDays > 0) "Còn $diffDays ngày" else "Chưa diễn ra"
                                        } else {
                                            val diffMillis = event.endDate - now
                                            val diffDays = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diffMillis)
                                            if (diffMillis > 0 && diffDays == 0L) {
                                                "Còn 1 ngày"
                                            } else if (diffDays > 0) {
                                                "Còn $diffDays ngày"
                                            } else if (diffDays == 0L) {
                                                "Hôm nay hết hạn"
                                            } else {
                                                "Đã hết hạn"
                                            }
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(20.dp),
                                            color = eventColor.copy(alpha = 0.12f),
                                            contentColor = eventColor
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DateRange,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(13.dp),
                                                    tint = eventColor
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = remainingText,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = eventColor
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Thanh line ngang: Thời gian hết hạn (Nếu chưa diễn ra -> 0f, vô thời hạn -> 0.4f, đã qua -> 1f)
                                    val timeProgress = if (event.endDate == null) {
                                        0.4f
                                    } else if (now < event.startDate) {
                                        0f
                                    } else if (now >= event.endDate) {
                                        1f
                                    } else {
                                        val totalDuration = (event.endDate - event.startDate).toFloat()
                                        val elapsed = (now - event.startDate).toFloat()
                                        if (totalDuration > 0) (elapsed / totalDuration).coerceIn(0f, 1f) else 0f
                                    }

                                    StripedProgressIndicator(
                                        progress = timeProgress,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        color = eventColor,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Group 2: Group Đã chi, Hạn mức, % Vòng tròn (Cân bằng tỉ lệ 50/50, chữ in đậm, màu theo eventColor)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Đã chi",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = FormatHelper.formatVND(totalSpent),
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = eventColor
                                                )
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .width(1.dp)
                                                    .height(24.dp)
                                                    .background(MaterialTheme.colorScheme.outlineVariant)
                                            )

                                            Spacer(modifier = Modifier.width(12.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Hạn mức",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = if (limit > 0) FormatHelper.formatVND(limit) else "Không có",
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        val percent = if (limit > 0) ((totalSpent / limit) * 100).toInt().coerceIn(0, 100) else 0
                                        val safeProgress = if (limit > 0) (totalSpent / limit).toFloat().coerceIn(0f, 1f) else 0f

                                        Box(
                                            modifier = Modifier.size(44.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(
                                                progress = { safeProgress },
                                                modifier = Modifier.fillMaxSize(),
                                                color = eventColor,
                                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                                strokeWidth = 4.dp
                                            )
                                            Text(
                                                text = "$percent%",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = eventColor
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet for 3-dots menu action
    if (showBottomSheetEvent != null) {
        val event = showBottomSheetEvent!!
        val sheetTitleColor = try { Color(android.graphics.Color.parseColor(event.colorHex)) } catch (e: Exception) { Color(0xFFFF9800) }

        ModalBottomSheet(
            onDismissRequest = { showBottomSheetEvent = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = event.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = sheetTitleColor
                    )
                    IconButton(onClick = { showBottomSheetEvent = null }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Spacer(modifier = Modifier.height(8.dp))

                // Xem chi tiết
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val target = event
                            showBottomSheetEvent = null
                            eventToView = target
                        }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Chi tiết",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Chi tiết sự kiện",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Sửa sự kiện
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val target = event
                            showBottomSheetEvent = null
                            eventToEdit = target
                        }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Sửa",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Sửa sự kiện",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Xóa sự kiện
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val target = event
                            showBottomSheetEvent = null
                            eventToDelete = target
                        }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Xóa",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Xóa sự kiện",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.error
                    )
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
                            color = if (progress >= 0.9f) MaterialTheme.colorScheme.error else try { Color(android.graphics.Color.parseColor(event.colorHex)) } catch (e: Exception) { MaterialTheme.colorScheme.primary },
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
                        LazyColumn(
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
                                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
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
            "#F44336", "#E91E63", "#9C27B0", "#673AB7", "#3F51B5", "#2196F3", "#03A9F4",
            "#00BCD4", "#009688", "#4CAF50", "#8BC34A", "#CDDC39", "#FFEB3B", "#FFC107",
            "#FF9800", "#FF5722", "#795548", "#9E9E9E", "#607D8B", "#3949AB", "#D81B60"
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

        Dialog(
            onDismissRequest = {
                showAddEventDialog = false
                eventToEdit = null
            }
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = if (editingEvent != null) "Sửa sự kiện" else "Thêm sự kiện mới",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Tên sự kiện (*)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Mô tả") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
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
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Text("Màu sắc", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    // Color selection in Grid Format (7 equal-weight columns per row for 100% uniform spacing)
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        colors.chunked(7).forEach { rowColors ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                for (i in 0 until 7) {
                                    Box(
                                        modifier = Modifier.weight(1f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (i < rowColors.size) {
                                            val hex = rowColors[i]
                                            val colorValue = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { Color.Black }
                                            Box(
                                                modifier = Modifier
                                                    .size(30.dp)
                                                    .clip(CircleShape)
                                                    .background(colorValue)
                                                    .clickable { selectedColor = hex }
                                                    .border(
                                                        2.dp,
                                                        if (selectedColor == hex) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                                        CircleShape
                                                    )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Row Bắt đầu
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showStartDatePicker = true },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = null,
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Bắt đầu", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Text(
                            text = FormatHelper.formatDate(startDate),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Row Kết thúc
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showEndDatePicker = true },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = null,
                                    tint = Color(0xFFF44336),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Kết thúc", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (endDate != null) {
                                Text(
                                    text = FormatHelper.formatDate(endDate!!),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                IconButton(
                                    onClick = { endDate = null },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        tint = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else {
                                Text(
                                    text = "Chọn",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF1E88E5)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Action buttons: Hủy & Lưu thay đổi
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                showAddEventDialog = false
                                eventToEdit = null
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Text("Hủy", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        Button(
                            onClick = {
                                if (name.isBlank()) {
                                    viewModel.showWarningNotification("Vui lòng nhập tên sự kiện")
                                    return@Button
                                }
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
                            },
                            modifier = Modifier
                                .weight(1.5f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF6C5CE7),
                                contentColor = Color.White
                            )
                        ) {
                            Text("Lưu thay đổi", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }
        }
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
