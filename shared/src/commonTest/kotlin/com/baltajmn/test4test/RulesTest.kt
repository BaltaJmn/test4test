package com.baltajmn.test4test

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
    fun validFormPasses() {
        assertNull(appFormError("Mi app", "https://g.co/a", "https://play.google.com/b", "https://g.co/c"))
    }

    @Test
    fun blankNameAndHttpUrlsFail() {
        assertNotNull(appFormError("   ", "https://a", "https://b", "https://c"))
        assertEquals(
            "El enlace de Play Store tiene que empezar por https://",
            appFormError("Mi app", "https://a", "http://b", "https://c"),
        )
    }
}
