package com.baltajmn.test4test

import test4test.shared.generated.resources.Res
import test4test.shared.generated.resources.error_groups_url
import test4test.shared.generated.resources.error_opt_in_url
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val GROUPS = "https://groups.google.com/g/test4test-baltajmn"
private const val OPT_IN = "https://play.google.com/apps/testing/com.baltajmn.test4test"

class RulesTest {

    @Test
    fun dailyNudgeAsksForTheRoundFirstAndSpacesTheRest() {
        assertEquals(DailyNudge.ROUND, dailyNudge(testing = 2, pending = 1, waiting = 0, daysSinceNudge = 0))
        // Ronda hecha: silencio, aunque haya mas apps esperando.
        assertEquals(DailyNudge.NONE, dailyNudge(testing = 2, pending = 0, waiting = 5, daysSinceNudge = 99))
        // Quien no prueba nada: un empujon cada tres dias, y solo si hay algo que probar.
        assertEquals(DailyNudge.JOIN, dailyNudge(testing = 0, pending = 0, waiting = 3, daysSinceNudge = 3))
        assertEquals(DailyNudge.NONE, dailyNudge(testing = 0, pending = 0, waiting = 3, daysSinceNudge = 2))
        assertEquals(DailyNudge.NONE, dailyNudge(testing = 0, pending = 0, waiting = 0, daysSinceNudge = 99))
    }

    @Test
    fun freeUserGetsOneSlot() {
        assertTrue(canCreateApp(isPremium = false, ownedApps = 0))
        assertFalse(canCreateApp(isPremium = false, ownedApps = 1))
    }

    @Test
    fun premiumUserIsNeverBlocked() {
        assertTrue(canCreateApp(isPremium = true, ownedApps = 0))
        assertTrue(canCreateApp(isPremium = true, ownedApps = 7))
    }

    // D1: la app instalada el dia 100 despues de ADS_EXEMPT_BEFORE_MS, y el reloj en dias desde entonces.
    private val adsDay = 86_400_000L
    private val installedAfter = ADS_EXEMPT_BEFORE_MS + 100 * adsDay
    private fun ads(me: Profile?, daysSinceInstall: Int = ADS_AFTER_DAYS, remote: Boolean? = true, installed: Long = installedAfter) =
        showsAds(me, installed, installed + daysSinceInstall * adsDay, remote)

    @Test
    fun adsOnlyForFreeUsers() {
        assertTrue(ads(Profile(id = "a")))
        assertFalse(ads(Profile(id = "a", isPremium = true)))
        assertFalse(ads(Profile(id = "a", isAdmin = true)))
        assertFalse(ads(null))
    }

    @Test
    fun noAdsTheFirstWeek() {
        // Siete dias cumplidos, escritos a mano para que cambiar la constante rompa el test.
        assertEquals(7, ADS_AFTER_DAYS)
        val installed = installedAfter
        val me = Profile(id = "a")
        assertFalse(showsAds(me, installed, installed + 6 * adsDay, remoteOn = true))
        assertFalse(showsAds(me, installed, installed + 7 * adsDay - 1, remoteOn = true))
        assertTrue(showsAds(me, installed, installed + 7 * adsDay, remoteOn = true))
        // Con el reloj atrasado respecto a la instalacion tampoco hay banner.
        assertFalse(ads(me, daysSinceInstall = -1))
    }

    @Test
    fun whoUsedTheAppBeforeNeverSeesAds() {
        // 2026-11-21T00:00:00Z: el dia 20 778 desde 1970, a medianoche en punto.
        assertEquals(20_778L, ADS_EXEMPT_BEFORE_MS / adsDay)
        assertEquals(0L, ADS_EXEMPT_BEFORE_MS % adsDay)
        val before = ADS_EXEMPT_BEFORE_MS - 1
        assertFalse(ads(Profile(id = "a"), daysSinceInstall = 400, installed = before))
        assertTrue(ads(Profile(id = "a"), daysSinceInstall = 400, installed = ADS_EXEMPT_BEFORE_MS))
    }

    @Test
    fun configJsonIsOnlyReadForWhoCouldEverSeeAnAd() {
        assertTrue(mayEverShowAds(Profile(id = "a"), installedAfter))
        assertFalse(mayEverShowAds(null, installedAfter))
        assertFalse(mayEverShowAds(Profile(id = "a", isPremium = true), installedAfter))
        assertFalse(mayEverShowAds(Profile(id = "a", isAdmin = true), installedAfter))
        assertFalse(mayEverShowAds(Profile(id = "a"), ADS_EXEMPT_BEFORE_MS - 1))
    }

