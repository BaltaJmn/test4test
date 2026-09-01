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
import org.jetbrains.compose.resources.stringResource
import test4test.shared.generated.resources.Res
import test4test.shared.generated.resources.action_delete
import test4test.shared.generated.resources.confirm_delete_app_comments
import test4test.shared.generated.resources.delete_error
import test4test.shared.generated.resources.feed_empty
import test4test.shared.generated.resources.feed_error

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
    // Se resuelven aqui porque LaunchedEffect y los lambdas no son composables.
    val loadErrorText = stringResource(Res.string.feed_error)
    val deleteErrorText = stringResource(Res.string.delete_error)

    // Sin Realtime en el MVP: los datos se refrescan al entrar y al deslizar.
    LaunchedEffect(uid, reload) {
        runCatching { feedApps(uid) }
            .onSuccess { apps = it; error = null }
            .onFailure { error = it.message ?: loadErrorText }
        refreshing = false
    }

    pendingDelete?.let { target ->
        ConfirmDialog(
            text = stringResource(Res.string.confirm_delete_app_comments, target.name),
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
                            stringResource(Res.string.feed_empty),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                for (app in list) {
                    item(key = app.id) {
                        AppCard(app, onClick = { onOpen(app.id) }) {
                            if (me?.isAdmin == true) {
                                TextButton(onClick = { pendingDelete = app }) { Text(stringResource(Res.string.action_delete)) }
                            }
                        }
                    }
                }
                item { FollowerHelp() }
            }
        }
    }
}
