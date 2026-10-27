package com.example.xdownloader.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.example.xdownloader.data.MediaItem
import com.example.xdownloader.data.MediaType
import com.example.xdownloader.database.AppDatabase
import com.example.xdownloader.database.DownloadEntity
import com.example.xdownloader.repository.DownloadRepository
import com.example.xdownloader.ui.components.NativeEmptyState
import com.example.xdownloader.ui.components.PageHeading
import kotlinx.coroutines.launch

@Composable
fun DownloadsScreen() {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val repository = remember { DownloadRepository(context) }
    val scope = rememberCoroutineScope()
    val downloads by db.downloadDao().getAllDownloads().collectAsState(initial = emptyList())
    var filter by remember { mutableStateOf("全部") }
    val filtered = downloads.filter { download ->
        when (filter) {
            "進行中" -> download.status == "DOWNLOADING" || download.status == "PENDING"
            "已完成" -> download.status == "COMPLETED"
            else -> true
        }
    }
    val completed = downloads.count { it.status == "COMPLETED" }
    val active = downloads.count { it.status == "DOWNLOADING" || it.status == "PENDING" }

    Column(modifier = Modifier.fillMaxSize().padding(top = 20.dp)) {
        PageHeading(
            "下載中心",
            "完成 " + completed + " 筆 · 進行中 " + active + " 筆",
            Modifier.padding(horizontal = 20.dp)
        )
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("全部", "進行中", "已完成").forEach { item ->
                FilterChip(
                    selected = filter == item,
                    onClick = { filter = item },
                    label = { Text(item) }
                )
            }
        }
        if (filtered.isEmpty()) {
            Spacer(Modifier.weight(1f))
            NativeEmptyState(
                Icons.Default.Download,
                if (downloads.isEmpty()) "還沒有下載紀錄" else "這個分類沒有紀錄",
                if (downloads.isEmpty()) "在媒體卡片上點擊「下載」，任務會出現在這裡。" else "試著切換上方的分類。"
            )
            Spacer(Modifier.weight(1f))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 24.dp)
            ) {
                items(filtered, key = { it.url }) { dl ->
                    DownloadRow(
                        dl,
                        onCancel = { scope.launch { repository.cancelDownload(dl.url) } },
                        onRetry = {
                            scope.launch {
                                repository.enqueueDownload(
                                    MediaItem(
                                        id = dl.title.removePrefix("Download "),
                                        url = dl.url,
                                        thumbnailUrl = "",
                                        type = if (dl.type == "IMAGE") MediaType.IMAGE else MediaType.VIDEO
                                    ),
                                    forceRetry = true
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloadRow(dl: DownloadEntity, onCancel: () -> Unit, onRetry: () -> Unit) {
    val active = dl.status == "PENDING" || dl.status == "DOWNLOADING"
    val state = when (dl.status) {
        "COMPLETED" -> "已完成"
        "DOWNLOADING" -> "下載中"
        "PENDING" -> "等待中"
        "FAILED" -> "失敗"
        "CANCELLED" -> "已取消"
        else -> dl.status
    }
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(
                        if (dl.type == "IMAGE") Icons.Default.Image else Icons.Default.Movie,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(12.dp).size(24.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(dl.title, style = MaterialTheme.typography.titleSmall)
                    Text(if (dl.type == "IMAGE") "圖片" else "影片",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    state,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (dl.status == "FAILED") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }
            if (active) {
                LinearProgressIndicator(
                    progress = { dl.progress.coerceIn(0, 100) / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("完成 " + dl.progress.coerceIn(0, 100) + "%",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall)
            }
            if (active || dl.status == "FAILED" || dl.status == "CANCELLED") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = if (active) onCancel else onRetry) {
                        Text(if (active) "取消下載" else "重新下載")
                    }
                }
            } else if (dl.status == "COMPLETED") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("已儲存到手機媒體庫", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
