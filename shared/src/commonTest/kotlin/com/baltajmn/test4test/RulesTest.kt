package com.baltajmn.test4test

import test4test.shared.generated.resources.Res
import test4test.shared.generated.resources.error_play_url
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

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
        assertNull(appFormError("Mi app", "https://g.co/a", "https://play.google.com/b", "https://g.co/c"))
    }

    @Test
    fun blankNameAndHttpUrlsFail() {
        assertNotNull(appFormError("   ", "https://a", "https://b", "https://c"))
        // La clave y no el texto: appFormError no sabe el idioma, lo resuelve la UI.
        assertEquals(
            Res.string.error_play_url,
            appFormError("Mi app", "https://a", "http://b", "https://c"),
        )
    }
}
