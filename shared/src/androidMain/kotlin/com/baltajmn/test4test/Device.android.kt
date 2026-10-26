package com.baltajmn.test4test

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

@Composable
actual fun SystemBack(enabled: Boolean, onBack: () -> Unit) = BackHandler(enabled, onBack)
