package com.example.xdownloader.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import com.example.xdownloader.database.FavoriteEntity
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.nestedscroll.nestedScroll

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.xdownloader.api.TweetParser
import com.example.xdownloader.api.XApiClient
import com.example.xdownloader.api.ApiResult
import com.example.xdownloader.database.AppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.example.xdownloader.ui.components.MediaDownloadBottomSheet
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Image

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onOpenSettings: () -> Unit = {}) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val apiClient = remember { XApiClient(context) }

    val viewModel: com.example.xdownloader.ui.viewmodel.HomeScreenViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return com.example.xdownloader.ui.viewmodel.HomeScreenViewModel(db, apiClient) as T
            }
        }
    )

    val tweets by viewModel.tweets.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val paginationError by viewModel.paginationError.collectAsState()

    val pullRefreshState = rememberPullToRefreshState()
    val listState = rememberLazyListState()

    val shouldLoadMore = remember {
        derivedStateOf {
            val totalItems = listState.layoutInfo.totalItemsCount
            val lastVisibleItemIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisibleItemIndex >= totalItems - 3
        }
    }

    val authManager = remember { com.example.xdownloader.api.AuthManager.getInstance(context) }
    val isConfigured by authManager.authState.collectAsState()

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value && !isLoading && !isLoadingMore && paginationError == null) {
            viewModel.loadMore()
        }
    }

    LaunchedEffect(isConfigured) {
        if (isConfigured) {
            viewModel.loadTimeline(isRefresh = true)
        }
    }

    LaunchedEffect(Unit) {
        if (tweets.isEmpty() && isConfigured) {
            viewModel.loadTimeline(isRefresh = true)
        }
    }

    if (pullRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            viewModel.loadTimeline(isRefresh = true)
            pullRefreshState.endRefresh()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 20.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                com.example.xdownloader.ui.components.PageHeading(
                    "媒體動態",
                    "你的追蹤作者 · 圖片與影片",
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { viewModel.loadTimeline(isRefresh = true) }, enabled = !isLoading) {
                    Icon(androidx.compose.material.icons.Icons.Default.Refresh, contentDescription = "重新整理")
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize().nestedScroll(pullRefreshState.nestedScrollConnection)) {
            if (isLoading && tweets.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (errorMessage != null && tweets.isEmpty()) {
                Column(modifier = Modifier.align(Alignment.Center).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(errorMessage!!)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { viewModel.loadTimeline(isRefresh = true) }) {
                        Text("重試 (Retry)")
                    }
                }
            } else if (tweets.isEmpty()) {
                com.example.xdownloader.ui.components.NativeEmptyState(
                    icon = androidx.compose.material.icons.Icons.Default.Image,
                    title = if (!isConfigured) "先連接 X 帳號" else "這裡還沒有媒體",
                    subtitle = if (!isConfigured) "前往設定連接帳號，即可載入追蹤作者的圖片與影片。" else "到探索頁加入作者，這裡就會出現新內容。",
                    action = if (!isConfigured) "前往設定" else "重新整理",
                    onAction = if (!isConfigured) onOpenSettings else { { viewModel.loadTimeline(isRefresh = true) } },
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 20.dp)) {
                    items(tweets, key = { it.tweetId }) { tweet ->
                        TweetCard(tweet)
                    }
                    if (viewModel.hasMore.values.any { it }) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                if (paginationError != null) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(paginationError!!, color = MaterialTheme.colorScheme.error)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(onClick = {
                                            viewModel.dismissPaginationError()
                                            viewModel.loadMore()
                                        }) {
                                            Text("重試載入更多 (Retry)")
                                        }
                                    }
                                } else if (isLoadingMore || (tweets.isEmpty() && viewModel.hasMore.values.any { it })) {
                                    CircularProgressIndicator()
                                }
                            }
                        }
                    } else if (tweets.isNotEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                Text("沒有更多貼文了", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                PullToRefreshContainer(
                    modifier = Modifier.align(Alignment.TopCenter),
                    state = pullRefreshState
                )
            }
        }
    }
}

@Composable
fun TweetCard(tweet: TweetParser.ParsedTweet) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val isFavorite by db.favoriteDao().isFavorite(tweet.tweetId).collectAsState(initial = false)
    var showViewer by remember { mutableStateOf(false) }
    var showDownloadSheet by remember { mutableStateOf(false) }
    val dateText = remember(tweet.createdAt) {
        if (tweet.createdAt > 0) {
            java.text.SimpleDateFormat("MM/dd HH:mm", java.util.Locale.TAIWAN)
                .format(java.util.Date(tweet.createdAt))
        } else ""
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 7.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                com.example.xdownloader.ui.components.AuthorAvatar(tweet.authorAvatar, tweet.authorName)
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(tweet.authorName, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "@" + tweet.authorScreenName + if (dateText.isNotEmpty()) " · " + dateText else "",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                IconButton(onClick = {
                    scope.launch {
                        if (isFavorite) {
                            db.favoriteDao().getFavoriteById(tweet.tweetId)?.let {
                                db.favoriteDao().deleteFavorite(it)
                            }
                        } else {
                            db.favoriteDao().insertFavorite(FavoriteEntity(
                                tweetId = tweet.tweetId,
                                authorName = tweet.authorName,
                                authorScreenName = tweet.authorScreenName,
                                authorAvatar = tweet.authorAvatar,
                                text = tweet.text,
                                createdAt = tweet.createdAt,
                                mediaUrls = tweet.media.joinToString(",") { it.url },
                                mediaTypes = tweet.media.joinToString(",") { it.type.name },
                                mediaThumbnails = tweet.media.joinToString(",") { it.thumbnailUrl }
                            ))
                        }
                    }
                }) {
                    Icon(
                        if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (isFavorite) "取消收藏" else "加入收藏",
                        tint = if (isFavorite) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (tweet.text.isNotBlank()) {
                Text(tweet.text, style = MaterialTheme.typography.bodyMedium)
            }
            com.example.xdownloader.ui.components.MediaPreviewStrip(
                items = tweet.media,
                onClick = { showViewer = true }
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "共 " + tweet.media.size + " 個媒體",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium
                )
                FilledTonalButton(
                    enabled = tweet.media.isNotEmpty(),
                    onClick = { showDownloadSheet = true }
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("下載媒體")
                }
            }
        }
    }

    if (showDownloadSheet && tweet.media.isNotEmpty()) {
        MediaDownloadBottomSheet(tweet.media, onDismiss = { showDownloadSheet = false })
    }
    if (showViewer && tweet.media.isNotEmpty()) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showViewer = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(Modifier.fillMaxSize()) {
                com.example.xdownloader.ui.components.MediaViewer(tweet.media)
                IconButton(
                    onClick = { showViewer = false },
                    modifier = Modifier.align(Alignment.TopStart).padding(16.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "關閉預覽", tint = androidx.compose.ui.graphics.Color.White)
                }
            }
        }
    }
}
