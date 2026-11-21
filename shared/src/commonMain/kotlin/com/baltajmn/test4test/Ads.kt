package com.baltajmn.test4test

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// Banner de AdMob del plan free, encima de las pestanas. Quien lo ve lo decide
// showsAds; montarlo es lo que pide el consentimiento (UMP), asi que quien paga
// nunca ve ni el formulario.
@Composable
expect fun AdBanner(modifier: Modifier = Modifier)

// Si toca el banner para este perfil: showsAds con lo que solo sabe el sistema (cuando
// se instalo la app) y lo que dice config.json de la web, que se vuelve a leer cada
// vez que se vuelve a la app.
@Composable
expect fun rememberShowsAds(me: Profile?): Boolean

// Abre el formulario para cambiar el consentimiento de los anuncios. Null donde
// la ley no lo pide (fuera del EEE y Reino Unido): ahi no se pinta la opcion.
@Composable
expect fun rememberAdPrivacyOptions(): (() -> Unit)?
