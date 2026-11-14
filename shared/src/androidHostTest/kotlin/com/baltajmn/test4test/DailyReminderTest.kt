package com.baltajmn.test4test

import android.Manifest
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.baltajmn.test4test.screenshots.Demo
import com.baltajmn.test4test.screenshots.DemoBackend
import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.auth.MemoryCodeVerifierCache
import io.github.jan.supabase.auth.MemorySessionManager
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.auth.user.UserSession
import io.ktor.client.engine.mock.MockEngine
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import test4test.shared.generated.resources.Res
import test4test.shared.generated.resources.joined_title
import test4test.shared.generated.resources.nudge_title
import test4test.shared.generated.resources.round_title

// El aviso diario entero, sin WorkManager: sesion restaurada, lectura del servidor
// (el de demo de las capturas) y lo que acaba en la bandeja del sistema.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DailyReminderTest {

    @OptIn(SupabaseInternal::class)
    @Test
    fun notifiesTheRoundThenTheWaitingAppsAndTheNewTester() = runBlocking {
        TestOverrides.engine = DemoBackend.engine
        TestOverrides.sessionManager = MemorySessionManager()
        TestOverrides.codeVerifierCache = MemoryCodeVerifierCache()
        // `supabase` se crea una sola vez: si otra prueba lo creo antes con el motor de
        // verdad, esta se salta en vez de hablar con produccion.
        assumeTrue(supabase.httpClient.httpClient.engine is MockEngine)
        supabase.auth.importSession(
            UserSession(
                accessToken = "demo", refreshToken = "demo", expiresIn = 3600, tokenType = "bearer",
                user = UserInfo(aud = "authenticated", id = Demo.ME), expiresAt = Clock.System.now() + 1.hours,
            ),
            autoRefresh = false,
        )
        val app = ApplicationProvider.getApplicationContext<Application>()
        val tray = shadowOf(app.getSystemService(NotificationManager::class.java))
        fun title(id: Int) = tray.getNotification(id)?.extras?.getString(Notification.EXTRA_TITLE)

        // Sin permiso no sale nada, y no es motivo para reintentar.
        DemoBackend.scenario = Demo.inRound
        assertTrue(runDailyReminder(app))
        assertEquals(0, tray.size())

        // Ronda a medias (2 de 4): aviso de la ronda. Los testers propios solo se apuntan.
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        assertTrue(runDailyReminder(app))
        assertEquals(getString(Res.string.round_title), title(1))
        assertNull(title(2))

        // Quien no prueba nada: apps esperando. Y si una app propia tenia menos testers
        // la ultima vez, tester nuevo.
        app.getSharedPreferences("round", Context.MODE_PRIVATE).edit().putInt("f_" + Demo.trail.id, 11).apply()
        DemoBackend.scenario = Demo.newcomer
        assertTrue(runDailyReminder(app))
        assertEquals(getString(Res.string.nudge_title), title(1))
        assertEquals(getString(Res.string.joined_title, Demo.trail.name), title(2))

        // Al dia siguiente no se repite el empujon (cada NUDGE_EVERY_DAYS) ni el tester ya contado.
        app.getSystemService(NotificationManager::class.java).cancelAll()
        assertTrue(runDailyReminder(app))
        assertEquals(0, tray.size())
        assertTrue(DemoBackend.unexpected.isEmpty(), DemoBackend.unexpected.toString())
    }
}
