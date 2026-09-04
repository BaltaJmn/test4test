package com.baltajmn.test4test

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import org.jetbrains.compose.resources.stringResource
import test4test.shared.generated.resources.Res
import test4test.shared.generated.resources.app_name
import test4test.shared.generated.resources.nav_back
import test4test.shared.generated.resources.tab_feed
import test4test.shared.generated.resources.tab_my_apps
import test4test.shared.generated.resources.tab_profile
import test4test.shared.generated.resources.title_detail
import test4test.shared.generated.resources.title_edit_app
import test4test.shared.generated.resources.title_new_app
import test4test.shared.generated.resources.title_paywall

sealed interface Screen {
    data object Feed : Screen
    data object MyApps : Screen
    data object Profile : Screen
    // El perfil de otra persona se apila encima en vez de sustituir la pestana:
    // volver atras tiene que devolver a la ficha desde la que se toco el nombre.
    data class User(val userId: String) : Screen
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
    Test4TestTheme {
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
    // Feed siempre debajo: al abrir un enlace compartido de una app, atras tiene
    // que llevar al feed y no cerrar la pagina.
    val stack = remember {
        mutableStateListOf<Screen>(Screen.Feed).apply {
            startAppId()?.let { add(Screen.Detail(it)) }
        }
    }
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
                title = { Text(title(current), style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    if (stack.size > 1) TextButton(onClick = ::back) { Text(stringResource(Res.string.nav_back)) }
                },
                // Transparente sobre el papel del Scaffold: sin barra de color, el
                // titulo se lee como el encabezado de la pagina y no como un bloque.
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        },
        bottomBar = {
            if (current is Screen.Feed || current is Screen.MyApps || current is Screen.Profile) {
                BottomTabs(current, ::goRoot)
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
                viewerId = uid,
                me = me,
                modifier = modifier,
                onOpen = { push(Screen.Detail(it)) },
                onPaywall = { push(Screen.Paywall) },
            )
            is Screen.User -> ProfileScreen(
                uid = screen.userId,
                viewerId = uid,
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
                onOpenProfile = { push(Screen.User(it)) },
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

// Barra propia y no NavigationBar: la app no tiene juego de iconos, y
// NavigationBar sin icono deja las etiquetas apretadas contra el borde. Aqui la
// pestana activa se marca con un subrayado, que es lo unico que hace falta.
@Composable
private fun BottomTabs(current: Screen, onSelect: (Screen) -> Unit) {
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars)) {
            Tab(stringResource(Res.string.tab_feed), current is Screen.Feed) { onSelect(Screen.Feed) }
            Tab(stringResource(Res.string.tab_my_apps), current is Screen.MyApps) { onSelect(Screen.MyApps) }
            Tab(stringResource(Res.string.tab_profile), current is Screen.Profile) { onSelect(Screen.Profile) }
        }
    }
}

@Composable
private fun RowScope.Tab(label: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .weight(1f)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(top = 12.dp, bottom = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box(
            Modifier
                .width(18.dp)
                .height(2.dp)
                .background(
                    if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    RoundedCornerShape(1.dp),
                )
        )
    }
}

@Composable
private fun title(screen: Screen): String = stringResource(
    when (screen) {
        is Screen.Feed -> Res.string.app_name
        is Screen.MyApps -> Res.string.tab_my_apps
        is Screen.Profile, is Screen.User -> Res.string.tab_profile
        is Screen.Detail -> Res.string.title_detail
        is Screen.EditApp -> if (screen.app == null) Res.string.title_new_app else Res.string.title_edit_app
        is Screen.Paywall -> Res.string.title_paywall
    }
)
