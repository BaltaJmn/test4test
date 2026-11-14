package com.baltajmn.test4test

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.baltajmn.test4test.shared.R
import com.google.android.play.core.review.ReviewManagerFactory
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import test4test.shared.generated.resources.Res
import test4test.shared.generated.resources.joined_text
import test4test.shared.generated.resources.joined_title
import test4test.shared.generated.resources.nudge_text
import test4test.shared.generated.resources.nudge_title
import test4test.shared.generated.resources.reminder_text
import test4test.shared.generated.resources.round_title

private const val PREFS = "round"
private const val KEY_ON = "reminder"
private const val KEY_NUDGED = "nudged"
private const val KEY_REVIEW = "review"
// Testers de cada app propia la ultima vez que se miro: "f_<id>".
private const val KEY_FOLLOWERS = "f_"
private const val WORK = "daily-round"
private const val CHANNEL = "round"
private const val DAY_MS = 86_400_000L
private const val REVIEW_EVERY_DAYS = 30
// ponytail: trabajo periodico de 24 h, deriva unos minutos al dia; si importa la
// hora exacta, AlarmManager con reprogramacion diaria.
private const val HOUR = 18

private fun Context.prefs() = getSharedPreferences(PREFS, Context.MODE_PRIVATE)

private fun schedule(context: Context) {
    val now = Calendar.getInstance()
    val next = (now.clone() as Calendar).apply {
        set(Calendar.HOUR_OF_DAY, HOUR)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        if (!after(now)) add(Calendar.DAY_OF_MONTH, 1)
    }
    val request = PeriodicWorkRequestBuilder<RoundWorker>(1, TimeUnit.DAYS)
        .setInitialDelay(next.timeInMillis - now.timeInMillis, TimeUnit.MILLISECONDS)
        // El aviso lee la ronda del servidor: sin red espera a tenerla.
        .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
        .build()
    WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.UPDATE, request)
}

private fun Context.canNotify() = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
    ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

@Composable
actual fun rememberDailyReminder(auto: Boolean): DailyReminder {
    val context = LocalContext.current.applicationContext
    var on by remember { mutableStateOf(context.prefs().getBoolean(KEY_ON, false)) }
    fun set(value: Boolean) {
        context.prefs().edit().putBoolean(KEY_ON, value).apply()
        on = value
        if (value) schedule(context) else WorkManager.getInstance(context).cancelUniqueWork(WORK)
    }
    // Android 13+ pide el permiso; si lo niega, el aviso se queda apagado.
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) set(true)
    }
    // Primera vez (nunca se ha elegido nada): encendido sin que nadie lo busque.
    // Apagado queda apuntado antes de preguntar, asi el permiso se pide una sola
    // vez. En las pruebas de capturas no hay a quien preguntar.
    LaunchedEffect(Unit) {
        if (!auto || TestOverrides.engine != null || context.prefs().contains(KEY_ON)) return@LaunchedEffect
        if (context.canNotify()) set(true) else {
            set(false)
            permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    return DailyReminder(
        on = on,
        toggle = {
            when {
                on -> set(false)
                context.canNotify() -> set(true)
                else -> permission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        },
    )
}

@Composable
actual fun rememberAskReview(): () -> Unit {
    val activity = LocalActivity.current
    return remember(activity) {
        {
            val prefs = activity?.prefs()
            val now = System.currentTimeMillis()
            if (activity != null && prefs != null && now - prefs.getLong(KEY_REVIEW, 0) > REVIEW_EVERY_DAYS * DAY_MS) {
                prefs.edit().putLong(KEY_REVIEW, now).apply()
                val manager = ReviewManagerFactory.create(activity)
                manager.requestReviewFlow().addOnCompleteListener { task ->
                    // Play decide si la ensena y no dice si lo hizo: no hay resultado que mirar.
                    if (task.isSuccessful) runCatching { manager.launchReviewFlow(activity, task.result) }
                }
            }
        }
    }
}

class RoundWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result =
        if (runDailyReminder(applicationContext) || runAttemptCount >= 3) Result.success() else Result.retry()
}

// El trabajo del aviso, fuera del Worker para poder probarlo sin WorkManager.
// false = no se pudo leer del servidor y merece otro intento.
internal suspend fun runDailyReminder(context: Context): Boolean {
    if (!context.canNotify()) return true
    // La sesion guardada tarda un momento en restaurarse; sin sesion no hay a quien avisar.
    val uid = withTimeoutOrNull(20_000) {
        supabase.auth.sessionStatus.first { it !is SessionStatus.Initializing }
        currentUserId
    } ?: return true
    val news = runCatching { dailyNews(uid) }.getOrElse { return false }
    val prefs = context.prefs()
    val today = System.currentTimeMillis() / DAY_MS
    // Nombre del canal en los ajustes del sistema.
    val channel = getString(Res.string.round_title)

    when (dailyNudge(news.testing, news.pending, news.waiting, today - prefs.getLong(KEY_NUDGED, 0))) {
        DailyNudge.ROUND -> context.post(
            channel, 1, channel,
            getPluralString(Res.plurals.reminder_text, news.pending, news.pending),
        )
        DailyNudge.JOIN -> {
            prefs.edit().putLong(KEY_NUDGED, today).apply()
            context.post(
                channel, 1, getString(Res.string.nudge_title),
                getPluralString(Res.plurals.nudge_text, news.waiting, news.waiting),
            )
        }
        DailyNudge.NONE -> {}
    }

    // Tester nuevo en una app propia desde la ultima vez. La primera pasada
    // solo apunta: sin recuento anterior no se sabe si alguno es nuevo.
    val edit = prefs.edit()
    for (app in news.mine) {
        val key = KEY_FOLLOWERS + app.id
        val before = prefs.getInt(key, -1)
        val count = app.followerCount.toInt()
        if (before in 0 until count) {
            context.post(channel, 2, getString(Res.string.joined_title, app.name), getString(Res.string.joined_text, count))
        }
        edit.putInt(key, count)
    }
    edit.apply()
    return true
}

private fun Context.post(channel: String, id: Int, title: String, text: String) {
    val manager = NotificationManagerCompat.from(this)
    manager.createNotificationChannel(
        NotificationChannelCompat.Builder(CHANNEL, NotificationManagerCompat.IMPORTANCE_DEFAULT)
            .setName(channel).build()
    )
    val open = packageManager.getLaunchIntentForPackage(packageName)?.let {
        PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }
    val notification = NotificationCompat.Builder(this, CHANNEL)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(title)
        .setContentText(text)
        .setStyle(NotificationCompat.BigTextStyle().bigText(text))
        .setContentIntent(open)
        .setAutoCancel(true)
        .build()
    // canNotify() ya lo ha mirado; el try es por si retiran el permiso entre medias.
    try {
        manager.notify(id, notification)
    } catch (_: SecurityException) {
    }
}
