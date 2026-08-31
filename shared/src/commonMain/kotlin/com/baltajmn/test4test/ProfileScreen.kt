package com.baltajmn.test4test

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    uid: String,
    me: Profile?,
    modifier: Modifier = Modifier,
    onOpen: (String) -> Unit,
    onPaywall: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var mine by remember { mutableStateOf<List<AppRow>>(emptyList()) }
    var testing by remember { mutableStateOf<List<AppRow>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(uid) {
        runCatching {
            mine = myApps(uid)
            testing = appsByIds(testerAppIds(uid).toList())
        }.onFailure { error = it.message ?: "No se pudo cargar el perfil" }
        loaded = true
    }

    if (!loaded) {
        Loading()
        return
    }

    PageLazyColumn(modifier) {
        item {
            Text(me?.displayName ?: "Usuario", style = MaterialTheme.typography.headlineSmall)
        }
        item {
            if (me?.isPremium == true) {
                Text("Premium: slots ilimitados", style = MaterialTheme.typography.labelLarge)
            } else {
                OutlinedButton(onClick = onPaywall, modifier = Modifier.fillMaxWidth()) {
                    Text("Plan gratuito - 1 app")
                }
            }
        }
        item { ErrorText(error) }

        item { Text("Mis apps", style = MaterialTheme.typography.titleMedium) }
        if (mine.isEmpty()) {
            item { Text("Ninguna todavia.", style = MaterialTheme.typography.bodyMedium) }
        }
        for (app in mine) {
            item(key = "mine-${app.id}") { AppCard(app, onClick = { onOpen(app.id) }) }
        }

        item { Text("Apps donde soy tester", style = MaterialTheme.typography.titleMedium) }
        if (testing.isEmpty()) {
            item { Text("Aun no te has unido a ninguna.", style = MaterialTheme.typography.bodyMedium) }
        }
        for (app in testing) {
            item(key = "testing-${app.id}") { AppCard(app, onClick = { onOpen(app.id) }) }
        }

        item {
            Button(
                onClick = { scope.launch { supabase.auth.signOut() } },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Cerrar sesion")
            }
        }
    }
}
