package com.baltajmn.test4test

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Se lee de la vista apps_with_followers: apps + su contador de testers en vivo.
@Serializable
data class AppRow(
    val id: String,
    @SerialName("owner_id") val ownerId: String,
    val name: String,
    @SerialName("google_groups_url") val googleGroupsUrl: String,
    @SerialName("play_store_url") val playStoreUrl: String,
    @SerialName("opt_in_url") val optInUrl: String,
    @SerialName("created_at") val createdAt: String? = null,
    // Solo vienen de la vista; al leer la tabla apps directamente no existen.
    @SerialName("follower_count") val followerCount: Long = 0,
    // Dia en curso de la racha con 12 testers, 0 si todavia no ha llegado. Lo
    // calcula la vista: el cliente no lleva libreria de fechas.
    @SerialName("full_days") val fullDays: Int = 0,
    // Reciprocidad del dueno: apps ajenas que testea y cuantas se le piden. La
    // vista las trae ya resueltas para que el feed pueda ordenar por ellas sin
    // una segunda consulta por cada fila.
    @SerialName("owner_testing_count") val ownerTestingCount: Int = 0,
    @SerialName("owner_testing_required") val ownerTestingRequired: Int = 0,
    @SerialName("owner_reciprocates") val ownerReciprocates: Boolean = true,
    // Paquete que sale del enlace de opt-in: es lo que se busca instalado en el
    // movil del tester. Lo deriva Postgres, asi que no puede no cuadrar.
    @SerialName("package_name") val packageName: String? = null,
    // Testers vistos con la app instalada en los ultimos tres dias.
    @SerialName("verified_count") val verifiedCount: Long = 0,
)

// Insert/update: sin id, created_at ni owner_id, que los pone Postgres. owner_id
// no puede viajar ni en la edicion: el GRANT de UPDATE es por columnas y lo deja
// fuera, asi que una edicion que lo llevara se rechazaria entera.
@Serializable
data class AppInput(
    val name: String,
    @SerialName("google_groups_url") val googleGroupsUrl: String,
    @SerialName("play_store_url") val playStoreUrl: String,
    @SerialName("opt_in_url") val optInUrl: String,
)

@Serializable
data class CommentRow(
    val id: String,
    @SerialName("app_id") val appId: String,
    @SerialName("author_id") val authorId: String,
    val body: String,
    @SerialName("linked_app_id") val linkedAppId: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class CommentInput(
    @SerialName("app_id") val appId: String,
    @SerialName("author_id") val authorId: String,
    val body: String,
    @SerialName("linked_app_id") val linkedAppId: String? = null,
)

@Serializable
data class Profile(
    val id: String,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("is_premium") val isPremium: Boolean = false,
    @SerialName("is_admin") val isAdmin: Boolean = false,
)

// Denuncia de contenido de usuario. Uno de los dos ids va relleno y el otro no,
// que es lo que comprueba el CHECK app_reports_target.
@Serializable
data class ReportInput(
    @SerialName("reporter_id") val reporterId: String,
    @SerialName("app_id") val appId: String? = null,
    @SerialName("comment_id") val commentId: String? = null,
)

@Serializable
data class TesterRow(
    @SerialName("app_id") val appId: String,
    @SerialName("user_id") val userId: String,
)

// Fila de app_tester_status: quien entro y si su movil confirmo la instalacion.
@Serializable
data class TesterStatusRow(
    @SerialName("user_id") val userId: String,
    @SerialName("verified_at") val verifiedAt: String? = null,
    @SerialName("installed_recently") val installedRecently: Boolean? = null,
)

enum class InstallState { INSTALLED, STALE, UNVERIFIED }

// Lo que ve el dueno de cada tester: la persona y si de verdad tiene la app.
data class TesterView(val profile: Profile, val install: InstallState)

// Comentario ya resuelto para pintar: autor y app vinculada en el mismo objeto.
// El autor va entero y no solo su nombre: la ficha tambien pinta su foto y lleva
// a su perfil, y el perfil ya venia en el mismo fetch.
data class CommentView(
    val comment: CommentRow,
    val author: Profile?,
    val linkedApp: AppRow?,
)
