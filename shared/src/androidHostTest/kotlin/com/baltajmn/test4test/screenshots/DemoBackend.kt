package com.baltajmn.test4test.screenshots

import com.baltajmn.test4test.AppRow
import com.baltajmn.test4test.CommentRow
import com.baltajmn.test4test.Profile
import com.baltajmn.test4test.TesterRow
import com.baltajmn.test4test.TesterStatusRow
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.http.headersOf
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

// Datos inventados para las capturas de la ficha de Play. Nada de esto existe: ni las
// apps, ni los paquetes, ni las personas. Se sirven por un motor HTTP de mentira que
// el cliente de Supabase usa en lugar de la red (HttpEngineOverride), asi que las
// pantallas que se pintan son las reales, con sus consultas reales, y nada sale del
// proceso: cualquier peticion que no sea una lectura prevista hace fallar la prueba.
//
// Las cifras se reparten una sola vez, aqui, para que la misma app diga lo mismo en
// todas las capturas: Trail Journal lleva 12 de 12 y el dia 9 de 14, Pantry Pal 3 de 12...

/** Que prueba quien mira la app y que abrio ya hoy. */
data class Scenario(val testing: List<String> = emptyList(), val openedToday: Set<String> = emptySet())

object Demo {
    const val ME = "u-me"

    // La app propia con la que se enseña la ficha del dueño, y la ajena de los comentarios.
    const val TRAIL = "a-trail"
    const val BIRD = "a-bird"

    private fun person(id: String, name: String, premium: Boolean = false) = Profile(id, name, null, premium, false)

    val me = person(ME, "Noa Ferreira", premium = true)
    private val rafa = person("u-rafa", "Rafa Souza")
    private val priya = person("u-priya", "Priya Nair")
    private val lena = person("u-lena", "Lena Hoffmann")
    private val budi = person("u-budi", "Budi Santoso")
    private val ivan = person("u-ivan", "Ivan Ruiz")
    private val carla = person("u-carla", "Carla Mendes")
    private val aarav = person("u-aarav", "Aarav Mehta")
    private val tom = person("u-tom", "Tom Becker")
    private val mei = person("u-mei", "Mei Tan")
    private val sofia = person("u-sofia", "Sofia Lopez")
    private val omar = person("u-omar", "Omar Haddad")
    private val jess = person("u-jess", "Jess Miller")
    private val ana = person("u-ana", "Ana Costa")
    private val kenji = person("u-kenji", "Kenji Mori")

    val profiles: List<Profile> =
        listOf(me, rafa, priya, lena, budi, ivan, carla, aarav, tom, mei, sofia, omar, jess, ana, kenji)

    private fun app(
        id: String,
        owner: Profile,
        name: String,
        slug: String,
        testers: Long,
        fullDays: Int = 0,
        installed: Long,
        ownerTesting: Int,
        created: String,
        // El paquete es de mentira. El de Trail Journal es el que se ve en el formulario, donde el campo del enlace es
        // mas corto que su texto: con "ru" el recorte cae justo despues de una letra entera (con "com" o "eu" parte una).
        pkg: String = "com.example.$slug",
    ): AppRow {
        return AppRow(
            id = id,
            ownerId = owner.id,
            name = name,
            googleGroupsUrl = "https://groups.google.com/g/$slug",
            playStoreUrl = "https://play.google.com/store/apps/details?id=$pkg",
            optInUrl = "https://play.google.com/apps/testing/$pkg",
            createdAt = created,
            followerCount = testers,
            fullDays = fullDays,
            ownerTestingCount = ownerTesting,
            ownerTestingRequired = 2,
            ownerReciprocates = true,
            packageName = pkg,
            verifiedCount = installed,
        )
    }

    // De otros desarrolladores, en el orden del feed: quien prueba a otros primero (todos
    // lo hacen) y dentro, los que menos testers tienen.
    val pantry = app("a-pantry", rafa, "Pantry Pal", "pantrypal", 3, installed = 2, ownerTesting = 3, created = "2026-11-04")
    val bird = app(BIRD, priya, "Bird Log", "birdlog", 5, installed = 4, ownerTesting = 4, created = "2026-11-02")
    val dumbbell = app("a-dumbbell", aarav, "Daily Dumbbell", "dailydumbbell", 6, installed = 5, ownerTesting = 2, created = "2026-10-30")
    val moss = app("a-moss", lena, "Moss Habits", "mosshabits", 7, installed = 6, ownerTesting = 5, created = "2026-10-27")
    val budget = app("a-budget", budi, "Tidy Budget", "tidybudget", 8, installed = 7, ownerTesting = 3, created = "2026-10-25")
    val chess = app("a-chess", ivan, "Pocket Chess Clock", "pocketchessclock", 10, installed = 9, ownerTesting = 4, created = "2026-10-20")
    val sky = app("a-sky", carla, "Sky Tonight", "skytonight", 12, fullDays = 4, installed = 11, ownerTesting = 6, created = "2026-10-12")

