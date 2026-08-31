package com.baltajmn.test4test

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus

sealed interface Screen {
    data object Feed : Screen
    data object MyApps : Screen
    data object Profile : Screen
    data class Detail(val appId: String) : Screen
    // app == null: alta. app != null: edicion (issue #16 reusa el formulario).
    data class EditApp(val app: AppRow?) : Screen
    data object Paywall : Screen
}

@Composable
fun App() {
    // Coil solo autodetecta el fetcher de red en Android, asi que el ImageLoader se
    // construye aqui una vez para las dos plataformas, sobre el motor de Ktor que ya
    // arrastra supabase-kt (okhttp en Android, js en Web).
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components { add(KtorNetworkFetcherFactory()) }
            .build()
    }
    MaterialTheme {
        val status by supabase.auth.sessionStatus.collectAsState()
        when (status) {
            // Initializing va aparte a proposito: colapsarlo con "sin sesion"
            // haria parpadear el login mientras se restaura la sesion guardada.
            is SessionStatus.Initializing -> Loading()
            is SessionStatus.Authenticated -> Home()
            else -> LoginScreen()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Home() {
    val uid = currentUserId ?: return
    val stack = remember { mutableStateListOf<Screen>(Screen.Feed) }
    val current = stack.last()

    // is_premium e is_admin viajan en el mismo fetch (issues #26 y #34).
    var me by remember { mutableStateOf<Profile?>(null) }
    var profileReload by remember { mutableStateOf(0) }
    LaunchedEffect(uid, profileReload) { me = runCatching { profile(uid) }.getOrNull() }

    fun push(screen: Screen) = stack.add(screen)
    fun back() {
        if (stack.size > 1) stack.removeAt(stack.lastIndex)
    }

    fun goRoot(screen: Screen) {
        stack.clear()
        stack.add(screen)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title(current)) },
                navigationIcon = {
                    if (stack.size > 1) TextButton(onClick = ::back) { Text("Atras") }
                },
            )
        },
        bottomBar = {
            if (current is Screen.Feed || current is Screen.MyApps || current is Screen.Profile) {
                NavigationBar {
                    NavigationBarItem(
                        selected = current is Screen.Feed,
                        onClick = { goRoot(Screen.Feed) },
                        icon = {},
                        label = { Text("Feed") },
                    )
                    NavigationBarItem(
                        selected = current is Screen.MyApps,
                        onClick = { goRoot(Screen.MyApps) },
                        icon = {},
                        label = { Text("Mis apps") },
                    )
                    NavigationBarItem(
                        selected = current is Screen.Profile,
                        onClick = { goRoot(Screen.Profile) },
                        icon = {},
                        label = { Text("Perfil") },
                    )
                }
            }
        },
    ) { padding ->
        val modifier = Modifier.padding(padding)
        when (val screen = current) {
            is Screen.Feed -> FeedScreen(uid, me, modifier) { push(Screen.Detail(it)) }
            is Screen.MyApps -> MyAppsScreen(
                uid = uid,
                me = me,
                modifier = modifier,
                onOpen = { push(Screen.Detail(it)) },
                onCreate = { push(Screen.EditApp(null)) },
                onEdit = { push(Screen.EditApp(it)) },
                onPaywall = { push(Screen.Paywall) },
            )
            is Screen.Profile -> ProfileScreen(
                uid = uid,
                me = me,
                modifier = modifier,
                onOpen = { push(Screen.Detail(it)) },
                onPaywall = { push(Screen.Paywall) },
            )
            is Screen.Detail -> AppDetailScreen(
                appId = screen.appId,
                uid = uid,
                me = me,
                modifier = modifier,
                onOpen = { push(Screen.Detail(it)) },
                onDeleted = ::back,
            )
            is Screen.EditApp -> CreateAppScreen(
                uid = uid,
                me = me,
                existing = screen.app,
                modifier = modifier,
                onDone = {
                    profileReload++
                    back()
                },
                // Se sale del formulario antes de entrar al paywall: volver atras
                // desde ahi tiene que llevar a "Mis apps", no a un alta que la RLS
                // acaba de rechazar.
                onPaywall = {
                    back()
                    push(Screen.Paywall)
                },
            )
            is Screen.Paywall -> PaywallScreen(
                uid = uid,
                me = me,
                modifier = modifier,
                onPurchased = {
                    profileReload++
                    back()
                },
            )
        }
    }
}

private fun title(screen: Screen): String = when (screen) {
    is Screen.Feed -> "Test4Test"
    is Screen.MyApps -> "Mis apps"
    is Screen.Profile -> "Perfil"
    is Screen.Detail -> "Detalle"
    is Screen.EditApp -> if (screen.app == null) "Nueva app" else "Editar app"
    is Screen.Paywall -> "Slots ilimitados"
}
