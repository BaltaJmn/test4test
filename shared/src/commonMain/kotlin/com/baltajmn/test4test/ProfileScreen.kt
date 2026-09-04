package com.baltajmn.test4test

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import test4test.shared.generated.resources.Res
import test4test.shared.generated.resources.profile_error
import test4test.shared.generated.resources.profile_fallback_name
import test4test.shared.generated.resources.profile_free
import test4test.shared.generated.resources.profile_my_apps
import test4test.shared.generated.resources.profile_none_yet
import test4test.shared.generated.resources.profile_premium
import test4test.shared.generated.resources.profile_privacy
import test4test.shared.generated.resources.profile_sign_out
import test4test.shared.generated.resources.profile_testing
import test4test.shared.generated.resources.testing_count

// La misma URL que va en la ficha de Play Console. El ancla lleva a como pedir
// la eliminacion de la cuenta, que Play exige tener accesible desde la app.
private const val PRIVACY_URL = "https://testers.baltajmn.dev/privacy"

// Una sola pantalla para el perfil propio y el de cualquier otro: las dos listas
// y el recuento son publicos y salen de las mismas consultas. Lo unico que
// cambia es la cola (plan, cerrar sesion, privacidad), que solo tiene sentido
// sobre uno mismo.
@Composable
fun ProfileScreen(
    uid: String,
    viewerId: String,
    me: Profile?,
    modifier: Modifier = Modifier,
    onOpen: (String) -> Unit,
    onPaywall: () -> Unit,
) {
    val isMe = uid == viewerId
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    var other by remember(uid) { mutableStateOf<Profile?>(null) }
    var mine by remember(uid) { mutableStateOf<List<AppRow>>(emptyList()) }
    var testing by remember(uid) { mutableStateOf<List<AppRow>>(emptyList()) }
    var error by remember(uid) { mutableStateOf<String?>(null) }
    var loaded by remember(uid) { mutableStateOf(false) }
    val loadErrorText = stringResource(Res.string.profile_error)

    LaunchedEffect(uid) {
        runCatching {
            mine = myApps(uid)
            testing = appsByIds(testerAppIds(uid).toList())
            // El propio ya viene de Home y ademas mas fresco: is_premium se
            // recarga ahi despues de comprar.
            if (!isMe) other = profile(uid)
        }.onFailure { error = it.message ?: loadErrorText }
        loaded = true
    }

    if (!loaded) {
        Loading()
        return
    }
    val person = if (isMe) me else other

    PageLazyColumn(modifier) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Avatar(person)
                Column {
                    Text(
                        person?.displayName?.takeIf { it.isNotBlank() }
                            ?: stringResource(Res.string.profile_fallback_name),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    // Sale de la lista que ya esta cargada: la reciprocidad es lo
                    // primero que se mira al llegar al perfil de alguien.
                    Text(
                        pluralStringResource(Res.plurals.testing_count, testing.size, testing.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (isMe) {
            item {
                if (me?.isPremium == true) {
                    Text(stringResource(Res.string.profile_premium), style = MaterialTheme.typography.labelLarge)
                } else {
                    OutlinedButton(
                        onClick = onPaywall,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(Res.string.profile_free))
                    }
                }
            }
        }
        item { ErrorText(error) }

        item { Text(stringResource(Res.string.profile_my_apps), style = MaterialTheme.typography.titleMedium) }
        if (mine.isEmpty()) {
            item { Text(stringResource(Res.string.profile_none_yet), style = MaterialTheme.typography.bodyMedium) }
        }
        for (app in mine) {
            item(key = "mine-${app.id}") { AppListItem(app, onClick = { onOpen(app.id) }) }
        }

        item { Text(stringResource(Res.string.profile_testing), style = MaterialTheme.typography.titleMedium) }
        if (testing.isEmpty()) {
            item { Text(stringResource(Res.string.profile_none_yet), style = MaterialTheme.typography.bodyMedium) }
        }
        for (app in testing) {
            item(key = "testing-${app.id}") { AppListItem(app, onClick = { onOpen(app.id) }) }
        }

        if (isMe) {
            item {
                Button(
                    onClick = { scope.launch { supabase.auth.signOut() } },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(Res.string.profile_sign_out))
                }
            }
            item {
                TextButton(
                    onClick = { uriHandler.openUri(PRIVACY_URL) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(Res.string.profile_privacy), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
