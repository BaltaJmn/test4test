package com.baltajmn.test4test

import androidx.compose.runtime.Composable

// La compra solo existe en Android (issue #26), pero el paywall vive en commonMain
// como el resto de pantallas, asi que lo unico que cambia por plataforma es esta
// seccion. El SDK de RevenueCat es Android-only y no puede subir a commonMain.
@Composable
expect fun PurchaseSection(uid: String, onPurchased: () -> Unit)
