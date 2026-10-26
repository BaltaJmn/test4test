package com.baltajmn.test4test

import androidx.compose.runtime.Composable

// Lo poco que la app necesita del sistema y que commonMain no alcanza.

// Si un paquete esta instalado y lo instalo Google Play. Es la prueba de que el
// tester entro de verdad en la prueba cerrada: sin opt-in, Play no deja instalar
// una app que solo existe en un canal cerrado, y un APK suelto no cuenta para
// Play aunque este instalado.
@Composable
expect fun rememberInstalledFromPlay(): (String) -> Boolean

// Boton y gesto de atras del sistema. Sin esto, atras cerraba la app desde
// cualquier pantalla en vez de volver a la anterior.
@Composable
expect fun SystemBack(enabled: Boolean, onBack: () -> Unit)
