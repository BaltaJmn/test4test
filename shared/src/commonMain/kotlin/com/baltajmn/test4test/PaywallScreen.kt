package com.baltajmn.test4test

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import test4test.shared.generated.resources.Res
import test4test.shared.generated.resources.paywall_active
import test4test.shared.generated.resources.paywall_body
import test4test.shared.generated.resources.paywall_terms

// Plan gratuito: una app a la vez. Mas apps, con la suscripcion. El estado se lee
// de profiles.is_premium, que solo escribe el webhook de RevenueCat.
@Composable
fun PaywallScreen(
    uid: String,
    me: Profile?,
    modifier: Modifier = Modifier,
    onPurchased: () -> Unit,
) {
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
        }
    }
}
