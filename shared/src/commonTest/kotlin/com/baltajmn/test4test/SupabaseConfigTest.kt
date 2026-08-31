package com.baltajmn.test4test

import kotlin.test.Test
import kotlin.test.assertTrue

class SupabaseConfigTest {

    @Test
    fun credentialsAreInjectedAtBuildTime() {
        assertTrue(
            SupabaseConfig.URL.startsWith("https://") && SupabaseConfig.URL.endsWith(".supabase.co"),
            "URL de Supabase mal inyectada: ${SupabaseConfig.URL}",
        )
        // Cabecera de un JWT ("{\"alg\"..." en base64url). No imprimimos la clave si falla.
        assertTrue(SupabaseConfig.ANON_KEY.startsWith("eyJ"), "anon key mal inyectada")
    }
}
