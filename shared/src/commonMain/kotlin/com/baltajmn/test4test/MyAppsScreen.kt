package com.baltajmn.test4test

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
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
import org.jetbrains.compose.resources.stringResource
import test4test.shared.generated.resources.Res
import test4test.shared.generated.resources.action_delete
import test4test.shared.generated.resources.action_edit
import test4test.shared.generated.resources.confirm_delete_my_app
import test4test.shared.generated.resources.delete_error
import test4test.shared.generated.resources.my_apps_add
import test4test.shared.generated.resources.my_apps_empty
import test4test.shared.generated.resources.my_apps_error
import test4test.shared.generated.resources.my_apps_rank

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
    val loadErrorText = stringResource(Res.string.my_apps_error)
    val deleteErrorText = stringResource(Res.string.delete_error)

    LaunchedEffect(uid, reload) {
        runCatching { myApps(uid) }
            .onSuccess { apps = it; error = null }
            .onFailure { error = it.message ?: loadErrorText }
    }

    pendingDelete?.let { target ->
        ConfirmDialog(
            text = stringResource(Res.string.confirm_delete_my_app, target.name),
            onConfirm = {
                pendingDelete = null
                scope.launch {
                    runCatching { deleteApp(target.id) }
                        .onSuccess { reload++ }
                        .onFailure { error = it.message ?: deleteErrorText }
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
            LinkButton(stringResource(Res.string.my_apps_add)) {
                // El limite de slots tambien lo aplica la RLS; esto solo evita
                // mandar al usuario a un formulario que el backend rechazaria.
                if (canCreateApp(me?.isPremium == true, list.size)) onCreate() else onPaywall()
            }
        }
        item { ErrorText(error) }
        if (list.isEmpty()) {
            item {
                Text(
                    stringResource(Res.string.my_apps_empty),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        // La reciprocidad es del dueno, no de cada app, asi que todas las filas
        // traen el mismo dato y basta con mirar una. Se avisa aqui y no en el
        // feed: quedar abajo sin saber por que es lo que hace que la gente se
        // vaya pensando que la app no funciona.
        list.firstOrNull()?.takeIf { !it.ownerReciprocates }?.let { row ->
            item {
                Text(
                    stringResource(Res.string.my_apps_rank, row.ownerTestingCount, row.ownerTestingRequired),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        for (app in list) {
            item(key = app.id) {
                AppListItem(app, onClick = { onOpen(app.id) }) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = { onEdit(app) }) { Text(stringResource(Res.string.action_edit)) }
                        TextButton(onClick = { pendingDelete = app }) { Text(stringResource(Res.string.action_delete)) }
                    }
                }
            }
        }
    }
}
