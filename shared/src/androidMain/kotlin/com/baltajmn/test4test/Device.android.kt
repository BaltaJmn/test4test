package com.baltajmn.test4test

import android.content.Intent
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

private const val PLAY_STORE = "com.android.vending"

// Ver otros paquetes desde Android 11 pide declararlos en <queries>. El manifest
// declara las apps con icono en el launcher, que es lo que es cualquier app en
// prueba cerrada, y no QUERY_ALL_PACKAGES, que Play restringe.
@Composable
actual fun rememberInstalledFromPlay(): (String) -> Boolean {
    val pm = LocalContext.current.packageManager
    return remember(pm) {
        { packageName ->
            runCatching {
                val installer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    pm.getInstallSourceInfo(packageName).installingPackageName
                } else {
                    @Suppress("DEPRECATION")
                    pm.getInstallerPackageName(packageName)
                }
                installer == PLAY_STORE
                // NameNotFoundException: no esta instalada, o no es visible.
            }.getOrDefault(false)
        }
    }
}

// La ve gracias al <queries> de launcher del manifest, el mismo que usa la
// comprobacion de instalacion.
@Composable
actual fun rememberAppLauncher(): (String) -> Boolean {
    val context = LocalContext.current
    return remember(context) {
        { packageName ->
            context.packageManager.getLaunchIntentForPackage(packageName)
                ?.let { context.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); true }
                ?: false
        }
    }
}

@Composable
actual fun SystemBack(enabled: Boolean, onBack: () -> Unit) = BackHandler(enabled, onBack)