    // Las del que mira (plan ilimitado): Trail Journal con los 12 y el dia 9 de 14.
    val trail = app(TRAIL, me, "Trail Journal", "trailjournal", 12, fullDays = 9, installed = 10, ownerTesting = 4, created = "2026-10-29", pkg = "ru.tj.trail")
    val dune = app("a-dune", me, "Dune Dash", "dunedash", 6, installed = 5, ownerTesting = 4, created = "2026-10-22")
    val pup = app("a-pup", me, "Pixel Pup", "pixelpup", 2, installed = 2, ownerTesting = 4, created = "2026-10-15")

    val apps: List<AppRow> = listOf(pantry, bird, dumbbell, moss, budget, chess, sky, trail, dune, pup)

    // Las cuatro que prueba el que mira, en el orden de su ronda de hoy.
    val tested: List<AppRow> = listOf(bird, moss, budget, chess)

    // Quien mira ya abrio Bird Log y Pocket Chess Clock hoy: 2 de 4.
    val inRound = Scenario(testing = tested.map { it.id }, openedToday = setOf(BIRD, chess.id))
    val newcomer = Scenario()

    // Los 12 de Trail Journal, el ultimo en entrar primero (como los ordena la consulta).
    // Uno hace dias que no se ve con la app y otro no la ha abierto nunca desde Test4Test.
    private enum class Install { OK, STALE, NEVER }

    private val trailTesters = listOf(
        Triple(tom, Install.OK, 9), Triple(aarav, Install.OK, 9), Triple(mei, Install.STALE, 3),
        Triple(sofia, Install.OK, 8), Triple(omar, Install.OK, 9), Triple(jess, Install.NEVER, 0),
        Triple(ana, Install.OK, 9), Triple(kenji, Install.OK, 7), Triple(rafa, Install.OK, 9),
        Triple(priya, Install.OK, 8), Triple(lena, Install.OK, 9), Triple(budi, Install.OK, 6),
    ).map { (who, install, days) ->
        TesterStatusRow(
            appId = TRAIL,
            userId = who.id,
            verifiedAt = if (install == Install.NEVER) null else "2026-11-05T09:00:00Z",
            installedRecently = install == Install.OK,
            openedToday = install == Install.OK && days >= 8,
            openDays = days,
        )
    }

    fun ownerRows(): List<TesterStatusRow> = trailTesters

    fun viewerRows(scenario: Scenario): List<TesterStatusRow> = scenario.testing.map {
        TesterStatusRow(appId = it, userId = ME, openedToday = it in scenario.openedToday, openDays = 5)
    }

    // Los comentarios de Bird Log, el mas nuevo primero, en el idioma de la captura: la dueña da la
    // bienvenida y tres testers dicen que estan dentro, cada uno enlazando su app para que se la devuelvan.
    fun comments(language: String): List<CommentRow> {
        val t = COMMENTS.getValue(language)
        return listOf(
            CommentRow("c4", BIRD, aarav.id, t[2], dumbbell.id, "2026-11-06T08:00:00Z"),
            CommentRow("c3", BIRD, lena.id, t[1], moss.id, "2026-11-05T19:00:00Z"),
            CommentRow("c2", BIRD, budi.id, t[0], budget.id, "2026-11-05T11:00:00Z"),
            CommentRow("c1", BIRD, priya.id, t[3], null, "2026-11-04T17:00:00Z"),
        )
    }

    // [Budi, Lena, Aarav, Priya (la bienvenida)]. Escritos para cada idioma, no traducidos a la letra.
    private val COMMENTS: Map<String, List<String>> = mapOf(
        "en-US" to listOf(
            "Joined the group and installed it from Play. Mine still needs a few testers, thanks!",
            "Smooth onboarding, nice work. I'm in, and I'll open it every day.",
            "One small thing: the search ignores accents. The rest is great. Mine is linked if you want to test it back.",
            "Welcome! Open the group first, then the opt-in link. Leave your app here and I will test it back.",
        ),
        "es-ES" to listOf(
            "Ya estoy en el grupo y la he instalado desde Play. La mía aún necesita testers, ¡gracias!",
            "Buen primer arranque, enhorabuena. Estoy dentro y la abriré cada día.",
            "Un detalle: el buscador ignora los acentos. Lo demás, genial. Te enlazo la mía por si la pruebas.",
            "¡Bienvenido! Abre primero el grupo y luego el enlace de opt-in. Deja aquí tu app y yo pruebo la tuya.",
        ),
        "pt-BR" to listOf(
            "Entrei no grupo e instalei pelo Play. O meu ainda precisa de testers, obrigado!",
            "Primeiro uso bem fluido, parabéns. Estou dentro e vou abrir todo dia.",
            "Um detalhe: a busca ignora os acentos. O resto está ótimo. Deixei o meu vinculado para você testar também.",
            "Bem-vindo! Abra primeiro o grupo e depois o link de participação. Deixe seu app aqui e eu testo o seu.",
        ),
        "id" to listOf(
            "Sudah gabung grup dan pasang dari Play. Punyaku masih butuh tester, terima kasih!",
            "Onboarding-nya mulus, bagus. Aku ikut dan akan membukanya tiap hari.",
            "Satu hal kecil: pencariannya mengabaikan aksen. Selebihnya mantap. Punyaku kutautkan, boleh dites balik.",
            "Selamat datang! Buka grupnya dulu, lalu tautan opt-in. Tinggalkan aplikasimu di sini, nanti kuuji balik.",
        ),
        "hi-IN" to listOf(
            "ग्रुप जॉइन कर लिया और Play से इंस्टॉल भी कर लिया। मेरे ऐप को अभी कुछ टेस्टर चाहिए, धन्यवाद!",
            "ऑनबोर्डिंग बहुत स्मूद है, बढ़िया काम। मैं टेस्ट में हूँ और रोज़ ऐप खोलूँगी।",
            "एक छोटी बात: सर्च में एक्सेंट वाले अक्षर नहीं पकड़ता। बाकी सब बढ़िया। मेरा ऐप लिंक किया है, चाहें तो टेस्ट कर दें।",
            "स्वागत है! पहले ग्रुप खोलें, फिर ऑप्ट-इन लिंक। अपना ऐप यहाँ छोड़ें, मैं भी उसे टेस्ट करूँगा।",
        ),
    )
}