    @Test
    fun theRemoteSwitchTurnsAdsOffAndWaitsForItsFirstAnswer() {
        assertFalse(ads(Profile(id = "a"), remote = false))
        // Sin respuesta todavia: ni banner ni formulario de consentimiento.
        assertFalse(ads(Profile(id = "a"), remote = null))
    }

    @Test
    fun theSwitchReadsConfigJsonAndFallsBackToOn() {
        assertFalse(nextAdsOn(null, """{"ads": false}"""))
        assertTrue(nextAdsOn(null, """{"ads": true}"""))
        // Se puede volver a encender, y el fichero que se publica se entiende tal cual.
        assertTrue(nextAdsOn(false, """{"ads": true}"""))
        assertTrue(nextAdsOn(false, "{ \"ads\": true }\n"))
        assertFalse(nextAdsOn(true, "{ \"ads\": false }\n"))
        // Claves que no conoce, no importan; sin la clave, encendido.
        assertFalse(nextAdsOn(true, """{"ads": false, "later": 1}"""))
        assertTrue(nextAdsOn(false, "{}"))
        // Sin red, 404, cuerpo vacio o JSON roto: lo que ya sabia esta ejecucion, y si no, encendido.
        assertFalse(nextAdsOn(false, null))
        assertFalse(nextAdsOn(false, ""))
        assertFalse(nextAdsOn(false, "<html>404</html>"))
        assertFalse(nextAdsOn(false, """{"ads": null}"""))
        assertTrue(nextAdsOn(null, null))
        assertTrue(nextAdsOn(null, "<html>404</html>"))
        assertTrue(nextAdsOn(null, """{"ads": "no"}"""))
    }

    @Test
    fun avatarInitialAlwaysRendersSomething() {
        assertEquals("B", avatarInitial("baltasar"))
        assertEquals("B", avatarInitial("  Baltasar Jimenez  "))
        // Sin nombre no puede quedarse el circulo vacio: la foto puede tardar o no venir.
        assertEquals("?", avatarInitial(null))
        assertEquals("?", avatarInitial(""))
        assertEquals("?", avatarInitial("   "))
    }

    @Test
    fun validFormPasses() {
        assertNull(appFormError("Mi app", GROUPS, OPT_IN))
        // Pegado con espacios o con parametros detras: se recorta, no se rechaza.
        assertNull(appFormError("Mi app", " $GROUPS ", "$OPT_IN?hl=es "))
    }

    @Test
    fun blankNameFails() {
        assertNotNull(appFormError("   ", GROUPS, OPT_IN))
    }

    @Test
    fun httpsAloneIsNotEnough() {
        // Antes valia cualquier https. Un enlace fuera de Play devuelve 404 a
        // quien lo abra, y quien lo abre se va sin ser tester y sin avisar.
        // La clave y no el texto: appFormError no sabe el idioma.
        assertEquals(Res.string.error_groups_url, appFormError("Mi app", "https://example.com/g", OPT_IN))
        assertEquals(
            Res.string.error_opt_in_url,
            appFormError("Mi app", GROUPS, "https://play.google.com/store/apps/details?id=com.x"),
        )
        // El prefijo solo, sin paquete detras, tampoco: abriria la raiz de Play.
        assertEquals(
            Res.string.error_opt_in_url,
            appFormError("Mi app", GROUPS, "https://play.google.com/apps/testing/"),
        )
    }

    @Test
    fun playLinkIsDerivedFromTheOptIn() {
        // La tabla apps exige que los dos enlaces lleven el mismo paquete, asi
        // que el de Play sale del opt-in y no se le pide al usuario.
        assertEquals(
            "https://play.google.com/store/apps/details?id=com.baltajmn.test4test",
            playUrlFor("$OPT_IN?hl=es"),
        )
    }

    @Test
    fun testerMeterPhasesFollowTheStreak() {
        // Sin racha se esta llenando, aunque ya haya gente dentro.
        assertEquals(TesterPhase.FILLING, testerPhase(0))
        // full_days es el dia en curso: el 1 es el dia que se llega a 12.
        assertEquals(TesterPhase.HOLDING, testerPhase(1))
        // El dia 14 todavia corre: dar la racha por hecha ahi manda a pedir
        // produccion un dia antes de tiempo.
        assertEquals(TesterPhase.HOLDING, testerPhase(FULL_DAYS_REQUIRED))
        assertEquals(TesterPhase.DONE, testerPhase(FULL_DAYS_REQUIRED + 1))
    }

