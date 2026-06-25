package com.sudugu.app.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.sudugu.app.ui.components.ErrorView
import com.sudugu.app.ui.components.LoadingIndicator
import com.sudugu.app.ui.components.NovelCard
import com.sudugu.app.ui.nav.Routes
import com.sudugu.app.viewmodel.SuduguViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDetailScreen(vm: SuduguViewModel, nav: NavHostController, slug: String, name: String) {
    val state by vm.categoryDetail.collectAsState()
    LaunchedEffect(slug) { vm.loadCategoryDetail(slug, 1) }

    Scaffold(
        topBar = { TopAppBar(title = { Text(name) }) },
    ) { inner ->
        Column(Modifier.fillMaxSize().padding(inner)) {
            when {
                state.loading && state.items.isEmpty() -> LoadingIndicator()
                state.error != null && state.items.isEmpty() -> ErrorView(state.error!!) { vm.loadCategoryDetail(slug, 1) }
                else -> {
                    LazyColumn(Modifier.weight(1f)) {
                        items(state.items) { novel ->
                            NovelCard(novel) { nav.navigate(Routes.novelDetail(novel.id)) }
                        }
                    }
                    PaginationBar(
                        page = state.page,
                        totalPages = state.totalPages,
                        onPrev = { if (state.page > 1) vm.loadCategoryDetail(slug, state.page - 1) },
                        onNext = { if (state.page < state.totalPages) vm.loadCategoryDetail(slug, state.page + 1) },
                    )
                }
            }
        }
    }
}

@Composable
fun PaginationBar(page: Int, totalPages: Int, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Button(onClick = onPrev, enabled = page > 1) { Text("上一页") }
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Text("第 $page / $totalPages 页", color = MaterialTheme.colorScheme.onSurface)
        }
        Button(onClick = onNext, enabled = page < totalPages) { Text("下一页") }
    }
}