/** El motor HTTP de mentira. Una sola instancia por proceso: el cliente de Supabase se crea una vez. */
object DemoBackend {
    @Volatile var language: String = "en-US"
    @Volatile var scenario: Scenario = Demo.newcomer

    /** Todas las peticiones, para comprobar que el motor se uso de verdad. */
    val requests = CopyOnWriteArrayList<String>()

    /** Lo que no es una lectura prevista. La prueba falla si no esta vacia. */
    val unexpected = CopyOnWriteArrayList<String>()

    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }

    val engine = MockEngine { request -> handle(request) }

    private fun <T> rows(serializer: KSerializer<T>, list: List<T>): List<JsonObject> =
        json.encodeToJsonElement(ListSerializer(serializer), list).let { (it as JsonArray).map { e -> e as JsonObject } }

    private fun MockRequestHandleScope.handle(request: HttpRequestData): HttpResponseData {
        val url = request.url
        val path = url.encodedPath
        requests += "${request.method.value} $path?${url.encodedQuery}"
        // Marcar como instaladas las apps del movil es lo unico que escribe la app sin que
        // nadie toque nada; aqui no llega a ningun sitio.
        if (request.method == HttpMethod.Post && path.endsWith("/rpc/confirm_installed")) {
            return respond("", HttpStatusCode.NoContent)
        }
        if (request.method != HttpMethod.Get || !path.startsWith("/rest/v1/")) {
            unexpected += "${request.method.value} $path"
            return respondError(HttpStatusCode.Forbidden)
        }
        val params = url.parameters
        val table = path.removePrefix("/rest/v1/")
        val all: List<JsonObject> = when (table) {
            "apps_with_followers" -> {
                // Feed: lo que no es mio, quien prueba a otros primero y los de menos testers antes.
                // Mis apps: las mas nuevas primero.
                val sorted = if (params["owner_id"]?.startsWith("neq.") == true) {
                    Demo.apps.sortedWith(compareByDescending<AppRow> { it.ownerReciprocates }.thenBy { it.followerCount })
                } else {
                    Demo.apps.sortedByDescending { it.createdAt }
                }
                rows(AppRow.serializer(), sorted)
            }
            "app_testers" -> rows(TesterRow.serializer(), scenario.testing.map { TesterRow(it, Demo.ME) })
            "app_tester_status" ->
                if (params["app_id"] != null) rows(TesterStatusRow.serializer(), Demo.ownerRows())
                else rows(TesterStatusRow.serializer(), Demo.viewerRows(scenario))
            "profiles" -> rows(Profile.serializer(), Demo.profiles)
            "app_comments" -> rows(CommentRow.serializer(), Demo.comments(language))
            else -> {
                unexpected += "GET $path"
                return respondError(HttpStatusCode.NotFound)
            }
        }
        val found = all.filter { matches(it, params) }
        val wantsObject = request.headers[HttpHeaders.Accept]?.contains("vnd.pgrst.object") == true
        val body: JsonElement = if (wantsObject) found.singleOrNull() ?: return respondError(HttpStatusCode.NotAcceptable) else JsonArray(found)
        return respond(
            content = body.toString(),
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, "application/json"),
        )
    }

    // Solo los filtros que usa la app: eq, neq e in.
    private fun matches(row: JsonObject, params: Parameters): Boolean =
        params.entries().filter { it.key !in RESERVED }.all { (column, values) ->
            val field = row[column]?.jsonPrimitive?.contentOrNull
            values.all { v ->
                when {
                    v.startsWith("eq.") -> field == v.removePrefix("eq.")
                    v.startsWith("neq.") -> field != v.removePrefix("neq.")
                    v.startsWith("in.(") ->
                        field in v.removePrefix("in.(").removeSuffix(")").split(",").map { it.trim().trim('"') }
                    else -> error("Filtro sin prever: $column=$v")
                }
            }
        }

    private val RESERVED = setOf("select", "order", "limit", "offset")
}
