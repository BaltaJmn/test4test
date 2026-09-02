package com.baltajmn.test4test

import org.jetbrains.compose.resources.StringResource
import test4test.shared.generated.resources.Res
import test4test.shared.generated.resources.error_groups_url
import test4test.shared.generated.resources.error_name_empty
import test4test.shared.generated.resources.error_name_too_long
import test4test.shared.generated.resources.error_opt_in_url
import test4test.shared.generated.resources.error_package_mismatch
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

// Los tres enlaces no valen con cualquier https. Un opt-in que no sea una URL de
// prueba de Play devuelve un 404 a quien lo abra, y el que lo abre se va sin ser
// tester ni avisar a nadie: es el fallo que mas se repite en los intercambios.
// Se corta en el alta, que es el unico momento en el que hay alguien mirando.
private const val GROUPS_PREFIX = "https://groups.google.com/"
private const val PLAY_PREFIX = "https://play.google.com/store/apps/details"
private const val OPT_IN_PREFIX = "https://play.google.com/apps/testing/"

// El id de Play acepta parametros en cualquier orden (?hl=es&id=...), asi que se
// busca la clave y no una posicion fija.
private fun playPackage(url: String): String =
    url.substringAfter("id=", "").substringBefore("&")

private fun optInPackage(url: String): String =
    url.substringAfter(OPT_IN_PREFIX, "").substringBefore("?").substringBefore("/")

// Validacion de cliente del alta de app (issue #15): evita el viaje al backend.
// Los mismos limites estan como CHECK en la tabla apps. Devuelve la clave del
// mensaje y no el texto: esta funcion no es composable y no sabe el idioma.
fun appFormError(
    name: String,
    googleGroupsUrl: String,
    playStoreUrl: String,
    optInUrl: String,
): StringResource? {
    val playId = playPackage(playStoreUrl)
    val optInId = optInPackage(optInUrl)
    return when {
        name.trim().isEmpty() -> Res.string.error_name_empty
        name.trim().length > 100 -> Res.string.error_name_too_long
        !googleGroupsUrl.startsWith(GROUPS_PREFIX) -> Res.string.error_groups_url
        !playStoreUrl.startsWith(PLAY_PREFIX) || playId.isEmpty() -> Res.string.error_play_url
        !optInUrl.startsWith(OPT_IN_PREFIX) || optInId.isEmpty() -> Res.string.error_opt_in_url
        // Dos enlaces al mismo sitio: si los paquetes no coinciden, uno de los
        // dos esta mal pegado y el error no se ve hasta que alguien lo abre.
        playId != optInId -> Res.string.error_package_mismatch
        else -> null
    }
}

// La app se anuncia en Telegram y por correo, asi que cada ficha tiene que poder
// enlazarse sola. Hash y no ruta: la web es un unico fichero estatico servido por
// Cloudflare, y con una ruta de verdad cualquier /algo daria 404.
fun appShareUrl(appId: String): String = "https://testers.baltajmn.dev/#$appId"
