package com.baltajmn.test4test

interface Platform {
    val name: String

    // La unica diferencia funcional permitida entre plataformas (issue #26):
    // la compra premium solo existe en Android.
    val isAndroid: Boolean
}

expect fun getPlatform(): Platform

// Id de app con el que arranca la sesion, o null para empezar en el feed. En Web
// viaja en el hash de la URL, que es lo que hace compartible una ficha; en
// Android no hay nada que leer y siempre se abre por el feed.
expect fun startAppId(): String?
