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
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.pluralStringResource
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
import test4test.shared.generated.resources.confirm_leave
import test4test.shared.generated.resources.delete_error
import test4test.shared.generated.resources.detail_action_error
import test4test.shared.generated.resources.detail_copy_link
import test4test.shared.generated.resources.detail_delete_app
import test4test.shared.generated.resources.detail_error
import test4test.shared.generated.resources.detail_groups
import test4test.shared.generated.resources.detail_install
import test4test.shared.generated.resources.detail_installed
import test4test.shared.generated.resources.detail_join
import test4test.shared.generated.resources.detail_join_help
import test4test.shared.generated.resources.detail_leave
import test4test.shared.generated.resources.detail_link_copied
import test4test.shared.generated.resources.detail_not_found
import test4test.shared.generated.resources.detail_not_installed
import test4test.shared.generated.resources.detail_opt_in
import test4test.shared.generated.resources.detail_play
import test4test.shared.generated.resources.detail_testers_empty
import test4test.shared.generated.resources.detail_testers_title
import test4test.shared.generated.resources.detail_verified
import test4test.shared.generated.resources.tester_installed
import test4test.shared.generated.resources.tester_stale
import test4test.shared.generated.resources.tester_unverified
import test4test.shared.generated.resources.testing_count

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
    var testers by remember(appId) { mutableStateOf<List<TesterView>>(emptyList()) }
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
    var pendingLeave by remember { mutableStateOf(false) }
    var copied by remember(appId) { mutableStateOf(false) }
    // Ids ya denunciados en esta sesion, de la app y de los comentarios. Solo
    // apaga el boton: la fila la protege el UNIQUE app_reports_once.
    var reported by remember(appId) { mutableStateOf(emptySet<String>()) }
    // Cuantos pasos del alta como tester lleva abiertos: 1 el grupo, 2 el opt-in.
    var opened by remember(appId) { mutableStateOf(0) }
    // Se vuelve aqui desde Play despues de instalar: cada vuelta recalcula si la
    // app esta en el movil, sin esperar a ninguna consulta.
    val installedFromPlay = rememberInstalledFromPlay()
    var resumes by remember { mutableStateOf(0) }
    LifecycleResumeEffect(Unit) {
        resumes++
        onPauseOrDispose { }
    }
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
            // Solo en la propia: es la unica ficha donde la lista sirve para algo.
            if (row.ownerId == uid) testers = testersOf(appId)
        }.onFailure { error = if (it is IllegalStateException) notFoundText else loadErrorText }
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
    val installedHere = remember(resumes, current.packageName) {
        current.packageName?.let(installedFromPlay) == true
    }

    fun toggleTester() {
        busy = true
        scope.launch {
            runCatching {
                if (isTester) {
                    leaveApp(current.id, uid)
                } else {
                    joinApp(current.id, uid)
                    // Quien ya la tenia instalada no tiene que esperar a volver a
                    // abrir Test4Test para que el dueno lo vea.
                    if (installedHere) runCatching { confirmInstalls(uid, installedFromPlay) }
                }
            }
                .onSuccess { reload++ }
                .onFailure { error = actionErrorText }
            busy = false
        }
    }

    if (pendingAppDelete) {
        ConfirmDialog(
            text = stringResource(Res.string.confirm_delete_app_content, current.name),
            onConfirm = {
                pendingAppDelete = false
                scope.launch {
                    runCatching { deleteApp(current.id) }
                        .onSuccess { onDeleted() }
                        .onFailure { error = deleteErrorText }
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
                        .onFailure { error = deleteErrorText }
                }
            },
            onDismiss = { pendingCommentDelete = null },
        )
    }
    // Salir no es borrar, pero tumba la racha del otro si era uno de sus 12. Un
    // toque sin querer no deberia poder hacerle eso a nadie.
    if (pendingLeave) {
        ConfirmDialog(
            text = stringResource(Res.string.confirm_leave, current.name),
            confirmLabel = stringResource(Res.string.detail_leave),
            onConfirm = {
                pendingLeave = false
                toggleTester()
            },
            onDismiss = { pendingLeave = false },
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
            // Cuantos de esos tienen la app de verdad, que es lo que cuenta Play.
            if (current.followerCount > 0) {
                Text(
                    stringResource(Res.string.detail_verified, current.verifiedCount.toInt()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        // Con quien se esta haciendo el intercambio, y cuantas apps prueba esa
        // persona. Es el dato que decide si merece la pena entrar. En la propia
        // ficha sobra: seria uno mismo.
        if (!isOwner) {
            item {
                PersonRow(
                    profile = owner,
                    subtitle = pluralStringResource(
                        Res.plurals.testing_count, current.ownerTestingCount, current.ownerTestingCount,
                    ),
                    onClick = { onOpenProfile(current.ownerId) },
                )
            }
        }
        // Y en la propia, en su lugar, el recuento se abre en las personas que
        // hay detras. Play pide doce testers y catorce dias seguidos, pero lo
        // unico que se podia ver hasta ahora era el numero: sin nombres no se
        // sabe a quien devolverle el favor ni quien se fue cuando la racha cae.
        if (isOwner) {
            item {
                Text(
                    stringResource(Res.string.detail_testers_title),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            if (testers.isEmpty()) {
                item {
                    Text(
                        stringResource(Res.string.detail_testers_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            // Con el estado de instalacion al lado: el dueno ve quien tiene la app
            // de verdad y a quien escribir cuando la racha se cae.
            for (tester in testers) {
                item(key = "tester-" + tester.profile.id) {
                    PersonRow(
                        profile = tester.profile,
                        subtitle = stringResource(
                            when (tester.install) {
                                InstallState.INSTALLED -> Res.string.tester_installed
                                InstallState.STALE -> Res.string.tester_stale
                                InstallState.UNVERIFIED -> Res.string.tester_unverified
                            }
                        ),
                        onClick = { onOpenProfile(tester.profile.id) },
                    )
                }
            }
            item { HorizontalDivider() }
        }
        item { ErrorText(error) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Los pasos van numerados porque el orden importa: Play devuelve
                // una pagina de error a quien abre el opt-in sin estar antes en
                // el grupo, y esa es la queja mas repetida de todo el intercambio
                // de testers. Por eso el 2 no se enciende hasta abrir el 1.
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
                // El paso 3 declara, no comprueba: nadie puede saber desde aqui si
                // Play acepto el opt-in. Estuvo condicionado a haber abierto los
                // otros dos, pero ese estado vivia en memoria y el camino normal
                // (salir al navegador, que Android mate el proceso, volver) lo
                // borraba. Lo que si se comprueba viene despues: el paso 4.
                if (!isOwner && !isTester) {
                    Button(
                        // busy bloquea el doble clic mientras se resuelve la llamada.
                        enabled = !busy,
                        onClick = ::toggleTester,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("3. " + stringResource(Res.string.detail_join))
                    }
                }
                // El paso 4 es la prueba: instalada y desde Play. Un APK suelto o
                // una app sin opt-in no pasan. Mientras no este, el boton lleva a
                // instalarla; cuando esta, se dice, porque es lo que ve el dueno.
                if (isTester) {
                    Text(
                        stringResource(if (installedHere) Res.string.detail_installed else Res.string.detail_not_installed),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (installedHere) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (!installedHere) {
                        Button(
                            onClick = { uriHandler.openUri(current.playStoreUrl) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("4. " + stringResource(Res.string.detail_install))
                        }
                    }
                    TextButton(enabled = !busy, onClick = { pendingLeave = true }) {
                        Text(stringResource(Res.string.detail_leave))
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
                            }.onFailure { error = sendErrorText }
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
