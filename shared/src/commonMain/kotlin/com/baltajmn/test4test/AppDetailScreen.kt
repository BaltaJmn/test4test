package com.baltajmn.test4test

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import test4test.shared.generated.resources.Res
import test4test.shared.generated.resources.action_delete
import test4test.shared.generated.resources.comment_author_fallback
import test4test.shared.generated.resources.comment_link_app
import test4test.shared.generated.resources.comment_placeholder
import test4test.shared.generated.resources.comment_send
import test4test.shared.generated.resources.comment_send_error
import test4test.shared.generated.resources.comments_empty
import test4test.shared.generated.resources.comments_title
import test4test.shared.generated.resources.confirm_delete_app_content
import test4test.shared.generated.resources.confirm_delete_comment
import test4test.shared.generated.resources.delete_error
import test4test.shared.generated.resources.detail_action_error
import test4test.shared.generated.resources.detail_delete_app
import test4test.shared.generated.resources.detail_error
import test4test.shared.generated.resources.detail_groups
import test4test.shared.generated.resources.detail_join
import test4test.shared.generated.resources.detail_leave
import test4test.shared.generated.resources.detail_opt_in
import test4test.shared.generated.resources.detail_own_app
import test4test.shared.generated.resources.detail_play

@Composable
fun AppDetailScreen(
    appId: String,
    uid: String,
    me: Profile?,
    modifier: Modifier = Modifier,
    onOpen: (String) -> Unit,
    onDeleted: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current

    var app by remember { mutableStateOf<AppRow?>(null) }
    var comments by remember { mutableStateOf<List<CommentView>>(emptyList()) }
    var myOwnApps by remember { mutableStateOf<List<AppRow>>(emptyList()) }
    var isTester by remember { mutableStateOf(false) }
    var body by remember { mutableStateOf("") }
    var linkedAppId by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var reload by remember { mutableStateOf(0) }
    var pendingCommentDelete by remember { mutableStateOf<CommentRow?>(null) }
    var pendingAppDelete by remember { mutableStateOf(false) }
    // Resueltos aqui: LaunchedEffect y los lambdas de onClick no son composables.
    val loadErrorText = stringResource(Res.string.detail_error)
    val deleteErrorText = stringResource(Res.string.delete_error)
    val actionErrorText = stringResource(Res.string.detail_action_error)
    val sendErrorText = stringResource(Res.string.comment_send_error)

    LaunchedEffect(appId, reload) {
        runCatching {
            app = appById(appId)
            comments = commentsFor(appId)
            isTester = appId in testerAppIds(uid)
            myOwnApps = myApps(uid)
        }.onFailure { error = it.message ?: loadErrorText }
    }

    val current = app
    if (current == null) {
        Loading()
        return
    }
    val isOwner = current.ownerId == uid
    val canModerate = me?.isAdmin == true

    if (pendingAppDelete) {
        ConfirmDialog(
            text = stringResource(Res.string.confirm_delete_app_content, current.name),
            onConfirm = {
                pendingAppDelete = false
                scope.launch {
                    runCatching { deleteApp(current.id) }
                        .onSuccess { onDeleted() }
                        .onFailure { error = it.message ?: deleteErrorText }
                }
            },
            onDismiss = { pendingAppDelete = false },
        )
    }
    pendingCommentDelete?.let { target ->
        ConfirmDialog(
            text = stringResource(Res.string.confirm_delete_comment),
            onConfirm = {
                pendingCommentDelete = null
                scope.launch {
                    runCatching { deleteComment(target.id) }
                        .onSuccess { reload++ }
                        .onFailure { error = it.message ?: deleteErrorText }
                }
            },
            onDismiss = { pendingCommentDelete = null },
        )
    }

    PageLazyColumn(modifier) {
        item {
            Text(current.name, style = MaterialTheme.typography.headlineSmall)
            TesterSummary(current.followerCount)
            FollowerHelp()
        }
        item { ErrorText(error) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LinkButton(stringResource(Res.string.detail_groups)) { uriHandler.openUri(current.googleGroupsUrl) }
                LinkButton(stringResource(Res.string.detail_play)) { uriHandler.openUri(current.playStoreUrl) }
                LinkButton(stringResource(Res.string.detail_opt_in)) { uriHandler.openUri(current.optInUrl) }
            }
        }
        item {
            when {
                isOwner -> Text(stringResource(Res.string.detail_own_app), style = MaterialTheme.typography.bodyMedium)
                else -> Button(
                    // busy bloquea el doble clic mientras se resuelve la llamada.
                    enabled = !busy,
                    onClick = {
                        busy = true
                        scope.launch {
                            runCatching {
                                if (isTester) leaveApp(current.id, uid) else joinApp(current.id, uid)
                            }
                                .onSuccess { reload++ }
                                .onFailure { error = it.message ?: actionErrorText }
                            busy = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(if (isTester) Res.string.detail_leave else Res.string.detail_join))
                }
            }
        }
        if (isOwner || canModerate) {
            item {
                TextButton(onClick = { pendingAppDelete = true }) { Text(stringResource(Res.string.detail_delete_app)) }
            }
        }

        item { HorizontalDivider() }
        item { Text(stringResource(Res.string.comments_title), style = MaterialTheme.typography.titleMedium) }
        item {
            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                label = { Text(stringResource(Res.string.comment_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        // Issue #21: sin apps propias no hay nada que vincular, asi que no se pinta.
        if (myOwnApps.isNotEmpty()) {
            item {
                Column {
                    Text(stringResource(Res.string.comment_link_app), style = MaterialTheme.typography.bodySmall)
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        for (own in myOwnApps) {
                            FilterChip(
                                selected = linkedAppId == own.id,
                                onClick = { linkedAppId = if (linkedAppId == own.id) null else own.id },
                                label = { Text(own.name) },
                            )
                        }
                    }
                }
            }
        }
        item {
            Button(
                enabled = !busy && body.isNotBlank(),
                onClick = {
                    busy = true
                    scope.launch {
                        runCatching {
                            postComment(
                                CommentInput(
                                    appId = current.id,
                                    authorId = uid,
                                    body = body.trim(),
                                    linkedAppId = linkedAppId,
                                )
                            )
                        }.onSuccess {
                            body = ""
                            linkedAppId = null
                            reload++
                        }.onFailure { error = it.message ?: sendErrorText }
                        busy = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(Res.string.comment_send)) }
        }
        if (comments.isEmpty()) {
            item { Text(stringResource(Res.string.comments_empty), style = MaterialTheme.typography.bodyMedium) }
        }
        for (view in comments) {
            item(key = view.comment.id) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            view.authorName.ifBlank { stringResource(Res.string.comment_author_fallback) },
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Text(view.comment.body, style = MaterialTheme.typography.bodyMedium)
                        view.linkedApp?.let { linked ->
                            LinkedAppChip(linked, onClick = { onOpen(linked.id) })
                        }
                        if (view.comment.authorId == uid || canModerate) {
                            TextButton(onClick = { pendingCommentDelete = view.comment }) { Text(stringResource(Res.string.action_delete)) }
                        }
                    }
                }
            }
        }
    }
}
