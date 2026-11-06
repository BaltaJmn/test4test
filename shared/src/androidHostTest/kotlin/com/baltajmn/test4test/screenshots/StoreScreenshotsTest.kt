package com.baltajmn.test4test.screenshots

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.printToString
import androidx.compose.ui.test.hasText
import androidx.test.core.app.ApplicationProvider
import com.baltajmn.test4test.App
import com.baltajmn.test4test.TestOverrides
import com.baltajmn.test4test.supabase
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.auth.MemoryCodeVerifierCache
import io.github.jan.supabase.auth.MemorySessionManager
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.auth.user.UserSession
import io.ktor.client.engine.mock.MockEngine
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.model.Statement
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import test4test.shared.generated.resources.Res
import test4test.shared.generated.resources.detail_testers_title
import test4test.shared.generated.resources.detail_verified
import test4test.shared.generated.resources.form_groups_url
import test4test.shared.generated.resources.form_name
import test4test.shared.generated.resources.form_opt_in_url
import test4test.shared.generated.resources.my_apps_add
import test4test.shared.generated.resources.profile_premium
import test4test.shared.generated.resources.round_progress
import test4test.shared.generated.resources.tab_my_apps
import test4test.shared.generated.resources.tab_profile

/**
 * Las capturas crudas de la ficha de Play: las pantallas reales de la app (App(), con su barra y sus pestanas)
 * contra datos inventados (DemoBackend), una por idioma. Salen en store/screenshots/raw/<idioma>/ y las enmarca,
 * con su titular, la plantilla de gk/contenido/plantilla/tienda (store/DISENO.md).
 * Sin backend de verdad: el cliente de Supabase usa un motor HTTP de mentira y cualquier peticion no prevista
 * hace fallar la prueba. Solo con las tareas de Roborazzi: ./gradlew :shared:recordRoborazziAndroidHostTest
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
abstract class StoreScreenshots(private val language: String) {

    @get:Rule(order = 0)
    val onlyWithRoborazzi = RoborazziOnly()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    private val outDir = File(checkNotNull(System.getProperty("user.dir"))).absoluteFile.parentFile!!.resolve("store/screenshots/raw/$language")

    @OptIn(SupabaseInternal::class)
    @Before
    fun signIn() {
        // Antes de tocar `supabase`, que se crea una sola vez y ya no cambia de motor.
        TestOverrides.engine = DemoBackend.engine
        TestOverrides.sessionManager = MemorySessionManager()
        TestOverrides.codeVerifierCache = MemoryCodeVerifierCache()
        DemoBackend.language = language
        DemoBackend.unexpected.clear()
        runBlocking {
            check(supabase.httpClient.httpClient.engine is MockEngine) { "El cliente de Supabase no usa el motor de demo" }
            supabase.auth.importSession(
                UserSession(
                    accessToken = "demo",
                    refreshToken = "demo",
                    expiresIn = 3600,
                    tokenType = "bearer",
                    user = UserInfo(aud = "authenticated", id = Demo.ME),
                    expiresAt = Clock.System.now() + 1.hours,
                ),
                autoRefresh = false,
            )
        }
    }

    // El gancho: el feed de quien llega y todavia no prueba nada.
    @Test
    fun feed() {
        start(Demo.newcomer)
        waitForText(Demo.pantry.name)
        shot("01-feed")
    }

    // La ronda de hoy, arriba del feed: 2 de 4 abiertas.
    @Test
    fun round() {
        start(Demo.inRound)
        waitForText(string(Res.string.round_progress, 2, 4))
        shot("02-round")
    }

    // La ficha de su propia app: los 12, el dia 9 de 14 y que hace cada tester con ella.
    @Test
    fun owner() {
        start(Demo.inRound)
        click(string(Res.string.tab_my_apps))
        waitForText(Demo.trail.name)
        click(Demo.trail.name)
        waitForText(string(Res.string.detail_testers_title))
        waitForText("Tom Becker")
        shot("03-owner")
    }

    // La ficha de una app ajena, bajada hasta sus comentarios.
    @Test
    fun comments() {
        start(Demo.inRound)
        waitForText(Demo.pantry.name)
        // Las que prueba quien mira bajan al final del feed: hay que llegar hasta su tarjeta, no hasta
        // su nombre en la ronda de arriba, que no se toca.
        val card = hasText(Demo.bird.name) and hasClickAction()
        scrollList().performScrollToNode(card)
        compose.onNode(card).performClick()
        // Los comentarios estan fuera de la pantalla y no se componen hasta bajar: se espera al principio de la ficha.
        waitForText(string(Res.string.detail_verified, Demo.bird.verifiedCount))
        // Hasta su titulo y no mas: sin tocar al lado de esas tarjetas de arriba (grupo, enlace, desinstalar).
        scrollList().performScrollToIndex(COMMENTS_INDEX)
        compose.waitForIdle()
        shot("04-comments")
    }

    // El alta de una app: nombre y dos enlaces de Play Console.
    @Test
    fun publish() {
        start(Demo.inRound)
        click(string(Res.string.tab_my_apps))
        click(string(Res.string.my_apps_add))
        waitForText(string(Res.string.form_name))
        // El nombre el ultimo: se queda con el foco, y los enlaces se leen desde el principio.
        for ((label, url) in listOf(
            string(Res.string.form_groups_url) to Demo.trail.googleGroupsUrl,
            string(Res.string.form_opt_in_url) to Demo.trail.optInUrl,
        )) {
            compose.onNodeWithText(label).performTextReplacement(url)
            // Un enlace largo se queda desplazado al final: se vuelve al principio, que es lo que se lee.
            compose.onNodeWithText(label).performKeyInput { pressKey(Key.MoveHome) }
        }
        compose.onNodeWithText(string(Res.string.form_name)).performTextReplacement(Demo.trail.name)
        shot("05-publish")
    }

    // El plan ilimitado, sin anuncios: tres apps publicadas y las que prueba.
    @Test
    fun profile() {
        start(Demo.inRound)
        click(string(Res.string.tab_profile))
        waitForText(string(Res.string.profile_premium))
        waitForText(Demo.pup.name)
        shot("06-profile")
    }

    private fun start(scenario: Scenario) {
        DemoBackend.scenario = scenario
        installTested(scenario)
        compose.setContent { App() }
    }

    // Lo que prueba quien mira esta instalado desde Play en su movil: "Abrir" y no "Instalar".
    private fun installTested(scenario: Scenario) {
        val shadow = shadowOf(ApplicationProvider.getApplicationContext<Application>().packageManager)
        for (app in Demo.apps.filter { it.id in scenario.testing }) {
            val pkg = checkNotNull(app.packageName)
            shadow.installPackage(PackageInfo().apply {
                packageName = pkg
                applicationInfo = ApplicationInfo().apply { packageName = pkg }
            })
            shadow.setInstallSourceInfo(pkg, PLAY_STORE, PLAY_STORE)
        }
    }

    // La lista de la pantalla, no las filas de chips que se desplazan de lado.
    private fun scrollList() = compose.onAllNodes(hasScrollAction()).onFirst()

    private fun string(res: StringResource, vararg args: Any) = runBlocking { getString(res, *args) }

    // Si no sale, lo que se pidio al servidor de demo y lo que hay en pantalla: sin eso un fallo no dice nada.
    private fun waitForText(text: String) = try {
        compose.waitUntil(20_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    } catch (e: ComposeTimeoutException) {
        throw AssertionError(
            "No sale \"$text\". Peticiones: ${DemoBackend.requests.takeLast(12)}\n${compose.onRoot().printToString(40)}", e,
        )
    }

    private fun click(text: String) {
        waitForText(text)
        compose.onNodeWithText(text).performClick()
    }

    private fun shot(name: String) {
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(1_000)
        // El motor se uso de verdad y no salio nada que no fuera una lectura prevista.
        assertTrue(DemoBackend.requests.isNotEmpty(), "Ninguna peticion paso por el motor de demo")
        assertEquals(emptyList(), DemoBackend.unexpected.toList(), "Peticiones sin prever")
        compose.onRoot().captureRoboImage(outDir.resolve("$name.png"))
    }

    private companion object {
        const val PLAY_STORE = "com.android.vending"

        // Posicion del titulo "Comentarios" en la lista de la ficha de una app ajena: cabecera, dueno, error,
        // pasos, enlaces, separador y titulo.
        const val COMMENTS_INDEX = 6
    }
}

/**
 * Las capturas solo se hacen con las tareas de Roborazzi (record, compare o verify): con
 * `./gradlew test` se saltan, para no alargar cada ejecucion. Tiene que ir antes que la regla
 * de Compose (`order = 0`), que abre una actividad al empezar.
 */
class RoborazziOnly : TestRule {
    override fun apply(base: Statement, description: Description) = object : Statement() {
        override fun evaluate() {
            assumeTrue(
                listOf("record", "compare", "verify").any { System.getProperty("roborazzi.test.$it") == "true" }
            )
            base.evaluate()
        }
    }
}

// 1080x2400 como un movil de gama media; el idioma lo pone el calificador.
private const val SCREEN = "w360dp-h800dp-port-xxhdpi"

@Config(sdk = [34], qualifiers = "en-rUS-$SCREEN")
class StoreEnTest : StoreScreenshots("en-US")

@Config(sdk = [34], qualifiers = "es-rES-$SCREEN")
class StoreEsTest : StoreScreenshots("es-ES")

@Config(sdk = [34], qualifiers = "pt-rBR-$SCREEN")
class StorePtTest : StoreScreenshots("pt-BR")

@Config(sdk = [34], qualifiers = "hi-rIN-$SCREEN")
class StoreHiTest : StoreScreenshots("hi-IN")

@Config(sdk = [34], qualifiers = "in-rID-$SCREEN")
class StoreIdTest : StoreScreenshots("id")
