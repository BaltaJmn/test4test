package com.baltajmn.test4test

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch

// Alta y edicion comparten formulario (issue #16): existing == null es alta.
@Composable
fun CreateAppScreen(
    uid: String,
    me: Profile?,
    existing: AppRow?,
    modifier: Modifier = Modifier,
    onDone: () -> Unit,
    onPaywall: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(existing?.name.orEmpty()) }
    var groups by remember { mutableStateOf(existing?.googleGroupsUrl ?: "https://") }
    var play by remember { mutableStateOf(existing?.playStoreUrl ?: "https://") }
    var optIn by remember { mutableStateOf(existing?.optInUrl ?: "https://") }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }

    PageColumn(modifier) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Nombre de la app") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = groups,
            onValueChange = { groups = it },
            label = { Text("Enlace de Google Groups") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = play,
            onValueChange = { play = it },
            label = { Text("Enlace de Play Store") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = optIn,
            onValueChange = { optIn = it },
            label = { Text("Enlace de opt-in") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        ErrorText(error)
        if (saving) LinearProgressIndicator(Modifier.fillMaxWidth())
        Button(
            enabled = !saving,
            onClick = {
                val invalid = appFormError(name, groups, play, optIn)
                error = invalid
                if (invalid != null) return@Button
                saving = true
                scope.launch {
                    val input = AppInput(
                        ownerId = uid,
                        name = name.trim(),
                        googleGroupsUrl = groups.trim(),
                        playStoreUrl = play.trim(),
                        optInUrl = optIn.trim(),
                    )
                    runCatching {
                        if (existing == null) createApp(input) else updateApp(existing.id, input)
                    }
                        .onSuccess { onDone() }
                        .onFailure { failure ->
                            // Issue #15: un alta rechazada por la policy
                            // apps_insert_own_within_slots devuelve un mensaje de
                            // Postgres generico, asi que no se parsea el error: se
                            // vuelven a contar las apps propias y se decide con la
                            // misma regla que aplica la RLS. Si el recuento falla,
                            // gana el mensaje de error en vez de un paywall a ciegas.
                            val owned = runCatching { myApps(uid).size }.getOrNull()
                            val outOfSlots = existing == null && owned != null &&
                                !canCreateApp(me?.isPremium == true, owned)
                            if (outOfSlots) onPaywall()
                            else error = failure.message ?: "No se pudo guardar"
                        }
                    saving = false
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (existing == null) "Crear app" else "Guardar cambios")
        }
    }
}
