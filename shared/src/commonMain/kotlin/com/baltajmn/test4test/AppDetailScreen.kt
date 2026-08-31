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
import androidx.compose.material3.OutlinedButton
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

    LaunchedEffect(appId, reload) {
        runCatching {
            app = appById(appId)
            comments = commentsFor(appId)
            isTester = appId in testerAppIds(uid)
            myOwnApps = myApps(uid)
        }.onFailure { error = it.message ?: "No se pudo cargar la app" }
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
            text = "Borrar \"${current.name}\" y todo su contenido?",
            onConfirm = {
                pendingAppDelete = false
                scope.launch {
                    runCatching { deleteApp(current.id) }
                        .onSuccess { onDeleted() }
                        .onFailure { error = it.message ?: "No se pudo borrar" }
                }
            },
            onDismiss = { pendingAppDelete = false },
        )
    }
    pendingCommentDelete?.let { target ->
        ConfirmDialog(
            text = "Borrar este comentario?",
            onConfirm = {
                pendingCommentDelete = null
                scope.launch {
                    runCatching { deleteComment(target.id) }
                        .onSuccess { reload++ }
                        .onFailure { error = it.message ?: "No se pudo borrar" }
                }
            },
            onDismiss = { pendingCommentDelete = null },
        )
    }

    PageLazyColumn(modifier) {
        item {
            Text(current.name, style = MaterialTheme.typography.headlineSmall)
            FollowerBadge(current.followerCount)
            Text(FOLLOWER_HELP, style = MaterialTheme.typography.bodySmall)
        }
        item { ErrorText(error) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { uriHandler.openUri(current.googleGroupsUrl) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Google Groups") }
                OutlinedButton(
                    onClick = { uriHandler.openUri(current.playStoreUrl) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Play Store") }
                OutlinedButton(
                    onClick = { uriHandler.openUri(current.optInUrl) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Enlace de opt-in") }
            }
        }
        item {
            when {
                isOwner -> Text("Esta app es tuya.", style = MaterialTheme.typography.bodyMedium)
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
                                .onFailure { error = it.message ?: "No se pudo completar" }
                            busy = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (isTester) "Salir" else "Unirme como tester")
                }
            }
        }
        if (isOwner || canModerate) {
            item {
                TextButton(onClick = { pendingAppDelete = true }) { Text("Borrar app") }
            }
        }

        item { HorizontalDivider() }
        item { Text("Comentarios", style = MaterialTheme.typography.titleMedium) }
        item {
            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                label = { Text("Escribe un comentario") },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        // Issue #21: sin apps propias no hay nada que vincular, asi que no se pinta.
        if (myOwnApps.isNotEmpty()) {
            item {
                Column {
                    Text("Vincular una app mia (opcional)", style = MaterialTheme.typography.bodySmall)
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
                        }.onFailure { error = it.message ?: "No se pudo enviar" }
                        busy = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Enviar") }
        }
        if (comments.isEmpty()) {
            item { Text("Todavia no hay comentarios.", style = MaterialTheme.typography.bodyMedium) }
        }
        for (view in comments) {
            item(key = view.comment.id) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(view.authorName, style = MaterialTheme.typography.labelLarge)
                        Text(view.comment.body, style = MaterialTheme.typography.bodyMedium)
                        view.linkedApp?.let { linked ->
                            LinkedAppChip(linked, onClick = { onOpen(linked.id) })
                        }
                        if (view.comment.authorId == uid || canModerate) {
                            TextButton(onClick = { pendingCommentDelete = view.comment }) { Text("Borrar") }
                        }
                    }
                }
            }
        }
    }
}
