package com.baltajmn.test4test

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

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
        Text("Slots ilimitados", style = MaterialTheme.typography.headlineSmall)
        Text(
            "El plan gratuito incluye 1 app. Con slots ilimitados publicas todas las que quieras.",
            style = MaterialTheme.typography.bodyLarge,
        )
        when {
            me?.isPremium == true -> Text("Ya tienes slots ilimitados.")
            getPlatform().isAndroid -> PurchaseSection(uid, onPurchased)
            else -> Text("Gestiona tu suscripcion desde la app Android: la compra no esta disponible en la Web.")
        }
    }
}
