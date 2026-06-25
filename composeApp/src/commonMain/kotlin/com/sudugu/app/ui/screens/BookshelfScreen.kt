package com.sudugu.app.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.sudugu.app.ui.components.NovelCard
import com.sudugu.app.ui.nav.Routes
import com.sudugu.app.viewmodel.SuduguViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookshelfScreen(vm: SuduguViewModel, nav: NavHostController) {
    val shelf by vm.store.bookshelfFlow.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("我的书架") }) },
    ) { inner ->
        if (shelf.isEmpty()) {
            Text(
                "书架空空如也，去首页添加喜欢的小说吧",
                modifier = Modifier.fillMaxSize().padding(inner).padding(24.dp),
            )
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(inner)) {
                items(shelf) { book ->
                    NovelCard(
                        novel = com.sudugu.app.model.Novel(
                            id = book.id,
                            title = book.title,
                            author = book.author,
                            cover = book.cover,
                            status = book.status,
                            category = book.category,
                        ),
                    ) { nav.navigate(Routes.novelDetail(book.id)) }
                }
            }
        }
    }
}
