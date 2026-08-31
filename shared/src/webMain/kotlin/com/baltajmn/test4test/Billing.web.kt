package com.baltajmn.test4test

import androidx.compose.runtime.Composable

// En Web no se vende nada en el MVP (issue #26). PaywallScreen decide por
// getPlatform().isAndroid y pinta el mensaje que manda a Android, asi que este
// actual no llega a componerse nunca; existe solo para cerrar el expect.
@Composable
actual fun PurchaseSection(uid: String, onPurchased: () -> Unit) {
}
