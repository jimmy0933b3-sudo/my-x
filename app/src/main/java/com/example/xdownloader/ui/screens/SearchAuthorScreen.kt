package com.example.xdownloader.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.xdownloader.api.ApiResult
import com.example.xdownloader.api.TweetParser
import com.example.xdownloader.api.XApiClient
import com.example.xdownloader.database.AppDatabase
import com.example.xdownloader.database.FollowEntity
import com.example.xdownloader.ui.components.NativeEmptyState
import com.example.xdownloader.ui.components.PageHeading
import kotlinx.coroutines.launch

@Composable
fun SearchAuthorScreen() {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val api = remember { XApiClient(context) }
    val scope = rememberCoroutineScope()
    val following by db.followDao().getAllFollowing().collectAsState(initial = emptyList())

    var author by remember { mutableStateOf("") }
    var link by remember { mutableStateOf("") }
    var isFollowing by remember { mutableStateOf(false) }
    var isFetching by remember { mutableStateOf(false) }
    var tweet by remember { mutableStateOf<TweetParser.ParsedTweet?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            PageHeading("探索與搜尋", "加入喜愛的作者，或直接解析貼文連結")
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PersonSearch, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("追蹤作者", style = MaterialTheme.typography.titleLarge)
                    }
                    Text("只會加入本 App 的動態名單，不會改變 X 上的追蹤關係。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = author,
                        onValueChange = { author = it; error = null },
                        label = { Text("X 使用者名稱") },
                        placeholder = { Text("例如：@username") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                    )
                    Button(
                        onClick = {
                            val username = author.trim().removePrefix("@")
                            if (username.isBlank() || isFollowing) return@Button
                            scope.launch {
                                isFollowing = true
                                try {
                                    when (val result = api.getUserIdByScreenName(username)) {
                                        is ApiResult.Success -> {
                                            db.followDao().insertFollow(
                                                FollowEntity(result.data, username, username, "", true)
                                            )
                                            author = ""
                                            Toast.makeText(context, "已加入 @" + username, Toast.LENGTH_SHORT).show()
                                        }
                                        is ApiResult.Unauthorized -> error = result.message
                                        is ApiResult.RateLimited -> error = result.message
                                        is ApiResult.NetworkError -> error = result.message
                                        is ApiResult.ParsingError -> error = result.message
                                    }
                                } catch (e: Exception) {
                                    error = e.message ?: "加入作者失敗"
                                } finally {
                                    isFollowing = false
                                }
                            }
                        },
                        enabled = author.isNotBlank() && !isFollowing,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isFollowing) CircularProgressIndicator(modifier = Modifier.size(18.dp))
                        else {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("加入我的動態")
                        }
                    }
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Link, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.width(8.dp))
                        Text("貼上貼文連結", style = MaterialTheme.typography.titleLarge)
                    }
                    OutlinedTextField(
                        value = link,
                        onValueChange = { link = it; error = null },
                        label = { Text("貼文網址") },
                        placeholder = { Text("https://x.com/.../status/...") },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                    FilledTonalButton(
                        enabled = link.isNotBlank() && !isFetching,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            val id = Regex("status/(\\d+)").find(link)?.groupValues?.get(1)
                            if (id == null) {
                                error = "請貼上有效的 X 貼文網址"
                            } else {
                                scope.launch {
                                    isFetching = true
                                    error = null
                                    try {
                                        when (val result = api.getTweet(id)) {
                                            is ApiResult.Success -> {
                                                val obj = result.data.getAsJsonObject("data")
                                                    ?.getAsJsonObject("tweetResult")
                                                    ?.getAsJsonObject("result")
                                                tweet = obj?.let { TweetParser.parseSingleTweetResult(it) }
                                                if (tweet == null) error = "貼文沒有可解析的媒體，或 X 已變更回傳格式"
                                            }
                                            is ApiResult.Unauthorized -> error = result.message
                                            is ApiResult.RateLimited -> error = result.message
                                            is ApiResult.NetworkError -> error = result.message
                                            is ApiResult.ParsingError -> error = result.message
                                        }
                                    } catch (e: Exception) {
                                        error = e.message ?: "讀取貼文失敗"
                                    } finally {
                                        isFetching = false
                                    }
                                }
                            }
                        }
                    ) {
                        if (isFetching) CircularProgressIndicator(modifier = Modifier.size(18.dp))
                        else Text("解析媒體")
                    }
                }
            }
        }
        if (error != null) {
            item {
                Text(
                    requireNotNull(error),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        if (tweet != null) {
            item { TweetCard(requireNotNull(tweet)) }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                Text("我的追蹤清單", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                Text(following.size.toString() + " 位", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (following.isEmpty()) {
            item {
                NativeEmptyState(
                    Icons.Default.PersonSearch,
                    "追蹤名單還是空的",
                    "在上方輸入使用者名稱即可新增"
                )
            }
        } else {
            items(following, key = { it.authorId }) { user ->
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)
                    ) {
                        com.example.xdownloader.ui.components.AuthorAvatar(user.authorAvatar, user.authorName)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(user.authorName, style = MaterialTheme.typography.titleSmall)
                            Text("@" + user.authorScreenName, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = {
                            scope.launch {
                                db.followDao().deleteFollow(user)
                            }
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "移除作者")
                        }
                    }
                }
            }
        }
    }
}
