package com.sudugu.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.sudugu.app.data.SuduguApi
import com.sudugu.app.data.platformEngineFactory
import com.sudugu.app.storage.LocalStore
import com.sudugu.app.storage.createSettings
import com.sudugu.app.ui.nav.SuduguNavHost
import com.sudugu.app.ui.theme.SuduguTheme
import com.sudugu.app.viewmodel.SuduguViewModel

/**
 * Root composable for all platforms.
 * Each platform provides its own MainActivity / AppDelegate / main() that
 * calls this with a freshly constructed [SuduguViewModel].
 */
@Composable
fun SuduguApp(vm: SuduguViewModel) {
    val theme by vm.store.themeFlow.collectAsState()
    SuduguTheme(mode = theme) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            SuduguNavHost(vm)
        }
    }
}

@Composable
fun rememberViewModel(): SuduguViewModel {
    val settings = remember { createSettings() }
    val store = remember(settings) { LocalStore(settings).also { it.loadAll() } }
    val api = remember { SuduguApi(platformEngineFactory) }
    val vm = remember { SuduguViewModel(api, store) }
    return vm
}
