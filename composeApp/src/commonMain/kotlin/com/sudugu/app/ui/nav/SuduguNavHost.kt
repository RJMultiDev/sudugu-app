package com.sudugu.app.ui.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.sudugu.app.ui.screens.BookshelfScreen
import com.sudugu.app.ui.screens.CategoryDetailScreen
import com.sudugu.app.ui.screens.CategoryScreen
import com.sudugu.app.ui.screens.ChapterListScreen
import com.sudugu.app.ui.screens.HomeScreen
import com.sudugu.app.ui.screens.NovelDetailScreen
import com.sudugu.app.ui.screens.ProfileScreen
import com.sudugu.app.ui.screens.RankingScreen
import com.sudugu.app.ui.screens.ReadHistoryScreen
import com.sudugu.app.ui.screens.ReaderScreen
import com.sudugu.app.ui.screens.SearchScreen
import com.sudugu.app.viewmodel.SuduguViewModel
import kotlinx.serialization.Serializable

private data class TabItem(val route: Any, val label: String, val icon: ImageVector)

private val tabs = listOf(
    TabItem(Home, "首页", Icons.Default.Home),
    TabItem(Category, "分类", Icons.Default.Search),
    TabItem(Bookshelf, "书架", Icons.Default.Search),
    TabItem(Profile, "我的", Icons.Default.Person),
)

@Serializable object Home
@Serializable object Category
@Serializable object Bookshelf
@Serializable object Profile
@Serializable object Search
@Serializable object Ranking
@Serializable object ReadHistory
@Serializable data class CategoryDetail(val slug: String, val name: String)
@Serializable data class NovelDetail(val id: String)
@Serializable data class Reader(val bookId: String, val bookTitle: String, val chapterIndex: Int)
@Serializable data class ChapterList(val bookId: String, val bookTitle: String)

@Composable
fun SuduguNavHost(vm: SuduguViewModel) {
    val nav = rememberNavController()
    Scaffold(
        bottomBar = { BottomBar(nav) },
    ) { inner ->
        Box(Modifier.fillMaxSize().padding(inner)) {
            NavHost(navController = nav, startDestination = Home) {
                composable<Home> { HomeScreen(vm, nav) }
                composable<Category> { CategoryScreen(vm, nav) }
                composable<Bookshelf> { BookshelfScreen(vm, nav) }
                composable<Profile> { ProfileScreen(vm, nav) }
                composable<Search> { SearchScreen(vm, nav) }
                composable<Ranking> { RankingScreen(vm, nav) }
                composable<ReadHistory> { ReadHistoryScreen(vm, nav) }
                composable<CategoryDetail> { entry ->
                    val r = entry.toRoute<CategoryDetail>()
                    CategoryDetailScreen(vm, nav, r.slug, r.name)
                }
                composable<NovelDetail> { entry ->
                    val r = entry.toRoute<NovelDetail>()
                    NovelDetailScreen(vm, nav, r.id)
                }
                composable<Reader> { entry ->
                    val r = entry.toRoute<Reader>()
                    ReaderScreen(vm, nav, r.bookId, r.bookTitle, r.chapterIndex)
                }
                composable<ChapterList> { entry ->
                    val r = entry.toRoute<ChapterList>()
                    ChapterListScreen(vm, nav, r.bookId, r.bookTitle)
                }
            }
        }
    }
}

@Composable
private fun BottomBar(nav: NavHostController) {
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route
    NavigationBar {
        tabs.forEach { tab ->
            val isCurrent = when (val r = tab.route) {
                Home -> current?.contains("Home") == true
                Category -> current?.contains("Category") == true
                Bookshelf -> current?.contains("Bookshelf") == true
                Profile -> current?.contains("Profile") == true
                else -> false
            }
            NavigationBarItem(
                selected = isCurrent,
                onClick = {
                    nav.navigate(tab.route) {
                        popUpTo(nav.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label, color = MaterialTheme.colorScheme.onSurface) },
            )
        }
    }
}
