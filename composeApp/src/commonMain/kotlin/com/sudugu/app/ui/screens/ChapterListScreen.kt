package com.sudugu.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.sudugu.app.ui.components.LoadingIndicator
import com.sudugu.app.ui.nav.Routes
import com.sudugu.app.viewmodel.SuduguViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterListScreen(
    vm: SuduguViewModel,
    nav: NavHostController,
    bookId: String,
    bookTitle: String,
) {
    val detail by vm.detail.collectAsState()
    LaunchedEffect(bookId) { vm.loadDetail(bookId) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("章节目录") }) },
    ) { inner ->
        if (detail.loading && detail.data == null) {
            LoadingIndicator()
        } else {
            val chapters = detail.data?.chapters.orEmpty()
            Column(Modifier.fillMaxSize().padding(inner)) {
                Text(
                    "$bookTitle · 共${chapters.size}章",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                )
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    items(chapters) { ch ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val idx = chapters.indexOfFirst { it.id == ch.id }
                                    nav.navigate(Routes.reader(bookId, bookTitle, idx))
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                        ) {
                            Text(
                                ch.title,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(0.5.dp)
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                        )
                    }
                }
            }
        }
    }
}
