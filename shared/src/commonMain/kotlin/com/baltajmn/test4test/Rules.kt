package com.baltajmn.test4test

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
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

// El aviso diario, uno como mucho. La ronda pendiente manda y sale cada dia; a
// quien no prueba nada se le recuerda que hay apps esperando, pero cada tres
// dias: a diario es la forma mas rapida de que desinstale.
const val NUDGE_EVERY_DAYS = 3

enum class DailyNudge { ROUND, JOIN, NONE }

fun dailyNudge(testing: Int, pending: Int, waiting: Int, daysSinceNudge: Long): DailyNudge = when {
    pending > 0 -> DailyNudge.ROUND
    testing == 0 && waiting > 0 && daysSinceNudge >= NUDGE_EVERY_DAYS -> DailyNudge.JOIN
    else -> DailyNudge.NONE
}

// Valoracion (V3, V4): dialogo del sistema tras completar la ronda, que se
// alcanza sin pagar. Todo a la vez: 2 dias o mas desde instalar, segundo
// arranque o posterior, una vez por version, 30 dias o mas entre peticiones,
// nada de Pro, consentimiento ni fallo en esta sesion (`busy`) y el presupuesto
// del dia sin gastar (`budgetOpen`, M7).
const val REVIEW_MIN_INSTALL_DAYS = 2
const val REVIEW_GAP_DAYS = 30
private const val DAY_MS = 86_400_000L

fun reviewDue(
    nowMs: Long,
    installedMs: Long,
    launches: Int,
    version: Long,
    askedVersion: Long,
    lastAskedMs: Long,
    busy: Boolean,
    budgetOpen: Boolean,
): Boolean = !busy &&
    budgetOpen &&
    launches >= 2 &&
    version != askedVersion &&
    nowMs - installedMs >= REVIEW_MIN_INSTALL_DAYS * DAY_MS &&
    nowMs - lastAskedMs >= REVIEW_GAP_DAYS * DAY_MS

// Presupuesto del dia (M7): como mucho una peticion cada 24 h entre el toque de Pro,
// la valoracion y el formulario de consentimiento de los anuncios. `lastAskMs` es el
// instante de la ultima peticion, 0 si nunca hubo una; con el reloj atrasado (la
// ultima cae en el futuro) el presupuesto se da por abierto.
fun askBudgetOpen(lastAskMs: Long, nowMs: Long): Boolean =
    lastAskMs == 0L || nowMs - lastAskMs >= DAY_MS || lastAskMs > nowMs

// Toque suave de Pro en Mis apps (M2, M3): la linea de "saltar la cola" sale la
// primera vez que hay una app esperando, se cierra y dura una sola sesion; vuelve
// una sola vez mas, 7 dias despues de la primera vez que se enseno (no de cuando se
// cerro). `offer` es que haya algo que ofrecer (app en la cola sin ir delante, y
// sin suscripcion).
enum class ProTouch { NONE, FIRST, REMINDER }

const val PRO_REMINDER_DAYS = 7

fun proTouchDue(
    offer: Boolean,
    nowMs: Long,
    firstShownMs: Long,
    reminded: Boolean,
    budgetOpen: Boolean,
): ProTouch = when {
    !offer || !budgetOpen -> ProTouch.NONE
    firstShownMs == 0L -> ProTouch.FIRST
    !reminded && nowMs - firstShownMs >= PRO_REMINDER_DAYS * DAY_MS -> ProTouch.REMINDER
    else -> ProTouch.NONE
}

// Plan free: 1 app a la vez. La misma condicion vive en la policy
// apps_insert_own_within_slots; aqui solo se replica para no llevar al usuario
// a un formulario que el backend va a rechazar.
const val FREE_APP_LIMIT = 1

fun canCreateApp(isPremium: Boolean, ownedApps: Int): Boolean = isPremium || ownedApps < FREE_APP_LIMIT

// Anuncios solo en el plan free: la suscripcion los quita. Sin perfil todavia,
// ninguno, para no ensenarle uno a quien paga mientras carga. Al admin tampoco:
// tocar tus propios anuncios es trafico no valido y AdMob cierra la cuenta, que
// es la misma de las demas apps.
//
// D1: tampoco la primera semana desde instalar, ni nunca a quien ya usaba la app
// antes de que los anuncios esperaran (instalada antes de ADS_EXEMPT_BEFORE_MS), y
// el interruptor "ads" de config.json de la web los apaga sin version. `remoteOn`
// es null hasta la primera respuesta de esta ejecucion: sin banner ni formulario
// de consentimiento mientras tanto.
const val ADS_AFTER_DAYS = 7

// El dia en que se envia la 1.15.0, la primera con la semana sin anuncios: 2026-11-21 UTC.
// Nunca posterior a su publicacion: quien instalara entre medias quedaria exento para siempre.
const val ADS_EXEMPT_BEFORE_MS = 1_795_219_200_000L

// Si a esta persona le puede salir un anuncio alguna vez: sin perfil todavia, con
// suscripcion, admin o instalada antes de ADS_EXEMPT_BEFORE_MS, nunca; y entonces ni
// se lee config.json, para no pedirselo a quien no lo necesita.
fun mayEverShowAds(me: Profile?, installedMs: Long): Boolean =
    me != null && !me.isPremium && !me.isAdmin && installedMs >= ADS_EXEMPT_BEFORE_MS

fun showsAds(me: Profile?, installedMs: Long, nowMs: Long, remoteOn: Boolean?): Boolean =
    mayEverShowAds(me, installedMs) && remoteOn == true && nowMs - installedMs >= ADS_AFTER_DAYS * DAY_MS

@Serializable
private class RemoteConfig(val ads: Boolean = true)

private val remoteJson = Json { ignoreUnknownKeys = true }

// Lo que dice el interruptor tras leer config.json: el archivo si se entiende; si no
// (sin red, 404, JSON roto), lo que esta ejecucion ya sabia; y si no sabia nada,
// encendido: el interruptor existe para apagar los anuncios, no para depender de el.
fun nextAdsOn(previous: Boolean?, json: String?): Boolean =
    json?.let { runCatching { remoteJson.decodeFromString<RemoteConfig>(it).ads }.getOrNull() } ?: previous ?: true

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
