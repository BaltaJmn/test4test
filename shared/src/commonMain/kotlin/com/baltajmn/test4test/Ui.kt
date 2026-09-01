package com.baltajmn.test4test

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import test4test.shared.generated.resources.Res
import test4test.shared.generated.resources.action_cancel
import test4test.shared.generated.resources.action_delete
import test4test.shared.generated.resources.confirm_title
import test4test.shared.generated.resources.follower_help
import test4test.shared.generated.resources.linked_app
import test4test.shared.generated.resources.testers

// Issue #28: en Web la ventana es mucho mas ancha que un movil. Una columna
// centrada con ancho maximo evita lineas de texto de 1500px sin escribir un
// layout distinto por plataforma.
private val CONTENT_MAX_WIDTH = 600.dp
private val PAGE_PADDING = 20.dp
private val ITEM_GAP = 14.dp

// Google Play pide 12 testers durante 14 dias seguidos antes de dejar publicar a
// una cuenta personal. El numero suelto no dice nada; contra las 12 marcas se lee
// cuanto falta, que es justo el orden en el que sale el feed.
private const val TESTERS_REQUIRED = 12

@Composable
fun PageColumn(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier.widthIn(max = CONTENT_MAX_WIDTH).fillMaxWidth().padding(PAGE_PADDING),
            verticalArrangement = Arrangement.spacedBy(ITEM_GAP),
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
            contentPadding = PaddingValues(PAGE_PADDING),
            verticalArrangement = Arrangement.spacedBy(ITEM_GAP),
            content = content,
        )
    }
}

@Composable
fun Loading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
    }
}

@Composable
fun ErrorText(message: String?, modifier: Modifier = Modifier) {
    if (message != null) {
        Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = modifier)
    }
}

// Decorativa: el recuento de al lado dice lo mismo en palabras, asi que
// anunciarla otra vez solo duplicaria la lectura del lector de pantalla.
@Composable
fun TesterTally(count: Long, modifier: Modifier = Modifier) {
    val filled = count.coerceIn(0, TESTERS_REQUIRED.toLong()).toInt()
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(TESTERS_REQUIRED) { index ->
            Box(
                Modifier
                    .width(4.dp)
                    .height(12.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (index < filled) MaterialTheme.colorScheme.primary
                        // Las marcas vacias van tenidas de violeta y no en gris: son
                        // los mismos huecos de la misma escala, no otro elemento.
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    )
            )
        }
    }
}

// "7/12" y no "7": el limite es la mitad de la informacion.
@Composable
fun TesterCount(count: Long, modifier: Modifier = Modifier) {
    val done = count >= TESTERS_REQUIRED
    Text(
        text = "$count/$TESTERS_REQUIRED",
        style = MaterialTheme.typography.labelLarge,
        fontFamily = monoFamily(),
        color = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

// Issue #19: el contador son las personas unidas como tester DENTRO de
// Test4Test, no descargas ni seguidores de Play Store.
@Composable
fun TesterSummary(count: Long, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TesterTally(count)
        Text(
            // pluralStringResource elige la forma segun el idioma: el indonesio no
            // tiene singular y el ingles y el espanol si.
            text = pluralStringResource(Res.plurals.testers, count.toInt(), count.toInt()),
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
fun FollowerHelp(modifier: Modifier = Modifier) {
    Text(
        stringResource(Res.string.follower_help),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            .size(56.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = avatarInitial(profile?.displayName),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
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

// Blanco sobre papel y un filete de 1dp en vez de sombra: en una lista larga la
// elevacion de Material se convierte en ruido.
@Composable
fun AppListItem(
    app: AppRow,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = {},
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    app.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                TesterCount(app.followerCount)
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TesterTally(app.followerCount)
                trailing()
            }
        }
    }
}

// Los tres enlaces del detalle son el trabajo de esa pantalla, no un adorno, asi
// que el borde va con el gris de contorno y no con el de los filetes, que a este
// tamano casi no se ve.
@Composable
fun LinkButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier.fillMaxWidth(),
    ) { Text(text) }
}

// Issue #22: app propia vinculada dentro de un comentario. Un tap lleva a su
// detalle, que es donde el otro usuario puede unirse como tester.
@Composable
fun LinkedAppChip(app: AppRow, onClick: () -> Unit, modifier: Modifier = Modifier) {
    AssistChip(
        onClick = onClick,
        label = { Text(stringResource(Res.string.linked_app, app.name, app.followerCount.toInt())) },
        colors = AssistChipDefaults.assistChipColors(labelColor = MaterialTheme.colorScheme.primary),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier,
    )
}

// Borrar es irreversible y en moderacion cae sobre contenido ajeno (issue #34),
// asi que siempre pasa por confirmacion. El boton que borra va en rojo: es el
// unico sitio de la app donde el color avisa en vez de decorar.
@Composable
fun ConfirmDialog(text: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        title = { Text(stringResource(Res.string.confirm_title), style = MaterialTheme.typography.titleLarge) },
        text = { Text(text, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) { Text(stringResource(Res.string.action_delete)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) } },
    )
}
