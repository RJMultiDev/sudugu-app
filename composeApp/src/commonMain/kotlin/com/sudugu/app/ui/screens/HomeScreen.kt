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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.sudugu.app.ui.components.ErrorView
import com.sudugu.app.ui.components.LoadingIndicator
import com.sudugu.app.ui.components.NovelCard
import com.sudugu.app.viewmodel.SuduguViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: SuduguViewModel, nav: NavHostController) {
    val state by vm.home.collectAsState()
    LaunchedEffect(Unit) { vm.loadHome() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("速读谷", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { nav.navigate(Search) }) {
                        Icon(Icons.Filled.Search, contentDescription = "搜索")
                    }
                },
            )
        },
    ) { inner ->
        when {
            state.loading && state.data == null -> LoadingIndicator()
            state.error != null && state.data == null -> ErrorView(state.error!!) { vm.loadHome() }
            state.data != null -> {
                val data = state.data!!
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(inner),
                    contentPadding = PaddingValues(bottom = 16.dp),
                ) {
                    item {
                        Text(
                            "最新更新",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(start = 12.dp, top = 12.dp, bottom = 4.dp),
                        )
                    }
                    items(data.latestUpdates) { novel ->
                        NovelCard(novel) { nav.navigate(NovelDetail(novel.id)) }
                    }

                    item {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "排行榜",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 4.dp),
                        )
                    }
                    items(data.rankings) { novel ->
                        NovelCard(novel) { nav.navigate(NovelDetail(novel.id)) }
                    }

                    item {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "完结小说",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 4.dp),
                        )
                    }
                    items(data.completedNovels) { novel ->
                        NovelCard(novel) { nav.navigate(NovelDetail(novel.id)) }
                    }
                }
            }
        }
    }
}
