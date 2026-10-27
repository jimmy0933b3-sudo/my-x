package com.example.xdownloader.ui.screens.login

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.xdownloader.api.ApiResult
import com.example.xdownloader.api.AuthManager
import com.example.xdownloader.api.XApiClient
import kotlinx.coroutines.launch

/**
 * Fully native fallback for the existing cookie-based X API integration.
 * Browser cookies are intentionally never read from an external browser.
 * A real one-tap X login requires OAuth 2.0 + PKCE and migrating XApiClient
 * to the official API endpoints; a Chrome Custom Tab alone cannot provide ct0.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavController) {
    val context = LocalContext.current
    val authManager = remember { AuthManager.getInstance(context) }
    val apiClient = remember { XApiClient(context) }
    val scope = rememberCoroutineScope()

    var authToken by remember { mutableStateOf("") }
    var ct0 by remember { mutableStateOf("") }
    var showSecrets by remember { mutableStateOf(false) }
    var isVerifying by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("連接 X 帳號") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Column(modifier = Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("保留你的下載工作區", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "新版已移除內嵌 WebView。舊版的登入憑證若仍有效，無須重新登入。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Text("使用既有 X 工作階段", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "這個相容模式需要 auth_token 與 ct0，並不是 X 的帳號密碼。請勿將憑證提供給陌生人，也不要貼到公開對話。",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
            OutlinedTextField(
                value = authToken,
                onValueChange = { authToken = it.trim(); errorMessage = null },
                label = { Text("auth_token") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (showSecrets) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )
            OutlinedTextField(
                value = ct0,
                onValueChange = { ct0 = it.trim(); errorMessage = null },
                label = { Text("ct0") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (showSecrets) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )
            TextButton(onClick = { showSecrets = !showSecrets }) {
                Icon(
                    if (showSecrets) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = null
                )
                Spacer(Modifier.width(8.dp))
                Text(if (showSecrets) "隱藏憑證" else "顯示憑證")
            }

            if (errorMessage != null) {
                Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.medium) {
                    Text(
                        text = requireNotNull(errorMessage),
                        modifier = Modifier.padding(14.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            Button(
                onClick = {
                    if (isVerifying) return@Button
                    isVerifying = true
                    errorMessage = null
                    scope.launch {
                        try {
                            when (val result = apiClient.verifyCredentials(authToken, ct0)) {
                                is ApiResult.Success -> {
                                    authManager.saveCredentials(authToken, ct0)
                                    Toast.makeText(context, "連接成功", Toast.LENGTH_SHORT).show()
                                    navController.navigate("home") {
                                        popUpTo("home") { inclusive = true }
                                        launchSingleTop = true
                                    }
                                }
                                is ApiResult.Unauthorized -> errorMessage = result.message
                                is ApiResult.RateLimited -> errorMessage = result.message
                                is ApiResult.NetworkError -> errorMessage = result.message
                                is ApiResult.ParsingError -> errorMessage = result.message
                            }
                        } catch (e: Exception) {
                            errorMessage = e.message ?: "連接失敗，請稍後再試"
                        } finally {
                            isVerifying = false
                        }
                    }
                },
                enabled = authToken.isNotBlank() && ct0.isNotBlank() && !isVerifying,
                modifier = Modifier.fillMaxWidth().height(54.dp)
            ) {
                if (isVerifying) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(Modifier.width(10.dp))
                    Text("安全驗證並連接")
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            OutlinedButton(
                modifier = Modifier.fillMaxWidth().height(52.dp),
                onClick = {
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://x.com/i/flow/login")))
                    } catch (e: Exception) {
                        Toast.makeText(context, "找不到可開啟網頁的瀏覽器", Toast.LENGTH_SHORT).show()
                    }
                }
            ) {
                Icon(Icons.Default.Launch, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("在外部瀏覽器開啟 X")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                Text(
                    "外部瀏覽器登入不會自動取得 Cookie。未來若接入 X 官方 OAuth 2.0 與付費 API，才能實現免輸入憑證的一鍵授權。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}
