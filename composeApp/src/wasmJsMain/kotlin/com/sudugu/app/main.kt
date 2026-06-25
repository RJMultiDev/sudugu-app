package com.sudugu.app

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.CanvasBasedWindow
import org.jetbrains.skiko.wasm.onWasmReady

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    onWasmReady {
        val body = kotlinx.browser.document.body!!
        val canvas = kotlinx.browser.document.createElement("canvas")
        body.appendChild(canvas)
        // Fallback to CanvasBasedWindow for KMP web
        CanvasBasedWindow(
            canvasElementId = "sudugu-canvas",
        ) {
            val vm = rememberViewModel()
            SuduguApp(vm)
        }
    }
}
