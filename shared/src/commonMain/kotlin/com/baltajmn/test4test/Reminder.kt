package com.baltajmn.test4test

import androidx.compose.runtime.Composable

// Aviso diario, por la tarde: la ronda sin hacer, las apps que esperan testers
// (a quien no prueba ninguna) y el tester nuevo en tu app. Lo decide dailyNudge
// con datos leidos en el momento. Con `auto` se enciende solo la primera vez,
// pidiendo el permiso si hace falta; despues manda lo que el usuario elija.
class DailyReminder(val on: Boolean, val toggle: () -> Unit)

@Composable
expect fun rememberDailyReminder(auto: Boolean = false): DailyReminder

// Pide la valoracion de Play, como mucho una vez al mes y solo si Play quiere.
@Composable
expect fun rememberAskReview(): () -> Unit
