package com.baltajmn.test4test

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.delay

private const val APPS = "apps"
// Vista de lectura: apps + follower_count. Para escribir se usa la tabla.
private const val FEED = "apps_with_followers"
private const val TESTERS = "app_testers"
private const val COMMENTS = "app_comments"
private const val PROFILES = "profiles"
private const val REPORTS = "app_reports"

val currentUserId: String? get() = supabase.auth.currentUserOrNull()?.id

suspend fun feedApps(uid: String): List<AppRow> =
    supabase.from(FEED).select {
        filter { neq("owner_id", uid) }
        // Dos niveles. Primero quien testea apps ajenas: sin esto el intercambio
        // se convierte en un tablon donde todos publican y nadie se une, que es
        // de lo que se mueren los grupos de testers. No se oculta a nadie, solo
        // se cobra el sitio de arriba.
        order("owner_reciprocates", Order.DESCENDING)
        // Y dentro de cada nivel, menos testers primero: reparte el esfuerzo.
        order("follower_count", Order.ASCENDING)
        order("created_at", Order.DESCENDING)
    }.decodeList()

suspend fun myApps(uid: String): List<AppRow> =
    supabase.from(FEED).select {
        filter { eq("owner_id", uid) }
        order("created_at", Order.DESCENDING)
    }.decodeList()

suspend fun appById(id: String): AppRow? =
    supabase.from(FEED).select { filter { eq("id", id) } }.decodeSingleOrNull()

suspend fun appsByIds(ids: List<String>): List<AppRow> =
    if (ids.isEmpty()) emptyList()
    else supabase.from(FEED).select { filter { isIn("id", ids) } }.decodeList()

suspend fun createApp(input: AppInput) {
    supabase.from(APPS).insert(input)
}

suspend fun updateApp(id: String, input: AppInput) {
    supabase.from(APPS).update(input) { filter { eq("id", id) } }
}

// Sirve al dueno y al admin: quien puede borrar lo decide la RLS, no el cliente.
suspend fun deleteApp(id: String) {
    supabase.from(APPS).delete { filter { eq("id", id) } }
}

// Ids de las apps en las que el usuario es tester. Un solo viaje, y vale
// tanto para pintar el boton del detalle como la lista del perfil.
suspend fun testerAppIds(uid: String): Set<String> =
    supabase.from(TESTERS).select { filter { eq("user_id", uid) } }
        .decodeList<TesterRow>().map { it.appId }.toSet()

suspend fun joinApp(appId: String, uid: String) {
    supabase.from(TESTERS).insert(TesterRow(appId, uid))
}

suspend fun leaveApp(appId: String, uid: String) {
    supabase.from(TESTERS).delete {
        filter {
            eq("app_id", appId)
            eq("user_id", uid)
        }
    }
}

suspend fun profile(uid: String): Profile? =
    supabase.from(PROFILES).select { filter { eq("id", uid) } }.decodeSingleOrNull()

// El cliente nunca escribe is_premium: lo hace el webhook de RevenueCat contra
// Supabase (issue #25), asi que tras comprar el cambio tarda unos segundos en
// aparecer. Se reintenta en vez de darlo por hecho, porque la policy
// apps_insert_own_within_slots solo deja crear la segunda app cuando la fila ya
// dice true. Devuelve null si no llega a activarse.
suspend fun awaitPremium(uid: String, attempts: Int = 6): Profile? {
    repeat(attempts) { attempt ->
        if (attempt > 0) delay(2_000)
        val me = runCatching { profile(uid) }.getOrNull()
        if (me?.isPremium == true) return me
    }
    return null
}

// Sin embeds de PostgREST a proposito: app_comments tiene dos FK contra apps
// (app_id y linked_app_id), asi que el embed necesitaria hints por nombre de
// constraint. Dos queries mas y el join se hace aqui.
suspend fun commentsFor(appId: String): List<CommentView> {
    val rows = supabase.from(COMMENTS).select {
        filter { eq("app_id", appId) }
        order("created_at", Order.DESCENDING)
    }.decodeList<CommentRow>()
    if (rows.isEmpty()) return emptyList()

    val authors = supabase.from(PROFILES)
        .select { filter { isIn("id", rows.map { it.authorId }.distinct()) } }
        .decodeList<Profile>()
        .associateBy { it.id }
    val linked = appsByIds(rows.mapNotNull { it.linkedAppId }.distinct()).associateBy { it.id }

    return rows.map { row ->
        CommentView(
            comment = row,
            // Puede faltar: el nombre de repuesto lo pone la UI, que es la que
            // sabe el idioma.
            author = authors[row.authorId],
            linkedApp = row.linkedAppId?.let(linked::get),
        )
    }
}

suspend fun postComment(input: CommentInput) {
    supabase.from(COMMENTS).insert(input)
}

// Solo escribe. Leerlas es cosa del admin, que ya tiene el boton de borrar; la
// policy app_reports_select_admin no deja verlas a nadie mas.
suspend fun report(input: ReportInput) {
    supabase.from(REPORTS).insert(input)
}

suspend fun deleteComment(id: String) {
    supabase.from(COMMENTS).delete { filter { eq("id", id) } }
}
