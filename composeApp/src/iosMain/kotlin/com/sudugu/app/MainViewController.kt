package com.sudugu.app

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController = ComposeUIViewController {
    val vm = rememberViewModel()
    SuduguApp(vm)
}
