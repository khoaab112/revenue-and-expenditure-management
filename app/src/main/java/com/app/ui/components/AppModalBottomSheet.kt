package com.app.ui.components

import com.app.data.*
import com.app.ui.*

import com.app.ui.viewmodels.*

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Standardized System ModalBottomSheet component.
 * Divided into 3 strict sections:
 * 1. HEADER: Standard DragHandle, Title (20.sp, FontWeight.Black), optional extra action buttons (e.g. Info icon), mandatory Close button (X), and HorizontalDivider line.
 * 2. CONTENT: Flexible body area, vertically scrollable by default or customized per screen needs.
 * 3. FOOTER: Flexible bottom area (Group action buttons, empty, or description text).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppModalBottomSheet(
    onDismissRequest: () -> Unit,
    title: String,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    headerExtraActions: @Composable (RowScope.() -> Unit)? = null,
    footer: @Composable (ColumnScope.() -> Unit)? = null,
    isScrollableContent: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .width(38.dp)
                        .height(4.5.dp),
                    shape = RoundedCornerShape(2.5.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                ) {}
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            // 1. HEADER SECTION
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (headerExtraActions != null) {
                        headerExtraActions()
                    }
                    IconButton(onClick = onDismissRequest) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Đóng",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 20.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )

            // 2. CONTENT SECTION
            val bodyModifier = if (isScrollableContent) {
                Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(contentPadding)
            } else {
                Modifier
                    .fillMaxWidth()
                    .padding(contentPadding)
            }

            Column(
                modifier = bodyModifier,
                content = content
            )

            // 3. FOOTER SECTION
            if (footer != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    content = footer
                )
            }
        }
    }
}
