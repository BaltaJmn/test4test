package com.baltajmn.test4test

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.baltajmn.test4test.shared.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import test4test.shared.generated.resources.Res
import test4test.shared.generated.resources.reminder_text
import test4test.shared.generated.resources.round_title

private const val PREFS = "round"
private const val KEY_ON = "reminder"
private const val KEY_TESTING = "testing"
private const val KEY_OPENED = "opened"
private const val KEY_DAY = "day"
private const val WORK = "daily-round"
private const val CHANNEL = "round"
// ponytail: trabajo periodico de 24 h, deriva unos minutos al dia; si importa la
// hora exacta, AlarmManager con reprogramacion diaria.
private const val HOUR = 18

// Dia local: el aviso va con el reloj del movil, no con el del servidor.
private fun today() = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

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
        .build()
    WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.UPDATE, request)
}

private fun Context.canNotify() = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
    ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

@Composable
actual fun rememberDailyReminder(): DailyReminder {
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
    return DailyReminder(
        on = on,
        toggle = {
            when {
                on -> set(false)
                context.canNotify() -> set(true)
                else -> permission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        },
        saveRound = { testing, opened ->
            context.prefs().edit().putInt(KEY_TESTING, testing).putInt(KEY_OPENED, opened)
                .putString(KEY_DAY, today()).apply()
        },
    )
}

class RoundWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val context = applicationContext
        val prefs = context.prefs()
        // Lo apuntado otro dia no vale para hoy: sin abrir Test4Test, toca la ronda entera.
        val testing = prefs.getInt(KEY_TESTING, 0)
        val pending = testing - if (prefs.getString(KEY_DAY, null) == today()) prefs.getInt(KEY_OPENED, 0) else 0
        if (pending <= 0 || !context.canNotify()) return Result.success()
        val manager = NotificationManagerCompat.from(context)
        val title = getString(Res.string.round_title)
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL, NotificationManagerCompat.IMPORTANCE_DEFAULT).setName(title).build()
        )
        val open = PendingIntent.getActivity(
            context, 0, context.packageManager.getLaunchIntentForPackage(context.packageName),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(getPluralString(Res.plurals.reminder_text, pending, pending))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        // canNotify() ya lo ha mirado; el try es por si lo retiran entre medias.
        try {
            manager.notify(1, notification)
        } catch (_: SecurityException) {
        }
        return Result.success()
    }
}
