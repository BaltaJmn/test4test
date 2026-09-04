package com.baltajmn.test4test

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.unit.Dp
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
import test4test.shared.generated.resources.profile_fallback_name
import test4test.shared.generated.resources.testers_days
import test4test.shared.generated.resources.testers_days_done
import test4test.shared.generated.resources.testers_of
import test4test.shared.generated.resources.testing_count

// Issue #28: en Web la ventana es mucho mas ancha que un movil. Una columna
// centrada con ancho maximo evita lineas de texto de 1500px sin escribir un
// layout distinto por plataforma.
private val CONTENT_MAX_WIDTH = 600.dp
private val PAGE_PADDING = 20.dp
private val ITEM_GAP = 14.dp

// Las 12 marcas del medidor: 4dp de ancho y 3dp de hueco entre ellas.
private val TICK_WIDTH = 4.dp
private val TICK_HEIGHT = 12.dp
private val TICK_GAP = 3.dp
private val METER_WIDTH = TICK_WIDTH * TESTERS_REQUIRED + TICK_GAP * (TESTERS_REQUIRED - 1)

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

// Play pide 12 testers y despues 14 dias seguidos con ellos. Las dos cosas caben
// en un instrumento porque son consecutivas: primero se llenan las 12 marcas, y
// solo cuando estan puestas empieza a correr la barra de los 14 dias. Antes esto
// se contaba tres veces en la misma tarjeta (las marcas, "7/12" y "Dia 3 de 14"),
// que es la misma informacion ocupando el triple de alto.
//
// Las marcas no llevan descripcion: el pie de linea dice lo mismo en palabras, y
// anunciarlo dos veces solo alarga la lectura del lector de pantalla.
@Composable
fun TesterMeter(count: Long, fullDays: Int, modifier: Modifier = Modifier) {
    val phase = testerPhase(fullDays)
    val accent = MaterialTheme.colorScheme.primary
    // En cuanto arranca la racha las 12 estan puestas por definicion, asi que el
    // recuento solo manda mientras se llena.
    val filled = when (phase) {
        TesterPhase.FILLING -> count.coerceIn(0, TESTERS_REQUIRED.toLong()).toInt()
        else -> TESTERS_REQUIRED
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(TICK_GAP)) {
            repeat(TESTERS_REQUIRED) { index ->
                Box(
                    Modifier
                        .width(TICK_WIDTH)
                        .height(TICK_HEIGHT)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            // Las marcas vacias van tenidas de violeta y no en gris:
                            // son los huecos de la misma escala, no otro elemento.
                            if (index < filled) accent else accent.copy(alpha = 0.15f)
                        )
                )
            }
        }
        // La barra solo existe mientras corre la racha. En fase de llenado no hay
        // nada que medir, y una barra a cero diria lo que ya dicen las marcas
        // vacias de arriba.
        if (phase == TesterPhase.HOLDING) {
            Box(
                Modifier
                    .width(METER_WIDTH)
                    .height(2.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(accent.copy(alpha = 0.15f))
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fullDays.toFloat() / FULL_DAYS_REQUIRED)
                        .background(accent)
                )
            }
        }
        Text(
            text = when (phase) {
                TesterPhase.FILLING -> stringResource(Res.string.testers_of, count.toInt(), TESTERS_REQUIRED)
                TesterPhase.HOLDING -> stringResource(Res.string.testers_days, fullDays, FULL_DAYS_REQUIRED)
                TesterPhase.DONE -> stringResource(Res.string.testers_days_done, FULL_DAYS_REQUIRED)
            },
            style = MaterialTheme.typography.bodySmall,
            // El violeta se reserva para cuando la racha ya cuenta: en fase de
            // llenado el dato todavia no es una buena noticia.
            color = if (phase == TesterPhase.FILLING) MaterialTheme.colorScheme.onSurfaceVariant else accent,
        )
    }
}

// Issue #19: el contador son las personas unidas como tester DENTRO de
// Test4Test, no descargas ni seguidores de Play Store.
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
fun Avatar(profile: Profile?, modifier: Modifier = Modifier, size: Dp = 56.dp) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = avatarInitial(profile?.displayName),
            style = if (size < 40.dp) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleLarge,
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

// Quien publica una app importa tanto como la app: es la persona a la que vas a
// pedirle que pruebe la tuya. Debajo del nombre va cuantas prueba ella, que es
// el unico dato con el que se decide si el intercambio va a ser reciproco.
//
// testingCount a null en las fichas de comentario: ahi el recuento costaria una
// consulta por autor y no es lo que se esta mirando.
@Composable
fun PersonRow(
    profile: Profile?,
    testingCount: Int?,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(profile, size = 32.dp)
        Column {
            Text(
                text = profile?.displayName?.takeIf { it.isNotBlank() }
                    ?: stringResource(Res.string.profile_fallback_name),
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (testingCount != null) {
                Text(
                    text = pluralStringResource(Res.plurals.testing_count, testingCount, testingCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// Blanco sobre papel y un filete de 1dp en vez de sombra: en una lista larga la
// elevacion de Material se convierte en ruido. Dos filas y no tres: el medidor ya
// trae el recuento y los dias en su propio pie.
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    app.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                trailing()
            }
            TesterMeter(app.followerCount, app.fullDays)
        }
    }
}

// Los enlaces del detalle son el trabajo de esa pantalla, no un adorno, asi que
// el borde va con el gris de contorno y no con el de los filetes, que a este
// tamano casi no se ve.
@Composable
fun LinkButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
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