    @Test
    fun installStateTellsTheOwnerWhoReallyHasTheApp() {
        assertEquals(InstallState.INSTALLED, installState(true, "2026-10-26T10:00:00Z"))
        // Vista alguna vez pero no en los ultimos dias: hay que preguntarle.
        assertEquals(InstallState.STALE, installState(false, "2026-10-01T10:00:00Z"))
        assertEquals(InstallState.UNVERIFIED, installState(null, null))
    }

    @Test
    fun queuePositionOnlyWhileWaiting() {
        fun row(testers: Long, rank: Int) = AppRow("a", "o", "n", "g", "p", "i", followerCount = testers, queueRank = rank)
        assertEquals(3, row(11, 3).queuePosition)
        // Con sus 12 ya no espera: sin puesto aunque la vista le de rango.
        assertNull(row(TESTERS_REQUIRED.toLong(), 9).queuePosition)
        // Fila que no viene de la vista (sin rango): sin puesto.
        assertNull(row(0, 0).queuePosition)
    }

    @Test
    fun reviewNeedsEveryConditionAtOnce() {
        val day = 86_400_000L
        val now = 100 * day
        fun due(
            installed: Long = now - 3 * day,
            launches: Int = 2,
            version: Long = 17,
            askedVersion: Long = 16,
            last: Long = 0,
            busy: Boolean = false,
            budgetOpen: Boolean = true,
        ) = reviewDue(now, installed, launches, version, askedVersion, last, busy, budgetOpen)
        assertTrue(due())
        assertFalse(due(installed = now - day))
        assertFalse(due(launches = 1))
        assertFalse(due(askedVersion = 17))
        assertFalse(due(last = now - 29 * day))
        assertTrue(due(last = now - 30 * day))
        assertFalse(due(busy = true))
        // M7: ni el dia en que ya hubo otra peticion.
        assertFalse(due(budgetOpen = false))
    }

    @Test
    fun oneAskEvery24Hours() {
        val hour = 3_600_000L
        val now = 100 * 24 * hour
        assertTrue(askBudgetOpen(lastAskMs = 0, nowMs = now))
        // Ventana de 24 h, no el dia del calendario: ni un milisegundo antes de cumplirse.
        assertFalse(askBudgetOpen(lastAskMs = now - 1, nowMs = now))
        assertFalse(askBudgetOpen(lastAskMs = now - 24 * hour + 1, nowMs = now))
        assertTrue(askBudgetOpen(lastAskMs = now - 24 * hour, nowMs = now))
        // Reloj atrasado: la ultima peticion cae en el futuro y no deja el presupuesto cerrado para siempre.
        assertTrue(askBudgetOpen(lastAskMs = now + hour, nowMs = now))
    }

    @Test
    fun theProTouchComesOnceAndAWeekLaterOnceMore() {
        val day = 86_400_000L
        val now = 100 * day
        fun touch(
            offer: Boolean = true,
            first: Long = 0,
            reminded: Boolean = false,
            budget: Boolean = true,
        ) = proTouchDue(offer, now, first, reminded, budget)
        // La primera vez que hay algo que ofrecer.
        assertEquals(ProTouch.FIRST, touch())
        assertEquals(ProTouch.NONE, touch(offer = false))
        assertEquals(ProTouch.NONE, touch(budget = false))
        // Despues de ensenarla no vuelve en cada arranque...
        assertEquals(ProTouch.NONE, touch(first = now - 6 * day))
        assertEquals(ProTouch.NONE, touch(first = now - 7 * day + 1))
        // ... sino 7 dias despues de la primera vez que se enseno, y solo esa vez.
        assertEquals(ProTouch.REMINDER, touch(first = now - 7 * day))
        assertEquals(ProTouch.NONE, touch(first = now - 30 * day, reminded = true))
        // Y el presupuesto del dia manda tambien sobre el recordatorio.
        assertEquals(ProTouch.NONE, touch(first = now - 30 * day, budget = false))
        // Una primera vez apuntada en el futuro (reloj atrasado) no vuelve a salir.
        assertEquals(ProTouch.NONE, touch(first = now + day))
    }
}
