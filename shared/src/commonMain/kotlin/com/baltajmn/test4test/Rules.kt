package com.baltajmn.test4test

import org.jetbrains.compose.resources.StringResource
import test4test.shared.generated.resources.Res
import test4test.shared.generated.resources.error_groups_url
import test4test.shared.generated.resources.error_name_empty
import test4test.shared.generated.resources.error_name_too_long
import test4test.shared.generated.resources.error_opt_in_url

// Google Play pide 12 testers y que aguanten 14 dias seguidos antes de dejar
// publicar a una cuenta personal. Son las dos mitades del mismo requisito.
const val TESTERS_REQUIRED = 12
const val FULL_DAYS_REQUIRED = 14

// Y la segunda mitad no puede empezar hasta que acaba la primera: son fases de
// una sola escala, no dos medidores. El trigger sync_full_since deja full_since
// a null en cuanto el recuento baja de 12, asi que full_days lo dice todo: si
// vale algo es que los 12 estan puestos, y si se va uno vuelve a cero solo.
enum class TesterPhase { FILLING, HOLDING, DONE }

// full_days es el dia en curso: vale 1 el dia que se llega a 12. Los 14 dias
// estan cumplidos cuando ya ha empezado el 15, no al empezar el 14: darlo por
// hecho un dia antes manda a la gente a pedir produccion y que Play lo rechace.
fun testerPhase(fullDays: Int): TesterPhase = when {
    fullDays > FULL_DAYS_REQUIRED -> TesterPhase.DONE
    fullDays > 0 -> TesterPhase.HOLDING
    else -> TesterPhase.FILLING
}

// Plan free: 1 app a la vez. La misma condicion vive en la policy
// apps_insert_own_within_slots; aqui solo se replica para no llevar al usuario
// a un formulario que el backend va a rechazar.
fun canCreateApp(isPremium: Boolean, ownedApps: Int): Boolean = isPremium || ownedApps < 1

// El movil del tester dice si la app esta instalada; la vista dice si eso se vio
// en los ultimos tres dias. Visto alguna vez pero hace mas es lo que el dueno
// tiene que perseguir: o la desinstalo o dejo de abrir Test4Test.
fun installState(installedRecently: Boolean?, verifiedAt: String?): InstallState = when {
    installedRecently == true -> InstallState.INSTALLED
    verifiedAt != null -> InstallState.STALE
    else -> InstallState.UNVERIFIED
}

// Inicial del avatar (issue #27). Se pinta cuando el perfil no trae foto y tambien
// por debajo mientras carga, asi que tiene que devolver algo siempre: un nombre en
// blanco o a solo espacios cae al interrogante.
fun avatarInitial(displayName: String?): String =
    displayName?.trim()?.take(1)?.uppercase()?.takeIf { it.isNotBlank() } ?: "?"

// Los enlaces no valen con cualquier https. Un opt-in que no sea una URL de
// prueba de Play devuelve un 404 a quien lo abra, y el que lo abre se va sin ser
// tester ni avisar a nadie: es el fallo que mas se repite en los intercambios.
// Se corta en el alta, que es el unico momento en el que hay alguien mirando. La
// tabla apps tiene los mismos CHECK por si alguien se salta el formulario.
private const val GROUPS_PREFIX = "https://groups.google.com/"
private const val OPT_IN_PREFIX = "https://play.google.com/apps/testing/"
private val PACKAGE = Regex("[A-Za-z0-9_.]+")

private fun optInPackage(url: String): String =
    url.trim().substringAfter(OPT_IN_PREFIX, "").substringBefore("?").substringBefore("/")

// El enlace de la ficha de Play ya no se pide: es siempre el mismo con el paquete
// del opt-in, y pedirlo aparte solo daba ocasion de pegar uno que no cuadrase.
fun playUrlFor(optInUrl: String): String =
    "https://play.google.com/store/apps/details?id=" + optInPackage(optInUrl)

// Validacion de cliente del alta de app (issue #15): evita el viaje al backend.
// Devuelve la clave del mensaje y no el texto: esta funcion no es composable y no
// sabe el idioma.
fun appFormError(
    name: String,
    googleGroupsUrl: String,
    optInUrl: String,
): StringResource? = when {
    name.trim().isEmpty() -> Res.string.error_name_empty
    name.trim().length > 100 -> Res.string.error_name_too_long
    !googleGroupsUrl.trim().startsWith(GROUPS_PREFIX) -> Res.string.error_groups_url
    !optInUrl.trim().startsWith(OPT_IN_PREFIX) || !PACKAGE.matches(optInPackage(optInUrl)) ->
        Res.string.error_opt_in_url
    else -> null
}

// La app se anuncia en Telegram y por correo, asi que cada ficha tiene que poder
// enlazarse sola. La pagina de ese dominio abre la ficha en la app instalada, o
// explica como entrar en la prueba si no lo esta. Hash y no ruta: es una pagina
// estatica, y con una ruta de verdad cualquier /algo daria 404.
fun appShareUrl(appId: String): String = "https://testers.baltajmn.dev/#$appId"

// Lo que llega por test4test://app/<id>, que es a donde manda esa pagina. Solo un
// uuid: el resto de la URL viene de fuera y no se pasa a ninguna consulta.
private val UUID = Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")

fun appIdFromDeepLink(host: String?, path: String?): String? =
    path?.trim('/')?.takeIf { host == "app" && UUID.matches(it) }
