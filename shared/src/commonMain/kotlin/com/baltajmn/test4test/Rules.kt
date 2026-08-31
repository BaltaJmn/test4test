package com.baltajmn.test4test

// Plan free: 1 app por usuario. La misma condicion vive en la policy
// apps_insert_own_within_slots; aqui solo se replica para no llevar al usuario
// a un formulario que el backend va a rechazar.
fun canCreateApp(isPremium: Boolean, ownedApps: Int): Boolean = isPremium || ownedApps < 1

// Validacion de cliente del alta de app (issue #15): evita el viaje al backend.
// Los mismos limites estan como CHECK en la tabla apps.
fun appFormError(
    name: String,
    googleGroupsUrl: String,
    playStoreUrl: String,
    optInUrl: String,
): String? = when {
    name.trim().isEmpty() -> "El nombre no puede estar vacio"
    name.trim().length > 100 -> "El nombre no puede pasar de 100 caracteres"
    !googleGroupsUrl.startsWith("https://") -> "El enlace de Google Groups tiene que empezar por https://"
    !playStoreUrl.startsWith("https://") -> "El enlace de Play Store tiene que empezar por https://"
    !optInUrl.startsWith("https://") -> "El enlace de opt-in tiene que empezar por https://"
    else -> null
}
