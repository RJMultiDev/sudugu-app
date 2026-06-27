package com.sudugu.app

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.CanvasBasedWindow

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    CanvasBasedWindow("Sudugu") {
        val vm = rememberViewModel()
        SuduguApp(vm)
    }
}
