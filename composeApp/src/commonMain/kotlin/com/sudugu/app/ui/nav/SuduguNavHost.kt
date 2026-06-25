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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
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

private data class TabItem(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    TabItem("home", "首页", Icons.Default.Home),
    TabItem("category", "分类", Icons.Default.Search),
    TabItem("bookshelf", "书架", Icons.Default.Search),
    TabItem("profile", "我的", Icons.Default.Person),
)

@Composable
fun SuduguNavHost(vm: SuduguViewModel) {
    val nav = rememberNavController()
    Scaffold(
        bottomBar = { BottomBar(nav) },
    ) { inner ->
        Box(Modifier.fillMaxSize().padding(inner)) {
            NavHost(navController = nav, startDestination = "home") {
                composable("home") { HomeScreen(vm, nav) }
                composable("category") { CategoryScreen(vm, nav) }
                composable("bookshelf") { BookshelfScreen(vm, nav) }
                composable("profile") { ProfileScreen(vm, nav) }

                composable("search") { SearchScreen(vm, nav) }
                composable("ranking") { RankingScreen(vm, nav) }
                composable("read_history") { ReadHistoryScreen(vm, nav) }

                composable(
                    route = "category_detail/{slug}/{name}",
                    arguments = listOf(
                        navArgument("slug") { type = NavType.StringType },
                        navArgument("name") { type = NavType.StringType },
                    ),
                ) { entry ->
                    val slug = entry.arguments?.getString("slug").orEmpty()
                    val name = com.sudugu.app.util.Url.decode(entry.arguments?.getString("name").orEmpty())
                    CategoryDetailScreen(vm, nav, slug, name)
                }

                composable(
                    route = "novel_detail/{id}",
                    arguments = listOf(navArgument("id") { type = NavType.StringType }),
                ) { entry ->
                    val id = entry.arguments?.getString("id").orEmpty()
                    NovelDetailScreen(vm, nav, id)
                }

                composable(
                    route = "reader/{bookId}/{bookTitle}/{chapterIndex}",
                    arguments = listOf(
                        navArgument("bookId") { type = NavType.StringType },
                        navArgument("bookTitle") { type = NavType.StringType },
                        navArgument("chapterIndex") { type = NavType.IntType },
                    ),
                ) { entry ->
                    val bookId = entry.arguments?.getString("bookId").orEmpty()
                    val bookTitle = com.sudugu.app.util.Url.decode(entry.arguments?.getString("bookTitle").orEmpty())
                    val chapterIndex = entry.arguments?.getInt("chapterIndex") ?: 0
                    ReaderScreen(vm, nav, bookId, bookTitle, chapterIndex)
                }

                composable(
                    route = "chapter_list/{bookId}/{bookTitle}",
                    arguments = listOf(
                        navArgument("bookId") { type = NavType.StringType },
                        navArgument("bookTitle") { type = NavType.StringType },
                    ),
                ) { entry ->
                    val bookId = entry.arguments?.getString("bookId").orEmpty()
                    val bookTitle = com.sudugu.app.util.Url.decode(entry.arguments?.getString("bookTitle").orEmpty())
                    ChapterListScreen(vm, nav, bookId, bookTitle)
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
            NavigationBarItem(
                selected = current == tab.route,
                onClick = {
                    if (current != tab.route) {
                        nav.navigate(tab.route) {
                            popUpTo(nav.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label, color = MaterialTheme.colorScheme.onSurface) },
            )
        }
    }
}
