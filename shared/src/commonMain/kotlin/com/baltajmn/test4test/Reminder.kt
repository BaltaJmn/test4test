package com.baltajmn.test4test

import androidx.compose.runtime.Composable

// Aviso diario de la ronda, por la tarde, solo si el usuario lo activa y solo
// si ese dia no la ha hecho. saveRound le deja apuntado lo que necesita saber
// sin red ni sesion: cuantas apps pruebas y cuantas abriste hoy.
class DailyReminder(
    val on: Boolean,
    val toggle: () -> Unit,
    val saveRound: (testing: Int, opened: Int) -> Unit,
)

@Composable
expect fun rememberDailyReminder(): DailyReminder
