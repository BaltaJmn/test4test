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
)

// Insert/update: sin id ni created_at, que los pone Postgres.
@Serializable
data class AppInput(
    @SerialName("owner_id") val ownerId: String,
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

// Comentario ya resuelto para pintar: autor y app vinculada en el mismo objeto.
data class CommentView(
    val comment: CommentRow,
    val authorName: String,
    val linkedApp: AppRow?,
)
