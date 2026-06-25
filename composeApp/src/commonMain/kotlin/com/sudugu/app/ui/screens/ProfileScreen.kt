package com.sudugu.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.sudugu.app.model.ThemeMode
import com.sudugu.app.viewmodel.SuduguViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(vm: SuduguViewModel, nav: NavHostController) {
    val theme by vm.store.themeFlow.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("我的") }) },
    ) { inner ->
        Column(Modifier.fillMaxSize().padding(inner)) {
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        vm.store.setThemeMode(if (theme == ThemeMode.DARK) ThemeMode.LIGHT else ThemeMode.DARK)
                    }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    if (theme == ThemeMode.DARK) Icons.Default.Brightness7 else Icons.Default.Brightness4,
                    contentDescription = "切换主题",
                )
                Spacer(Modifier.height(0.dp).padding(start = 12.dp))
                Text(if (theme == ThemeMode.DARK) "切换到日间模式" else "切换到夜间模式", modifier = Modifier.padding(start = 12.dp))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { nav.navigate("ranking") }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.Leaderboard, contentDescription = "排行榜")
                Text("排行榜", modifier = Modifier.padding(start = 12.dp))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { nav.navigate("read_history") }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.History, contentDescription = "阅读记录")
                Text("阅读记录", modifier = Modifier.padding(start = 12.dp))
            }
        }
    }
}
