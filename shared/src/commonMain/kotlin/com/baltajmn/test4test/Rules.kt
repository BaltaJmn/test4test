package com.baltajmn.test4test

import org.jetbrains.compose.resources.StringResource
import test4test.shared.generated.resources.Res
import test4test.shared.generated.resources.error_groups_url
import test4test.shared.generated.resources.error_name_empty
import test4test.shared.generated.resources.error_name_too_long
import test4test.shared.generated.resources.error_opt_in_url
import test4test.shared.generated.resources.error_play_url

// Plan free: 1 app por usuario. La misma condicion vive en la policy
// apps_insert_own_within_slots; aqui solo se replica para no llevar al usuario
// a un formulario que el backend va a rechazar.
fun canCreateApp(isPremium: Boolean, ownedApps: Int): Boolean = isPremium || ownedApps < 1

// Inicial del avatar (issue #27). Se pinta cuando el perfil no trae foto y tambien
// por debajo mientras carga, asi que tiene que devolver algo siempre: un nombre en
// blanco o a solo espacios cae al interrogante.
fun avatarInitial(displayName: String?): String =
    displayName?.trim()?.take(1)?.uppercase()?.takeIf { it.isNotBlank() } ?: "?"

// Validacion de cliente del alta de app (issue #15): evita el viaje al backend.
// Los mismos limites estan como CHECK en la tabla apps. Devuelve la clave del
// mensaje y no el texto: esta funcion no es composable y no sabe el idioma.
fun appFormError(
    name: String,
    googleGroupsUrl: String,
    playStoreUrl: String,
    optInUrl: String,
): StringResource? = when {
    name.trim().isEmpty() -> Res.string.error_name_empty
    name.trim().length > 100 -> Res.string.error_name_too_long
    !googleGroupsUrl.startsWith("https://") -> Res.string.error_groups_url
    !playStoreUrl.startsWith("https://") -> Res.string.error_play_url
    !optInUrl.startsWith("https://") -> Res.string.error_opt_in_url
    else -> null
}
