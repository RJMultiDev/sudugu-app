package com.sudugu.app.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.sudugu.app.model.ReaderFontSize
import com.sudugu.app.ui.components.ErrorView
import com.sudugu.app.ui.components.LoadingIndicator
import com.sudugu.app.ui.nav.Routes
import com.sudugu.app.ui.theme.AppColors
import com.sudugu.app.viewmodel.SuduguViewModel
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    vm: SuduguViewModel,
    nav: NavHostController,
    bookId: String,
    bookTitle: String,
    chapterIndex: Int,
) {
    val state by vm.chapter.collectAsState()
    val fontSize by vm.store.readerFontSizeFlow.collectAsState()
    val theme by vm.store.themeFlow.collectAsState()
    val isDark = theme == com.sudugu.app.model.ThemeMode.DARK
    val readerBg = if (isDark) AppColors.darkReaderBg else AppColors.lightReaderBg
    val readerText = if (isDark) AppColors.darkReaderText else AppColors.lightReaderText
    val listState = rememberLazyListState()
    var showSettings by remember { mutableStateOf(false) }

    // Map: chapter index from previous screen → chapter id from detail. We
    // need to know how many chapters the book has so navigation works. We
    // store a small cache of "current chapter ids" via the chaptersCache
    // module, but for simplicity we use the loaded chapter and let prev/next
    // cycle the chapter id. Since this is the only reader screen entry point
    // coming from `Routes.reader` with an *index*, we keep the index in state
    // and rely on `loadChapter(bookId, chapterId)` using whatever id we pass.
    var currentIndex by remember { mutableStateOf(chapterIndex) }
    var currentChapterId by remember { mutableStateOf<String?>(null) }

    // First time we land here we don't yet know the chapter id; pull detail
    // to get the id list. The detail will set up an initial load.
    val detail by vm.detail.collectAsState()
    LaunchedEffect(bookId) {
        if (detail.data?.id != bookId) vm.loadDetail(bookId)
    }
    LaunchedEffect(detail.data, currentIndex) {
        val ch = detail.data?.chapters?.getOrNull(currentIndex)
        if (ch != null && ch.id != currentChapterId) {
            currentChapterId = ch.id
            vm.loadChapter(bookId, ch.id)
        }
    }

    // Persist scroll position (debounced)
    val scope = rememberCoroutineScope()
    LaunchedEffect(currentChapterId) {
        if (currentChapterId == null) return@LaunchedEffect
        snapshotFlow { listState.firstVisibleItemScrollOffset }
            .distinctUntilChanged()
            .collect { offset ->
                if (offset > 0) {
                    state.data?.let { c ->
                        vm.saveScrollPosition(bookId, c.chapterId, c.chapterTitle, bookTitle, offset.toFloat())
                    }
                }
            }
    }
    DisposableEffect(Unit) {
        onDispose { /* nothing to clean up */ }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(state.data?.chapterTitle ?: "", maxLines = 1, fontWeight = FontWeight.SemiBold)
                        Text(
                            "$bookTitle · 第 ${currentIndex + 1} 章",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showSettings = !showSettings }) {
                        Text("Aa", style = MaterialTheme.typography.titleMedium)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        containerColor = readerBg,
    ) { inner ->
        Box(Modifier.fillMaxSize().padding(inner).background(readerBg)) {
            when {
                state.loading && state.data == null -> LoadingIndicator("加载章节...")
                state.error != null && state.data == null -> ErrorView(state.error!!) { state.data?.let { vm.loadChapter(bookId, it.chapterId) } }
                state.data != null -> {
                    val c = state.data!!
                    val paragraphs = c.content.split("\n\n").filter { it.isNotBlank() }
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(20.dp),
                    ) {
                        item {
                            Text(
                                text = c.chapterTitle,
                                fontSize = (fontSize.size + 6).sp,
                                fontWeight = FontWeight.Bold,
                                color = readerText,
                                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                            )
                        }
                        items(paragraphs.size) { i ->
                            Text(
                                text = paragraphs[i],
                                fontSize = fontSize.size.sp,
                                color = readerText,
                                lineHeight = (fontSize.size * 1.8).sp,
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            )
                        }
                        item {
                            Spacer(Modifier.height(20.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    "上一章",
                                    modifier = Modifier
                                        .clickable(enabled = currentIndex > 0) { currentIndex-- }
                                        .padding(12.dp),
                                    color = if (currentIndex > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                )
                                Text(
                                    "目录",
                                    modifier = Modifier
                                        .clickable { nav.navigate(Routes.chapterList(bookId, bookTitle)) }
                                        .padding(12.dp),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    "下一章",
                                    modifier = Modifier
                                        .clickable(enabled = currentIndex < (detail.data?.chapters?.size ?: 0) - 1) { currentIndex++ }
                                        .padding(12.dp),
                                    color = if (currentIndex < (detail.data?.chapters?.size ?: 0) - 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                )
                            }
                            Spacer(Modifier.height(40.dp))
                        }
                    }
                }
            }
            if (showSettings) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(16.dp),
                ) {
                    Text("字体大小", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ReaderFontSize.entries.forEach { size ->
                            val selected = fontSize == size
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background,
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
                                    )
                                    .clickable { vm.store.setReaderFontSize(size) }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                            ) {
                                Text(
                                    when (size) {
                                        ReaderFontSize.SMALL -> "小"
                                        ReaderFontSize.MEDIUM -> "中"
                                        ReaderFontSize.LARGE -> "大"
                                        ReaderFontSize.XLARGE -> "特大"
                                    },
                                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// LazyColumn items helper that avoids an import-once cost
