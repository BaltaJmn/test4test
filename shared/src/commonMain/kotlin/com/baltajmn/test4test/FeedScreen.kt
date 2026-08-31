package com.baltajmn.test4test

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    uid: String,
    me: Profile?,
    modifier: Modifier = Modifier,
    onOpen: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var apps by remember { mutableStateOf<List<AppRow>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var refreshing by remember { mutableStateOf(false) }
    var reload by remember { mutableStateOf(0) }
    var pendingDelete by remember { mutableStateOf<AppRow?>(null) }

    // Sin Realtime en el MVP: los datos se refrescan al entrar y al deslizar.
    LaunchedEffect(uid, reload) {
        runCatching { feedApps(uid) }
            .onSuccess { apps = it; error = null }
            .onFailure { error = it.message ?: "No se pudo cargar el feed" }
        refreshing = false
    }

    pendingDelete?.let { target ->
        ConfirmDialog(
            text = "Borrar \"${target.name}\" y todos sus comentarios?",
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

    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = { refreshing = true; reload++ },
        modifier = modifier,
    ) {
        val list = apps
        if (list == null) {
            Loading()
        } else {
            PageLazyColumn {
                item { ErrorText(error) }
                if (list.isEmpty()) {
                    item {
                        Text(
                            "Todavia no hay apps de otros usuarios. Vuelve en un rato.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                for (app in list) {
                    item(key = app.id) {
                        AppCard(app, onClick = { onOpen(app.id) }) {
                            if (me?.isAdmin == true) {
                                TextButton(onClick = { pendingDelete = app }) { Text("Borrar") }
                            }
                        }
                    }
                }
                item { Text(FOLLOWER_HELP, style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}
