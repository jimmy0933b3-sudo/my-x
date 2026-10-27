package com.example.xdownloader.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.xdownloader.data.MediaItem
import com.example.xdownloader.data.MediaType
import com.example.xdownloader.database.AppDatabase
import com.example.xdownloader.database.FavoriteEntity
import com.example.xdownloader.ui.components.AuthorAvatar
import com.example.xdownloader.ui.components.MediaDownloadBottomSheet
import com.example.xdownloader.ui.components.MediaPreviewStrip
import com.example.xdownloader.ui.components.NativeEmptyState
import com.example.xdownloader.ui.components.PageHeading
import kotlinx.coroutines.launch

@Composable
fun FavoritesScreen() {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val favorites by db.favoriteDao().getAllFavorites().collectAsState(initial = emptyList())

    if (favorites.isEmpty()) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            PageHeading("我的收藏", "讓喜歡的圖片與影片有個家")
            Spacer(Modifier.weight(1f))
            NativeEmptyState(Icons.Default.Bookmark, "還沒有收藏", "點擊動態卡片的愛心，即可收藏喜愛的貼文。")
            Spacer(Modifier.weight(1f))
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 20.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    PageHeading("我的收藏", "隨時回來欣賞與下載", Modifier.weight(1f))
                    AssistChip(onClick = {}, label = { Text(favorites.size.toString() + " 篇") })
                }
            }
            items(favorites, key = { it.tweetId }) { FavoriteCard(it) }
        }
    }
}

@Composable
fun FavoriteCard(fav: FavoriteEntity) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val scope = rememberCoroutineScope()
    var showViewer by remember { mutableStateOf(false) }
    var showSheet by remember { mutableStateOf(false) }

    val urls = remember(fav.mediaUrls) { fav.mediaUrls.split(",").filter { it.isNotBlank() } }
    val types = remember(fav.mediaTypes) { fav.mediaTypes.split(",") }
    val thumbs = remember(fav.mediaThumbnails) { fav.mediaThumbnails.split(",") }
    val media = remember(fav.tweetId, urls, types, thumbs) {
        urls.mapIndexed { i, url ->
            MediaItem(
                id = fav.tweetId + "_" + i,
                url = url,
                thumbnailUrl = thumbs.getOrNull(i).orEmpty(),
                type = if (types.getOrNull(i) == "VIDEO") MediaType.VIDEO else MediaType.IMAGE
            )
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AuthorAvatar(fav.authorAvatar, fav.authorName)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(fav.authorName, style = MaterialTheme.typography.titleMedium)
                    Text("@" + fav.authorScreenName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { scope.launch { db.favoriteDao().deleteFavorite(fav) } }) {
                    Icon(Icons.Default.Close, contentDescription = "移除收藏")
                }
            }
            if (fav.text.isNotBlank()) Text(fav.text, style = MaterialTheme.typography.bodyMedium)
            if (fav.deleted) {
                Text("貼文可能已刪除或無法存取", color = MaterialTheme.colorScheme.error)
            }
            MediaPreviewStrip(items = media, onClick = { showViewer = true })
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(media.size.toString() + " 個媒體",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f))
                FilledTonalButton(onClick = { showSheet = true }, enabled = media.isNotEmpty()) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("下載")
                }
            }
        }
    }

    if (showSheet && media.isNotEmpty()) {
        MediaDownloadBottomSheet(media, onDismiss = { showSheet = false })
    }
    if (showViewer && media.isNotEmpty()) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showViewer = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(Modifier.fillMaxSize()) {
                com.example.xdownloader.ui.components.MediaViewer(media)
                IconButton(
                    onClick = { showViewer = false },
                    modifier = Modifier.align(Alignment.TopStart).padding(16.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "關閉", tint = androidx.compose.ui.graphics.Color.White)
                }
            }
        }
    }
}
