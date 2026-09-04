package com.baltajmn.test4test

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import test4test.shared.generated.resources.Res
import test4test.shared.generated.resources.action_delete
import test4test.shared.generated.resources.action_report
import test4test.shared.generated.resources.action_reported
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
import test4test.shared.generated.resources.detail_copy_link
import test4test.shared.generated.resources.detail_delete_app
import test4test.shared.generated.resources.detail_error
import test4test.shared.generated.resources.detail_groups
import test4test.shared.generated.resources.detail_join
import test4test.shared.generated.resources.detail_join_help
import test4test.shared.generated.resources.detail_leave
import test4test.shared.generated.resources.detail_link_copied
import test4test.shared.generated.resources.detail_not_found
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
    onOpenProfile: (String) -> Unit,
    onDeleted: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current

    var app by remember { mutableStateOf<AppRow?>(null) }
    var owner by remember(appId) { mutableStateOf<Profile?>(null) }
    var comments by remember { mutableStateOf<List<CommentView>>(emptyList()) }
    var myOwnApps by remember { mutableStateOf<List<AppRow>>(emptyList()) }
    var isTester by remember { mutableStateOf(false) }
    var body by remember { mutableStateOf("") }
    var linkedAppId by remember { mutableStateOf<String?>(null) }
    // El compositor arranca plegado: abierto ocupa campo, chips y boton, y en un
    // movil eso deja todos los comentarios fuera de pantalla.
    var composing by remember(appId) { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var reload by remember { mutableStateOf(0) }
    var pendingCommentDelete by remember { mutableStateOf<CommentRow?>(null) }
    var pendingAppDelete by remember { mutableStateOf(false) }
    var copied by remember(appId) { mutableStateOf(false) }
    // Ids ya denunciados en esta sesion, de la app y de los comentarios. Solo
    // apaga el boton: la fila la protege el UNIQUE app_reports_once.
    var reported by remember(appId) { mutableStateOf(emptySet<String>()) }
    // Cuantos pasos del alta como tester lleva abiertos: 1 el grupo, 2 el opt-in.
    var opened by remember(appId) { mutableStateOf(0) }
    // Resueltos aqui: LaunchedEffect y los lambdas de onClick no son composables.
    val loadErrorText = stringResource(Res.string.detail_error)
    val notFoundText = stringResource(Res.string.detail_not_found)
    val deleteErrorText = stringResource(Res.string.delete_error)
    val actionErrorText = stringResource(Res.string.detail_action_error)
    val sendErrorText = stringResource(Res.string.comment_send_error)

    LaunchedEffect(appId, reload) {
        runCatching {
            // Un id que no existe no es un fallo de red: pasa al abrir un enlace
            // compartido de una app ya borrada. Sin esto la pantalla se queda
            // cargando para siempre.
            val row = appById(appId) ?: throw IllegalStateException(notFoundText)
            app = row
            owner = profile(row.ownerId)
            comments = commentsFor(appId)
            isTester = appId in testerAppIds(uid)
            myOwnApps = myApps(uid)
        }.onFailure { error = it.message ?: loadErrorText }
    }

    val current = app
    if (current == null) {
        if (error == null) Loading() else PageColumn(modifier) { ErrorText(error) }
        return
    }
    val isOwner = current.ownerId == uid
    val canModerate = me?.isAdmin == true
    // Quien ya paso por Play no tiene que volver a recorrer los pasos en orden.
    val stepsDone = isOwner || isTester

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

    // Una denuncia no tiene vuelta atras util ni nada que ensenar despues, asi
    // que no pasa por dialogo: el boton se queda apagado y ya esta.
    fun sendReport(key: String, input: ReportInput) {
        reported = reported + key
        scope.launch { runCatching { report(input) } }
    }

    PageLazyColumn(modifier) {
        item {
            Text(current.name, style = MaterialTheme.typography.headlineSmall)
            TesterMeter(current.followerCount, current.fullDays)
            // Pegado al contador y no mas abajo: explica ese numero, y separado
            // de el se leia como un parrafo suelto mas.
            FollowerHelp()
        }
        // Con quien se esta haciendo el intercambio, y cuantas apps prueba esa
        // persona. Es el dato que decide si merece la pena entrar.
        item {
            PersonRow(
                profile = owner,
                testingCount = current.ownerTestingCount,
                onClick = { onOpenProfile(current.ownerId) },
            )
        }
        item { ErrorText(error) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Los pasos van numerados porque el orden importa: Play devuelve
                // una pagina de error a quien abre el opt-in sin estar antes en
                // el grupo, y esa es la queja mas repetida de todo el intercambio
                // de testers. Por eso cada boton no se enciende hasta el anterior.
                if (!stepsDone) {
                    Text(
                        stringResource(Res.string.detail_join_help),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                LinkButton("1. " + stringResource(Res.string.detail_groups)) {
                    opened = maxOf(opened, 1)
                    uriHandler.openUri(current.googleGroupsUrl)
                }
                LinkButton(
                    text = "2. " + stringResource(Res.string.detail_opt_in),
                    enabled = stepsDone || opened >= 1,
                ) {
                    opened = 2
                    uriHandler.openUri(current.optInUrl)
                }
                when {
                    isOwner -> Text(
                        stringResource(Res.string.detail_own_app),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    // Marcarse sin haber pasado por Play deja el contador diciendo
                    // una cosa y Play Console otra, asi que el paso 3 depende de
                    // los dos anteriores igual que ellos entre si.
                    else -> Button(
                        // busy bloquea el doble clic mientras se resuelve la llamada.
                        enabled = !busy && (isTester || opened >= 2),
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
                        Text(
                            if (isTester) stringResource(Res.string.detail_leave)
                            else "3. " + stringResource(Res.string.detail_join)
                        )
                    }
                }
            }
        }
        item {
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // Play Store baja de boton a ancho completo a esta fila: en una
                // prueba cerrada la ficha publica ni siquiera abre para quien
                // todavia no es tester, asi que es contexto, no el trabajo.
                TextButton(onClick = { uriHandler.openUri(current.playStoreUrl) }) {
                    Text(stringResource(Res.string.detail_play))
                }
                // La app se anuncia fuera, asi que cada ficha necesita una URL
                // suya que abra aqui directamente.
                TextButton(onClick = {
                    clipboard.setText(AnnotatedString(appShareUrl(current.id)))
                    copied = true
                }) {
                    Text(stringResource(if (copied) Res.string.detail_link_copied else Res.string.detail_copy_link))
                }
                if (!isOwner) {
                    TextButton(
                        enabled = current.id !in reported,
                        onClick = { sendReport(current.id, ReportInput(reporterId = uid, appId = current.id)) },
                    ) {
                        Text(stringResource(if (current.id in reported) Res.string.action_reported else Res.string.action_report))
                    }
                }
                if (isOwner || canModerate) {
                    TextButton(onClick = { pendingAppDelete = true }) { Text(stringResource(Res.string.detail_delete_app)) }
                }
            }
        }

        item { HorizontalDivider() }
        item { Text(stringResource(Res.string.comments_title), style = MaterialTheme.typography.titleMedium) }
        if (!composing) {
            item { LinkButton(stringResource(Res.string.comment_placeholder)) { composing = true } }
        } else {
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
                                composing = false
                                reload++
                            }.onFailure { error = it.message ?: sendErrorText }
                            busy = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(Res.string.comment_send)) }
            }
        }
        if (comments.isEmpty()) {
            item { Text(stringResource(Res.string.comments_empty), style = MaterialTheme.typography.bodyMedium) }
        }
        for (view in comments) {
            item(key = view.comment.id) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        PersonRow(
                            profile = view.author,
                            testingCount = null,
                            onClick = { onOpenProfile(view.comment.authorId) },
                        )
                        Text(view.comment.body, style = MaterialTheme.typography.bodyMedium)
                        view.linkedApp?.let { linked ->
                            LinkedAppChip(linked, onClick = { onOpen(linked.id) })
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (view.comment.authorId == uid || canModerate) {
                                TextButton(onClick = { pendingCommentDelete = view.comment }) { Text(stringResource(Res.string.action_delete)) }
                            }
                            // Denunciar el propio comentario no tiene sentido: si
                            // molesta, el boton de al lado lo borra.
                            if (view.comment.authorId != uid) {
                                val id = view.comment.id
                                TextButton(
                                    enabled = id !in reported,
                                    onClick = { sendReport(id, ReportInput(reporterId = uid, commentId = id)) },
                                ) {
                                    Text(stringResource(if (id in reported) Res.string.action_reported else Res.string.action_report))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
