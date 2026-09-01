package com.baltajmn.test4test

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import test4test.shared.generated.resources.Res
import test4test.shared.generated.resources.paywall_active
import test4test.shared.generated.resources.paywall_body
import test4test.shared.generated.resources.paywall_web

// Issue #26: la compra solo existe en Android. En Web el estado premium se lee
// igual (mismo profiles.is_premium) pero no se vende nada.
// Fuera del MVP: via de pago Web con Stripe / RevenueCat Web Billing.
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
        when {
            me?.isPremium == true -> Text(stringResource(Res.string.paywall_active))
            getPlatform().isAndroid -> PurchaseSection(uid, onPurchased)
            else -> Text(stringResource(Res.string.paywall_web))
        }
    }
}
