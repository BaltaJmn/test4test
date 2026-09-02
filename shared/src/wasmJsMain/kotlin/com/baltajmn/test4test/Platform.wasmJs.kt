package com.baltajmn.test4test

class WasmPlatform: Platform {
    override val name: String = "Web with Kotlin/Wasm"
    override val isAndroid: Boolean = false
}

actual fun getPlatform(): Platform = WasmPlatform()

// js() como cuerpo completo: en wasmJs no vale dentro de una expresion.
@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
private fun locationHash(): String = js("window.location.hash")

actual fun startAppId(): String? = locationHash().removePrefix("#").takeIf { it.isNotBlank() }
