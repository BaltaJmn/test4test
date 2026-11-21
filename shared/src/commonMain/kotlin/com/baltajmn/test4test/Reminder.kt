package com.baltajmn.test4test

import androidx.compose.runtime.Composable

// Aviso diario, por la tarde: la ronda sin hacer, las apps que esperan testers
// (a quien no prueba ninguna) y el tester nuevo en tu app. Lo decide dailyNudge
// con datos leidos en el momento. Con `auto` se enciende solo la primera vez,
// pidiendo el permiso si hace falta; despues manda lo que el usuario elija.
class DailyReminder(val on: Boolean, val toggle: () -> Unit)

@Composable
expect fun rememberDailyReminder(auto: Boolean = false): DailyReminder

// Marca de la sesion: algo de Pro, el consentimiento de anuncios o un fallo ya se
// ha visto en esta ejecucion y la valoracion no se pide (V3, M7). Solo sube.
object Session {
    var busy = false

    // El toque de Pro de Mis apps (M2, M3): que tipo toco en esta ejecucion, null si aun
    // no se ha decidido, y si ya se cerro. Dura una sola sesion: se decide una vez.
    var proTouch: ProTouch? = null
    var proTouchClosed = false
}

// La linea de "saltar la cola" de Mis apps, con las reglas de `proTouchDue` y lo que
// se recuerda de una ejecucion a otra (cuando salio por primera vez, en que version
// se recordo, y el dia de la ultima peticion). `offer`: hay algo que ofrecer.
class ProTouchState(val visible: Boolean, val close: () -> Unit)

@Composable
expect fun rememberProTouch(offer: Boolean): ProTouchState

// Pide la valoracion de Play con las condiciones de `reviewDue`, y solo si Play quiere.
@Composable
expect fun rememberAskReview(): () -> Unit

// Abre la pagina de resena de Play desde la fila "Valorar" del perfil (V5).
const val REVIEW_URL = "https://play.google.com/store/apps/details?id=com.baltajmn.test4test&showAllReviews=true"
