package com.baltajmn.test4test

import io.github.jan.supabase.auth.CodeVerifierCache
import io.github.jan.supabase.auth.SessionManager
import io.ktor.client.engine.HttpClientEngine

// Solo para las pruebas de las capturas de tienda: un motor HTTP propio, para que el
// cliente de Supabase hable con datos de demo en vez de con la red, y una sesion y
// una cache del verificador en memoria, porque en una prueba del host no hay
// SharedPreferences que leer. En la app todos son siempre null y el cliente usa los
// de cada plataforma, igual que antes. Tienen que ponerse antes del primer uso de
// `supabase`, que se crea una sola vez.
internal object TestOverrides {
    var engine: HttpClientEngine? = null
    var sessionManager: SessionManager? = null
    var codeVerifierCache: CodeVerifierCache? = null
}
