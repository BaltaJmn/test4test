package com.baltajmn.test4test

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import test4test.shared.generated.resources.Res
import test4test.shared.generated.resources.action_cancel
import test4test.shared.generated.resources.action_delete
import test4test.shared.generated.resources.confirm_title
import test4test.shared.generated.resources.follower_help
import test4test.shared.generated.resources.ic_tester
import test4test.shared.generated.resources.linked_app
import test4test.shared.generated.resources.testers

// Issue #28: en Web la ventana es mucho mas ancha que un movil. Una columna
// centrada con ancho maximo evita lineas de texto de 1500px sin escribir un
// layout distinto por plataforma.
private val CONTENT_MAX_WIDTH = 600.dp

@Composable
fun PageColumn(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier.widthIn(max = CONTENT_MAX_WIDTH).fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@Composable
fun PageLazyColumn(
    modifier: Modifier = Modifier,
    content: LazyListScope.() -> Unit,
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.widthIn(max = CONTENT_MAX_WIDTH).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@Composable
fun Loading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

@Composable
fun ErrorText(message: String?, modifier: Modifier = Modifier) {
    if (message != null) {
        Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = modifier)
    }
}

// Issue #19: el contador son las personas unidas como tester DENTRO de
// Test4Test, no descargas ni seguidores de Play Store.
@Composable
fun FollowerBadge(count: Long, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // Decorativo: el texto de al lado ya dice lo mismo, asi que anunciarlo
        // otra vez solo duplicaria la lectura del lector de pantalla.
        Icon(
            painter = painterResource(Res.drawable.ic_tester),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
        Text(
            // pluralStringResource elige la forma segun el idioma: el indonesio no
            // tiene singular y el ingles y el espanol si.
            text = pluralStringResource(Res.plurals.testers, count.toInt(), count.toInt()),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
fun FollowerHelp(modifier: Modifier = Modifier) {
    Text(
        stringResource(Res.string.follower_help),
        style = MaterialTheme.typography.bodySmall,
        modifier = modifier,
    )
}

// Issue #27: la foto sale de profiles.avatar_url, que rellena Google al registrarse.
// La inicial se pinta siempre por debajo, asi que hay algo visible mientras carga
// y tambien cuando el perfil no trae foto o la descarga falla: no hace falta
// manejar estados de error de imagen.
@Composable
fun Avatar(profile: Profile?, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = avatarInitial(profile?.displayName),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
        val url = profile?.avatarUrl
        if (!url.isNullOrBlank()) {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        }
    }
}

@Composable
fun AppCard(
    app: AppRow,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = {},
) {
    Card(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(app.name, style = MaterialTheme.typography.titleMedium)
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FollowerBadge(app.followerCount)
                trailing()
            }
        }
    }
}

// Issue #22: app propia vinculada dentro de un comentario. Un tap lleva a su
// detalle, que es donde el otro usuario puede unirse como tester.
@Composable
fun LinkedAppChip(app: AppRow, onClick: () -> Unit, modifier: Modifier = Modifier) {
    AssistChip(
        onClick = onClick,
        label = { Text(stringResource(Res.string.linked_app, app.name, app.followerCount.toInt())) },
        modifier = modifier,
    )
}

// Borrar es irreversible y en moderacion cae sobre contenido ajeno (issue #34),
// asi que siempre pasa por confirmacion.
@Composable
fun ConfirmDialog(text: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.confirm_title)) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(Res.string.action_delete)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) } },
    )
}
