package com.baltajmn.test4test

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

// Un unico cliente para toda la app. supabase-kt ya persiste la sesion
// (SharedPreferences en Android, localStorage en Web) y refresca el token
// expirado el solo, asi que no hay storage propio por plataforma que escribir.
val supabase = createSupabaseClient(SupabaseConfig.URL, SupabaseConfig.ANON_KEY) {
    install(Auth) {
        // Vuelta del OAuth de Google en Android: test4test://login.
        // En Web la vuelta es por redirect y la resuelve el propio plugin.
        scheme = "test4test"
        host = "login"
    }
    install(Postgrest)
}
