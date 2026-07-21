package com.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.ui.FinanceViewModel

@Composable
fun OnboardingScreen(
    viewModel: FinanceViewModel,
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val syncStatus by viewModel.syncStatus.collectAsState()
    
    // We only want to trigger onComplete once after a successful sync
    var isWaitingForSync by remember { mutableStateOf(false) }

    LaunchedEffect(syncStatus) {
        if (isWaitingForSync) {
            if (syncStatus == "SUCCESS") {
                viewModel.showSuccessNotification("Quá trình đồng bộ đã hoàn tất")
                isWaitingForSync = false
                onComplete()
            } else if (syncStatus == "ERROR") {
                isWaitingForSync = false
            }
        }
    }
    
    val signInLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            try {
                val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(result.data)
                task.getResult(com.google.android.gms.common.api.ApiException::class.java)
                
                isWaitingForSync = true
                viewModel.restoreFromDrive(context)
            } catch (e: Exception) {
                viewModel.showWarningNotification("Lỗi đăng nhập Google: ${e.message}")
            }
        } else {
            viewModel.showWarningNotification("Đăng nhập bị hủy.")
        }
    }

    val backupFilePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let {
            isWaitingForSync = true
            viewModel.importLocalBackup(context, it)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF0F4FA) // Fresh, modern light blue/grey background
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            // Main card containing the content
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 64.dp), // Extend vertically almost to screen edges
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.6f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Chào Mừng",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Người dùng mới button
                    OutlinedButton(
                        onClick = onComplete,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, Color.Black),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White, contentColor = Color.Black)
                    ) {
                        Text("Người dùng mới", fontSize = 16.sp, fontWeight = FontWeight.Normal)
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        // Backup card container
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp), // Push down to let text overlap the border
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color.LightGray)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 26.dp, bottom = 16.dp, start = 16.dp, end = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                            // Google Login Button
                            OutlinedButton(
                                onClick = {
                                    val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN)
                                        .requestEmail()
                                        .requestScopes(com.google.android.gms.common.api.Scope("https://www.googleapis.com/auth/drive.file"))
                                        .build()
                                    val client = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(context, gso)
                                    signInLauncher.launch(client.signInIntent)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, Color.LightGray),
                                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = com.app.R.drawable.ic_google),
                                        contentDescription = "Google Logo",
                                        modifier = Modifier.size(20.dp),
                                        tint = Color.Unspecified
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("Đăng nhập bằng Google", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color.Black)
                                }
                            }

                            // JSON Backup Button
                            Button(
                                onClick = {
                                    backupFilePickerLauncher.launch("application/json")
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE2E6FF), contentColor = Color(0xFF19224C))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(imageVector = Icons.Default.UploadFile, contentDescription = null, tint = Color(0xFF7084FF))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("Tải lên tệp backup (JSON)", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }

                    // Overlapping text
                    Text(
                        text = "Bạn đã có data từ trước",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Gray,
                        modifier = Modifier
                            .background(Color.White)
                            .padding(horizontal = 8.dp)
                    )
                }
            }
        }

            // Fullscreen Loading Overlay
            if (syncStatus == "SYNCING" && isWaitingForSync) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .pointerInput(Unit) {}, // Consume clicks
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = Color(0xFF4285F4))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Đang đồng bộ dữ liệu...",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.Black
                            )
                        }
                    }
                }
            }
        }
    }
}
