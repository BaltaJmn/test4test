package com.baltajmn.test4test

interface Platform {
    val name: String

    // La unica diferencia funcional permitida entre plataformas (issue #26):
    // la compra premium solo existe en Android.
    val isAndroid: Boolean
}

expect fun getPlatform(): Platform