package com.baltajmn.test4test

import androidx.compose.runtime.Composable

// El paywall vive en commonMain como el resto de pantallas; lo unico que depende
// de la plataforma es esta seccion, porque el SDK de RevenueCat es de Android.
@Composable
expect fun PurchaseSection(uid: String, onPurchased: () -> Unit)
