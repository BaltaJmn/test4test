package com.baltajmn.test4test

import android.content.Context
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.PurchasesTransactionException
import com.revenuecat.purchases.awaitLogIn
import com.revenuecat.purchases.awaitOfferings
import com.revenuecat.purchases.awaitPurchase
import com.revenuecat.purchases.awaitRestore
import kotlinx.coroutines.launch

// Issues #23 y #24: precio real desde el offering de RevenueCat, compra y
// restaurar. Quien decide si el usuario es premium sigue siendo la fila de
// profiles, no esta pantalla: aqui solo se dispara la compra y se espera.
@Composable
actual fun PurchaseSection(uid: String, onPurchased: () -> Unit) {
    // Sin API key la compra no existe todavia. Asi el repo, la CI y cualquiera que
    // clone compilan sin tener dada de alta la cuenta de RevenueCat.
    if (RevenueCatConfig.ANDROID_KEY.isBlank()) {
        Text("Compra en preparacion.", style = MaterialTheme.typography.bodySmall)
        return
    }

    val context = LocalContext.current
    val activity = LocalActivity.current
    val scope = rememberCoroutineScope()
    var offer by remember { mutableStateOf<Package?>(null) }
    var busy by remember { mutableStateOf(true) }
    var message by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uid) {
        message = runCatching {
            signIn(context, uid)
            // Primer paquete del offering actual: slots ilimitados es el unico
            // producto que se vende, no hay nada que elegir.
            offer = Purchases.sharedInstance.awaitOfferings()
                .current?.availablePackages?.firstOrNull()
        }.fold(
            onSuccess = { if (offer == null) "No hay ningun producto disponible todavia." else null },
            onFailure = { "No se pudo cargar el precio: ${it.message}" },
        )
        busy = false
    }

    Button(
        onClick = {
            val pack = offer ?: return@Button
            val host = activity ?: return@Button
            busy = true
            message = null
            scope.launch {
                runCatching {
                    Purchases.sharedInstance.awaitPurchase(PurchaseParams.Builder(host, pack).build())
                }
                    // is_premium lo escribe el webhook contra Supabase, no el cliente
                    // (issue #25), asi que hay que esperar a que llegue: la RLS solo
                    // deja crear la segunda app cuando la fila ya dice true.
                    .onSuccess { message = activate(uid, onPurchased) }
                    .onFailure { message = purchaseError(it) }
                busy = false
            }
        },
        enabled = !busy && offer != null && activity != null,
        modifier = Modifier.fillMaxWidth(),
    ) {
        val price = offer?.product?.price?.formatted
        Text(if (price == null) "Suscribirme" else "Suscribirme por $price")
    }

    // Reinstalar en otro movil no genera evento nuevo en RevenueCat, asi que sin
    // esto un usuario que ya pago se quedaria sin via de recuperar el acceso.
    TextButton(
        onClick = {
            busy = true
            message = null
            scope.launch {
                runCatching { Purchases.sharedInstance.awaitRestore() }
                    .onSuccess { info ->
                        message =
                            if (info.entitlements.active.isEmpty()) "No hemos encontrado ninguna compra activa."
                            else activate(uid, onPurchased)
                    }
                    .onFailure { message = purchaseError(it) }
                busy = false
            }
        },
        enabled = !busy,
    ) {
        Text("Restaurar compras")
    }

    if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
    ErrorText(message)
}

// app_user_id = profiles.id: es la clave con la que el webhook sabe a quien
// actualizar. Si no coinciden, la compra se cobra y nadie se entera (issue #25).
private suspend fun signIn(context: Context, uid: String) {
    if (!Purchases.isConfigured) {
        Purchases.configure(
            PurchasesConfiguration.Builder(context, RevenueCatConfig.ANDROID_KEY)
                .appUserID(uid)
                .build()
        )
    } else if (Purchases.sharedInstance.appUserID != uid) {
        Purchases.sharedInstance.awaitLogIn(uid)
    }
}

// Devuelve el mensaje a pintar, o null si el premium ya esta activo.
private suspend fun activate(uid: String, onPurchased: () -> Unit): String? =
    if (awaitPremium(uid)?.isPremium == true) {
        onPurchased()
        null
    } else {
        "Compra registrada, pero el acceso aun no se ha activado. Vuelve a entrar en un minuto."
    }

// Cancelar no es un fallo: el usuario ya sabe que ha cancelado.
private fun purchaseError(cause: Throwable): String? =
    if ((cause as? PurchasesTransactionException)?.userCancelled == true) null
    else cause.message ?: "No se pudo completar la compra"
