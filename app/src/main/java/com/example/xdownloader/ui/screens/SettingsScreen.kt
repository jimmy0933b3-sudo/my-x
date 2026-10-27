package com.example.xdownloader.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.xdownloader.api.ApiResult
import com.example.xdownloader.api.AuthManager
import com.example.xdownloader.api.XApiClient
import com.example.xdownloader.ui.components.PageHeading
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val auth = remember { AuthManager.getInstance(context) }
    val api = remember { XApiClient(context) }
    val scope = rememberCoroutineScope()
    val configured by auth.authState.collectAsState()
    var verifying by remember { mutableStateOf(false) }
    var resultText by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        PageHeading("設定", "帳號、連接狀態與隱私")
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = MaterialTheme.shapes.large
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = if (configured) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(
                            if (configured) Icons.Default.CheckCircle else Icons.Default.Person,
                            contentDescription = null,
                            tint = if (configured) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(13.dp).size(26.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(if (configured) "已連接 X 工作階段" else "尚未連接 X",
                            style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (configured) "憑證儲存在此裝置的加密儲存空間"
                            else "使用原生介面連接既有工作階段",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                if (resultText != null) {
                    Text(requireNotNull(resultText),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall)
                }
                if (configured) {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !verifying,
                        onClick = {
                            scope.launch {
                                verifying = true
                                try {
                                    resultText = when (val result = api.verifyCredentials()) {
                                        is ApiResult.Success -> "驗證成功：@" + result.data
                                        is ApiResult.Unauthorized -> result.message
                                        is ApiResult.RateLimited -> result.message
                                        is ApiResult.NetworkError -> result.message
                                        is ApiResult.ParsingError -> result.message
                                    }
                                } catch (e: Exception) {
                                    resultText = e.message ?: "驗證失敗"
                                } finally { verifying = false }
                            }
                        }
                    ) {
                        if (verifying) CircularProgressIndicator(Modifier.size(18.dp))
                        else Text("重新驗證連接")
                    }
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { navController.navigate("login") }
                    ) {
                        Text("切換工作階段")
                    }
                    TextButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            auth.clearCredentials()
                            resultText = null
                            Toast.makeText(context, "已清除本機登入憑證", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("登出並清除本機憑證")
                    }
                } else {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { navController.navigate("login") }
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("連接 X 帳號")
                    }
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = MaterialTheme.shapes.large
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("關於新版", style = MaterialTheme.typography.titleLarge)
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    Text(
                        "所有主畫面、媒體預覽、搜尋、收藏與下載清單皆使用 Android 原生元件。登入頁也已移除 WebView。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                HorizontalDivider()
                Text(
                    "目前仍以既有 X 工作階段憑證存取媒體；官方 OAuth 登入需申請 X Developer Client ID，並同步遷移 API。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
