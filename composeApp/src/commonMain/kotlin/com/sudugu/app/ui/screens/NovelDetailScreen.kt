package com.sudugu.app.ui.screens

import com.sudugu.app.ui.nav.Bookshelf
import com.sudugu.app.ui.nav.Category
import com.sudugu.app.ui.nav.CategoryDetail
import com.sudugu.app.ui.nav.ChapterList
import com.sudugu.app.ui.nav.Home
import com.sudugu.app.ui.nav.NovelDetail
import com.sudugu.app.ui.nav.Profile
import com.sudugu.app.ui.nav.Ranking
import com.sudugu.app.ui.nav.Reader
import com.sudugu.app.ui.nav.ReadHistory
import com.sudugu.app.ui.nav.Search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import com.sudugu.app.model.Novel
import com.sudugu.app.ui.components.ErrorView
import com.sudugu.app.ui.components.LoadingIndicator
import com.sudugu.app.viewmodel.SuduguViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovelDetailScreen(vm: SuduguViewModel, nav: NavHostController, id: String) {
    val state by vm.detail.collectAsState()
    val shelf by vm.store.bookshelfFlow.collectAsState()
    LaunchedEffect(id) { vm.loadDetail(id) }

    Scaffold(
        topBar = { TopAppBar(title = { Text(state.data?.title?.take(20) ?: "小说详情") }) },
    ) { inner ->
        when {
            state.loading && state.data == null -> LoadingIndicator()
            state.error != null && state.data == null -> ErrorView(state.error!!) { vm.loadDetail(id) }
            state.data != null -> {
                val novel = state.data!!
                val inShelf = shelf.any { it.id == id }
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(inner),
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(16.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 90.dp, height = 120.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.background),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (novel.cover.isNotBlank()) {
                                    AsyncImage(
                                        model = novel.cover,
                                        contentDescription = novel.title,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop,
                                    )
                                } else {
                                    Text("📖", style = MaterialTheme.typography.titleLarge)
                                }
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    novel.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Spacer(Modifier.height(4.dp))
                                if (novel.author.isNotBlank()) {
                                    Text(
                                        "作者：${novel.author}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    if (novel.status.isNotBlank()) {
                                        Text(
                                            novel.status,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                    if (novel.category.isNotBlank()) {
                                        Text(
                                            novel.category,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    if (novel.words.isNotBlank()) {
                                        Text(
                                            novel.words,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Button(
                                onClick = {
                                    vm.toggleBookshelf(
                                        Novel(
                                            id = novel.id,
                                            title = novel.title,
                                            author = novel.author,
                                            cover = novel.cover,
                                            status = novel.status,
                                            category = novel.category,
                                        )
                                    )
                                },
                                modifier = Modifier.weight(1f),
                            ) { Text(if (inShelf) "已加入书架" else "加入书架") }
                            Button(
                                onClick = {
                                    val progress = vm.readProgress(novel.id)
                                    val targetIdx = progress?.let { p ->
                                        novel.chapters.indexOfFirst { it.id == p.chapterId }
                                    }?.takeIf { it >= 0 } ?: 0
                                    nav.navigate(Reader(novel.id, novel.title, targetIdx))
                                },
                                modifier = Modifier.weight(1f),
                                enabled = novel.chapters.isNotEmpty(),
                            ) {
                                Text(if (vm.readProgress(novel.id) != null) "继续阅读" else "开始阅读")
                            }
                        }

                        if (novel.description.isNotBlank()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(14.dp),
                            ) {
                                Text(
                                    "简介",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    novel.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        if (novel.txtLinks.isNotEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(14.dp),
                            ) {
                                Text(
                                    "TXT 下载",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Spacer(Modifier.height(4.dp))
                                novel.txtLinks.forEach { link ->
                                    Text(
                                        "${link.label}  >",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { /* open link in platform browser */ }
                                            .padding(vertical = 8.dp),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }

                        if (novel.chapters.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    "目录 (${novel.chapters.size}章)",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    "全部章节 >",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.clickable {
                                        nav.navigate(ChapterList(novel.id, novel.title))
                                    },
                                )
                            }
                        }
                    }
                    // Last 10 chapters preview
                    items(novel.chapters.takeLast(10)) { ch ->
                        val progress = vm.readProgress(novel.id)
                        val isCurrent = progress?.chapterId == ch.id
                        Text(
                            text = ch.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface)
                                .clickable {
                                    val idx = novel.chapters.indexOfFirst { it.id == ch.id }
                                    nav.navigate(Reader(novel.id, novel.title, idx))
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}
