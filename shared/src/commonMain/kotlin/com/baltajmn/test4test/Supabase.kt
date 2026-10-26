package com.baltajmn.test4test

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

// Un unico cliente para toda la app. supabase-kt ya persiste la sesion
// (SharedPreferences) y refresca el token expirado el solo, asi que no hay
// storage propio que escribir.
val supabase = createSupabaseClient(SupabaseConfig.URL, SupabaseConfig.ANON_KEY) {
    install(Auth) {
        // Vuelta del OAuth de Google: test4test://login.
        scheme = "test4test"
        host = "login"
    }
    install(Postgrest)
}
