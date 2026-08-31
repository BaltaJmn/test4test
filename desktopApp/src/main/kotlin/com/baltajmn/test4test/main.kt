package com.baltajmn.test4test

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Test4Test",
    ) {
        App()
    }
}