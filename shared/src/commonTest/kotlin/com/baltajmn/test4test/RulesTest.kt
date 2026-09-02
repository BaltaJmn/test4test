package com.baltajmn.test4test

import test4test.shared.generated.resources.Res
import test4test.shared.generated.resources.error_opt_in_url
import test4test.shared.generated.resources.error_package_mismatch
import test4test.shared.generated.resources.error_play_url
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val GROUPS = "https://groups.google.com/g/test4test-baltajmn"
private const val PLAY = "https://play.google.com/store/apps/details?id=com.baltajmn.test4test"
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
        assertNull(appFormError("Mi app", GROUPS, PLAY, OPT_IN))
    }

    @Test
    fun playLinkAcceptsExtraParamsInAnyOrder() {
        val withLocale = "https://play.google.com/store/apps/details?hl=es&id=com.baltajmn.test4test"
        assertNull(appFormError("Mi app", GROUPS, withLocale, OPT_IN))
        val trailing = "https://play.google.com/store/apps/details?id=com.baltajmn.test4test&hl=es"
        assertNull(appFormError("Mi app", GROUPS, trailing, OPT_IN))
    }

    @Test
    fun blankNameFails() {
        assertNotNull(appFormError("   ", GROUPS, PLAY, OPT_IN))
    }

    @Test
    fun httpsAloneIsNotEnough() {
        // Antes valia cualquier https. Un enlace fuera de Play devuelve 404 a
        // quien lo abra, y quien lo abre se va sin ser tester y sin avisar.
        // La clave y no el texto: appFormError no sabe el idioma.
        assertEquals(
            Res.string.error_play_url,
            appFormError("Mi app", GROUPS, "https://example.com/app", OPT_IN),
        )
        assertEquals(
            Res.string.error_opt_in_url,
            appFormError("Mi app", GROUPS, PLAY, "https://play.google.com/store/apps/details?id=com.x"),
        )
    }

    @Test
    fun playAndOptInMustBeTheSamePackage() {
        assertEquals(
            Res.string.error_package_mismatch,
            appFormError("Mi app", GROUPS, PLAY, "https://play.google.com/apps/testing/com.otra.app"),
        )
    }

    @Test
    fun shareUrlOpensTheApp() {
        assertEquals("https://testers.baltajmn.dev/#abc-123", appShareUrl("abc-123"))
    }
}
