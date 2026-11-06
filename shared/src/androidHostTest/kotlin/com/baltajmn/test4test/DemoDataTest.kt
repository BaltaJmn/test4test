package com.baltajmn.test4test

import com.baltajmn.test4test.screenshots.Demo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

// Los datos inventados de las capturas de tienda (DemoBackend) tienen que contar la misma
// historia en todas las pantallas: si una cifra no cuadra con otra, la ficha de Play miente.
class DemoDataTest {

    @Test
    fun trailJournalIsTwelveOfTwelveOnDayNine() {
        assertEquals(TESTERS_REQUIRED.toLong(), Demo.trail.followerCount)
        assertEquals(9, Demo.trail.fullDays)
        assertEquals(TesterPhase.HOLDING, testerPhase(Demo.trail.fullDays))
    }

    @Test
    fun ownerListAddsUpToTheCounters() {
        val rows = Demo.ownerRows()
        assertEquals(Demo.trail.followerCount, rows.size.toLong())
        val installed = rows.count { installState(it.installedRecently, it.verifiedAt) == InstallState.INSTALLED }
        assertEquals(Demo.trail.verifiedCount, installed.toLong())
        // Nadie la abre mas dias de los que lleva la racha, y cada tester tiene perfil.
        assertTrue(rows.all { it.openDays <= Demo.trail.fullDays })
        assertTrue(rows.all { row -> Demo.profiles.any { it.id == row.userId } })
        // Entre los 12 hay uno que no se ve con la app desde hace dias y otro que nunca la abrio.
        assertEquals(1, rows.count { installState(it.installedRecently, it.verifiedAt) == InstallState.STALE })
        assertEquals(1, rows.count { it.openDays == 0 })
    }

    @Test
    fun noAppClaimsMoreInstalledThanTesters() {
        assertTrue(Demo.apps.all { it.verifiedCount <= it.followerCount })
        assertTrue(Demo.apps.all { it.followerCount <= TESTERS_REQUIRED })
        assertEquals(Demo.apps.size, Demo.apps.map { it.id }.toSet().size)
    }

    @Test
    fun theRoundIsFourAppsOfOtherPeopleAndTwoAreOpened() {
        val round = Demo.inRound
        assertEquals(4, round.testing.size)
        assertTrue(round.testing.all { id -> Demo.apps.single { it.id == id }.ownerId != Demo.ME })
        assertEquals(2, round.openedToday.size)
        assertTrue(round.testing.containsAll(round.openedToday))
        assertEquals(round.testing, Demo.tested.map { it.id })
    }

    @Test
    fun commentsExistInEveryLanguageAndLinkTheirAuthorsOwnApp() {
        for (language in listOf("en-US", "es-ES", "pt-BR", "hi-IN", "id")) {
            val comments = Demo.comments(language)
            assertEquals(4, comments.size, language)
            assertTrue(comments.all { it.body.isNotBlank() }, language)
            for (comment in comments) {
                val linked = comment.linkedAppId ?: continue
                assertEquals(comment.authorId, assertNotNull(Demo.apps.find { it.id == linked }).ownerId, language)
            }
        }
    }
}
