package com.baltajmn.test4test

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    PageColumn(modifier) {
        Spacer(Modifier.height(64.dp))
        Text("Test4Test", style = MaterialTheme.typography.headlineLarge)
        Text(
            "Consigue testers para tu app de Google Play ayudando a otros a conseguir los suyos.",
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(16.dp))
        OutlinedButton(
            onClick = {
                error = null
                loading = true
                scope.launch {
                    // Mismo camino en Android y Web: OAuth por navegador. En
                    // Android vuelve por el deeplink test4test://login.
                    runCatching { supabase.auth.signInWith(Google) }
                        .onFailure { error = it.message ?: "No se pudo iniciar sesion" }
                    loading = false
                }
            },
            enabled = !loading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Continuar con Google")
        }
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        ErrorText(error)
    }
}
