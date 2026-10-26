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
    fun freeUserGetsOneSlot() {
        assertTrue(canCreateApp(isPremium = false, ownedApps = 0))
        assertFalse(canCreateApp(isPremium = false, ownedApps = 1))
    }

    @Test
    fun premiumUserIsNeverBlocked() {
        assertTrue(canCreateApp(isPremium = true, ownedApps = 0))
        assertTrue(canCreateApp(isPremium = true, ownedApps = 7))
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
    fun deepLinkOnlyOpensAppIds() {
        val id = "0b6c1c2e-3f4a-4b5c-8d9e-0f1a2b3c4d5e"
        assertEquals(id, appIdFromDeepLink("app", "/$id"))
        // La vuelta del login usa el mismo scheme con otro host.
        assertNull(appIdFromDeepLink("login", "/$id"))
        // Lo que llega de fuera no pasa a una consulta si no es un uuid.
        assertNull(appIdFromDeepLink("app", "/x' or 1=1"))
        assertNull(appIdFromDeepLink("app", null))
    }

    @Test
    fun shareUrlOpensTheApp() {
        assertEquals("https://testers.baltajmn.dev/#abc-123", appShareUrl("abc-123"))
    }
}
