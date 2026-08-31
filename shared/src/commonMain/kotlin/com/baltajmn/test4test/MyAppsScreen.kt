package com.baltajmn.test4test

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun MyAppsScreen(
    uid: String,
    me: Profile?,
    modifier: Modifier = Modifier,
    onOpen: (String) -> Unit,
    onCreate: () -> Unit,
    onEdit: (AppRow) -> Unit,
    onPaywall: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var apps by remember { mutableStateOf<List<AppRow>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableStateOf(0) }
    var pendingDelete by remember { mutableStateOf<AppRow?>(null) }

    LaunchedEffect(uid, reload) {
        runCatching { myApps(uid) }
            .onSuccess { apps = it; error = null }
            .onFailure { error = it.message ?: "No se pudieron cargar tus apps" }
    }

    pendingDelete?.let { target ->
        ConfirmDialog(
            text = "Borrar \"${target.name}\"? Se iran tambien sus testers y comentarios.",
            onConfirm = {
                pendingDelete = null
                scope.launch {
                    runCatching { deleteApp(target.id) }
                        .onSuccess { reload++ }
                        .onFailure { error = it.message ?: "No se pudo borrar" }
                }
            },
            onDismiss = { pendingDelete = null },
        )
    }

    val list = apps
    if (list == null) {
        Loading()
        return
    }

    PageLazyColumn(modifier) {
        item {
            Button(
                // El limite de slots tambien lo aplica la RLS; esto solo evita
                // mandar al usuario a un formulario que el backend rechazaria.
                onClick = { if (canCreateApp(me?.isPremium == true, list.size)) onCreate() else onPaywall() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Anadir app")
            }
        }
        item { ErrorText(error) }
        if (list.isEmpty()) {
            item {
                Text(
                    "Aun no has dado de alta ninguna app.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        for (app in list) {
            item(key = app.id) {
                AppCard(app, onClick = { onOpen(app.id) }) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = { onEdit(app) }) { Text("Editar") }
                        TextButton(onClick = { pendingDelete = app }) { Text("Borrar") }
                    }
                }
            }
        }
    }
}
