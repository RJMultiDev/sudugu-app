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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.sudugu.app.ui.components.ErrorView
import com.sudugu.app.ui.components.LoadingIndicator
import com.sudugu.app.viewmodel.SuduguViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(vm: SuduguViewModel, nav: NavHostController) {
    val state by vm.categories.collectAsState()
    LaunchedEffect(Unit) { vm.loadCategories() }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("小说分类", fontWeight = FontWeight.Bold) })
        },
    ) { inner ->
        when {
            state.loading && state.categories.isEmpty() -> LoadingIndicator()
            state.error != null && state.categories.isEmpty() -> ErrorView(state.error!!) { vm.loadCategories() }
            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize().padding(inner),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.categories) { c ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { nav.navigate(CategoryDetail(c.slug, c.name)) }
                            .padding(12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(c.name, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                "${c.count} 部",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
