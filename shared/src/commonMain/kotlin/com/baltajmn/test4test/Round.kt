package com.baltajmn.test4test

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import test4test.shared.generated.resources.Res
import test4test.shared.generated.resources.reminder_off
import test4test.shared.generated.resources.reminder_on
import test4test.shared.generated.resources.round_done
import test4test.shared.generated.resources.round_help
import test4test.shared.generated.resources.round_install
import test4test.shared.generated.resources.round_open
import test4test.shared.generated.resources.round_opened
import test4test.shared.generated.resources.round_progress
import test4test.shared.generated.resources.round_title

// Ronda de hoy, arriba del feed. Google rechaza produccion con "los testers no
// usaron la app": instalarla no basta. Aqui se abre cada app que pruebas desde
// Test4Test y queda apuntado con el dia del servidor, que es lo que ve su dueno.
// Y da un motivo para volver cada dia, que es lo que el intercambio necesita.
@Composable
fun RoundCard(uid: String, testing: Set<String>, modifier: Modifier = Modifier) {
    if (testing.isEmpty()) return
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val launch = rememberAppLauncher()
    val installedFromPlay = rememberInstalledFromPlay()
    val reminder = rememberDailyReminder()
    var items by remember(testing) { mutableStateOf<List<RoundItem>?>(null) }
    LaunchedEffect(uid, testing) { items = runCatching { myRound(uid) }.getOrNull() }
    val list = items ?: return
    if (list.isEmpty()) return
    val opened = list.count { it.openedToday }
    LaunchedEffect(opened, list.size) { reminder.saveRound(list.size, opened) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        // Filete violeta y no gris: es lo unico del feed que pide algo hoy.
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
    ) {
        Column(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(stringResource(Res.string.round_title), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(Res.string.round_help),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                if (opened == list.size) stringResource(Res.string.round_done)
                else stringResource(Res.string.round_progress, opened, list.size),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            for (item in list) {
                // Alto fijo: con boton o con "Abierta hoy", las filas no bailan.
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 40.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        item.app.name,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (item.openedToday) {
                        Text(
                            stringResource(Res.string.round_opened),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        val pkg = item.app.packageName
                        val installed = pkg != null && installedFromPlay(pkg)
                        TextButton(onClick = {
                            if (installed && launch(pkg!!)) {
                                // La app ya esta delante; la apertura se apunta de fondo.
                                scope.launch {
                                    runCatching { recordOpen(item.app.id) }.onSuccess {
                                        items = list.map { if (it.app.id == item.app.id) it.copy(openedToday = true) else it }
                                    }
                                }
                            } else {
                                uriHandler.openUri(item.app.playStoreUrl)
                            }
                        }) {
                            Text(stringResource(if (installed) Res.string.round_open else Res.string.round_install))
                        }
                    }
                }
            }
            TextButton(onClick = reminder.toggle) {
                Text(
                    stringResource(if (reminder.on) Res.string.reminder_off else Res.string.reminder_on),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
