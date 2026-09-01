package com.baltajmn.test4test

import androidx.compose.foundation.layout.Arrangement
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
import org.jetbrains.compose.resources.stringResource
import test4test.shared.generated.resources.Res
import test4test.shared.generated.resources.profile_error
import test4test.shared.generated.resources.profile_fallback_name
import test4test.shared.generated.resources.profile_free
import test4test.shared.generated.resources.profile_my_apps
import test4test.shared.generated.resources.profile_none_yet
import test4test.shared.generated.resources.profile_not_joined
import test4test.shared.generated.resources.profile_premium
import test4test.shared.generated.resources.profile_privacy
import test4test.shared.generated.resources.profile_sign_out
import test4test.shared.generated.resources.profile_testing

// La misma URL que va en la ficha de Play Console. El ancla lleva a como pedir
// la eliminacion de la cuenta, que Play exige tener accesible desde la app.
private const val PRIVACY_URL = "https://testers.baltajmn.dev/privacy"

@Composable
fun ProfileScreen(
    uid: String,
    me: Profile?,
    modifier: Modifier = Modifier,
    onOpen: (String) -> Unit,
    onPaywall: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    var mine by remember { mutableStateOf<List<AppRow>>(emptyList()) }
    var testing by remember { mutableStateOf<List<AppRow>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var loaded by remember { mutableStateOf(false) }
    val loadErrorText = stringResource(Res.string.profile_error)

    LaunchedEffect(uid) {
        runCatching {
            mine = myApps(uid)
            testing = appsByIds(testerAppIds(uid).toList())
        }.onFailure { error = it.message ?: loadErrorText }
        loaded = true
    }

    if (!loaded) {
        Loading()
        return
    }

    PageLazyColumn(modifier) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Avatar(me)
                Text(
                    me?.displayName?.takeIf { it.isNotBlank() } ?: stringResource(Res.string.profile_fallback_name),
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
        }
        item {
            if (me?.isPremium == true) {
                Text(stringResource(Res.string.profile_premium), style = MaterialTheme.typography.labelLarge)
            } else {
                OutlinedButton(onClick = onPaywall, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(Res.string.profile_free))
                }
            }
        }
        item { ErrorText(error) }

        item { Text(stringResource(Res.string.profile_my_apps), style = MaterialTheme.typography.titleMedium) }
        if (mine.isEmpty()) {
            item { Text(stringResource(Res.string.profile_none_yet), style = MaterialTheme.typography.bodyMedium) }
        }
        for (app in mine) {
            item(key = "mine-${app.id}") { AppCard(app, onClick = { onOpen(app.id) }) }
        }

        item { Text(stringResource(Res.string.profile_testing), style = MaterialTheme.typography.titleMedium) }
        if (testing.isEmpty()) {
            item { Text(stringResource(Res.string.profile_not_joined), style = MaterialTheme.typography.bodyMedium) }
        }
        for (app in testing) {
            item(key = "testing-${app.id}") { AppCard(app, onClick = { onOpen(app.id) }) }
        }

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
