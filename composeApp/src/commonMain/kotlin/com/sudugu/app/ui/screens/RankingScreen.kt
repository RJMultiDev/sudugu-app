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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.sudugu.app.ui.components.ErrorView
import com.sudugu.app.ui.components.LoadingIndicator
import com.sudugu.app.ui.components.NovelCard
import com.sudugu.app.viewmodel.SuduguViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RankingScreen(vm: SuduguViewModel, nav: NavHostController) {
    val state by vm.ranking.collectAsState()
    LaunchedEffect(Unit) { vm.loadRanking(1) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("排行榜") }) },
    ) { inner ->
        Column(Modifier.fillMaxSize().padding(inner)) {
            when {
                state.loading && state.items.isEmpty() -> LoadingIndicator()
                state.error != null && state.items.isEmpty() -> ErrorView(state.error!!) { vm.loadRanking(1) }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 24.dp),
                    ) {
                        items(state.items) { novel ->
                            NovelCard(novel) { nav.navigate(NovelDetail(novel.id)) }
                        }
                    }
                    PaginationBar(
                        page = state.page,
                        totalPages = state.totalPages,
                        onPrev = { if (state.page > 1) vm.loadRanking(state.page - 1) },
                        onNext = { if (state.page < state.totalPages) vm.loadRanking(state.page + 1) },
                    )
                }
            }
        }
    }
}
