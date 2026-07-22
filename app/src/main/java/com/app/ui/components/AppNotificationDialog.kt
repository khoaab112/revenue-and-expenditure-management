package com.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Reusable Dialog Button configuration
 */
data class DialogButtonConfig(
    val text: String,
    val action: () -> Unit = {},
    val containerColor: Color? = null,
    val contentColor: Color? = null
)

/**
 * General System Notification Dialog Component matching design requirements.
 * Cannot be closed by clicking outside or back button; user must actively click [Đóng] or X.
 */
@Composable
fun AppNotificationDialog(
    showDialog: Boolean,
    title: String = "Thông báo",
    content: String,
    confirmButton: DialogButtonConfig? = null,
    cancelButton: DialogButtonConfig? = null,
    onDismissRequest: () -> Unit = {},
    showCloseIcon: Boolean = true
) {
    if (!showDialog) return

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f)),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .clickable(enabled = false) {}, // Prevent click propagation
                shape = RoundedCornerShape(28.dp),
                color = Color.White,
                tonalElevation = 6.dp,
                shadowElevation = 12.dp
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp)
                ) {
                    // Header Section (Title + Close Icon)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = title,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF191C24)
                        )
                        if (showCloseIcon) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEFEFEF))
                                    .clickable { onDismissRequest() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Đóng",
                                    tint = Color(0xFF666666),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Content Body Text
                    Text(
                        text = content,
                        fontSize = 15.sp,
                        color = Color(0xFF4A4A4A),
                        lineHeight = 22.sp
                    )

                    // Button Action Section (Supports 0, 1, or 2 buttons)
                    if (confirmButton != null || cancelButton != null) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (cancelButton != null && confirmButton != null) {
                                OutlinedButton(
                                    onClick = cancelButton.action,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp),
                                    shape = RoundedCornerShape(25.dp),
                                    border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF191C24))
                                ) {
                                    Text(
                                        text = cancelButton.text,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                 Button(
                                    onClick = confirmButton.action,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp),
                                    shape = RoundedCornerShape(25.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = confirmButton.containerColor ?: Color(0xFF191C24),
                                        contentColor = confirmButton.contentColor ?: Color.White
                                    )
                                ) {
                                    Text(
                                        text = confirmButton.text,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            } else if (confirmButton != null) {
                                Button(
                                    onClick = confirmButton.action,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp),
                                    shape = RoundedCornerShape(25.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = confirmButton.containerColor ?: Color(0xFF191C24),
                                        contentColor = confirmButton.contentColor ?: Color.White
                                    )
                                ) {
                                    Text(
                                        text = confirmButton.text,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            } else if (cancelButton != null) {
                                OutlinedButton(
                                    onClick = cancelButton.action,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp),
                                    shape = RoundedCornerShape(25.dp),
                                    border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF191C24))
                                ) {
                                    Text(
                                        text = cancelButton.text,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold
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
