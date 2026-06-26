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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.sudugu.app.ui.components.ErrorView
import com.sudugu.app.ui.components.LoadingIndicator
import com.sudugu.app.ui.components.NovelCard
import com.sudugu.app.viewmodel.SuduguViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(vm: SuduguViewModel, nav: NavHostController) {
    val state by vm.search.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("搜索小说") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { inner ->
        Column(Modifier.fillMaxSize().padding(inner)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = state.keyword,
                    onValueChange = { vm.setSearchKeyword(it) },
                    placeholder = { Text("输入书名 / 作者") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = { vm.doSearch() }) {
                    Icon(Icons.Filled.Search, contentDescription = "搜索")
                }
            }

            when {
                state.loading -> LoadingIndicator()
                state.error != null -> ErrorView(state.error!!) { vm.doSearch() }
                state.results.isNotEmpty() -> {
                    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                        items(state.results) { novel ->
                            NovelCard(novel) { nav.navigate(NovelDetail(novel.id)) }
                        }
                    }
                }
                state.keyword.isNotBlank() && !state.loading -> {
                    Text(
                        "没有找到结果",
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
