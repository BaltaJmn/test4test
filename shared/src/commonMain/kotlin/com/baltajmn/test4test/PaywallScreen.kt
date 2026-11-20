package com.baltajmn.test4test

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import test4test.shared.generated.resources.Res
import test4test.shared.generated.resources.paywall_active
import test4test.shared.generated.resources.paywall_body
import test4test.shared.generated.resources.paywall_terms
import test4test.shared.generated.resources.paywall_terms_link
import test4test.shared.generated.resources.profile_privacy

// Plan gratuito: una app a la vez. Mas apps, con la suscripcion. El estado se lee
// de profiles.is_premium, que solo escribe el webhook de RevenueCat.
@Composable
fun PaywallScreen(
    uid: String,
    me: Profile?,
    modifier: Modifier = Modifier,
    onPurchased: () -> Unit,
) {
    SideEffect { Session.busy = true }
    PageColumn(modifier) {
        Text(
            stringResource(Res.string.paywall_body),
            style = MaterialTheme.typography.bodyLarge,
        )
        if (me?.isPremium == true) {
            Text(stringResource(Res.string.paywall_active))
        } else {
            PurchaseSection(uid, onPurchased)
            // Play pide que la renovacion y como cancelar se lean antes de pagar,
            // no en la hoja de compra.
            Text(
                stringResource(Res.string.paywall_terms),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // Terminos y privacidad junto a la compra, como pide Play de una suscripcion.
            val uriHandler = LocalUriHandler.current
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { uriHandler.openUri(TERMS_URL) }, modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(Res.string.paywall_terms_link),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                    )
                }
                TextButton(onClick = { uriHandler.openUri(PRIVACY_URL) }, modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(Res.string.profile_privacy),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
