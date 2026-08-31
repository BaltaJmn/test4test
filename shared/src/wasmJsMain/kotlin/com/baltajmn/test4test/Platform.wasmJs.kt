package com.baltajmn.test4test

class WasmPlatform: Platform {
    override val name: String = "Web with Kotlin/Wasm"
    override val isAndroid: Boolean = false
}

actual fun getPlatform(): Platform = WasmPlatform()