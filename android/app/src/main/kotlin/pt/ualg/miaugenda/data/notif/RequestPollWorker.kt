package pt.ualg.miaugenda.data.notif

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import pt.ualg.miaugenda.MainActivity
import pt.ualg.miaugenda.MiauGendaApp
import pt.ualg.miaugenda.data.remote.RetrofitClient
import java.util.concurrent.TimeUnit

private const val CHANNEL_ID = "miaugenda_requests"
private const val CHANNEL_NAME = "Pedidos"
private const val WORK_NAME = "request_poll"
private const val PREFS_NAME = "miaugenda_notif"
private const val KEY_LAST_TIMEOFF_ID = "last_timeoff_id"
private const val KEY_LAST_SWAP_ID = "last_swap_id"
private const val NOTIF_BASE_ID = 4200

class RequestPollWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        val tm = MiauGendaApp.getTokenManager(ctx)
        if (tm.getAccessToken() == null) return Result.success()

        val userId = tm.getUserId()
        val role = tm.getUserRole() ?: "EMPLOYEE"
        val isManager = role == "ADMIN" || role == "MANAGER"

        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastTorId = prefs.getInt(KEY_LAST_TIMEOFF_ID, 0)
        val lastSwapId = prefs.getInt(KEY_LAST_SWAP_ID, 0)
        val firstRun = lastTorId == 0 && lastSwapId == 0

        return try {
            val torResp = RetrofitClient.requestApi.getTimeOffRequests()
            val swapResp = RetrofitClient.requestApi.getShiftSwapRequests()
            if (!torResp.isSuccessful || !swapResp.isSuccessful) return Result.retry()

            val tors = torResp.body().orEmpty()
            val swaps = swapResp.body().orEmpty()

            val maxTorId = (tors.maxOfOrNull { it.id } ?: 0).coerceAtLeast(lastTorId)
            val maxSwapId = (swaps.maxOfOrNull { it.id } ?: 0).coerceAtLeast(lastSwapId)

            val items = mutableListOf<Pair<String, String>>()
            if (!firstRun) {
                tors.filter { it.id > lastTorId }
                    .filter { isManager && it.userId != userId && it.status == "PENDING" }
                    .forEach { items += "Novo pedido de folga" to "${it.user?.name ?: "Funcionário"} submeteu um pedido" }

                swaps.filter { it.id > lastSwapId }.forEach { s ->
                    val relevant = when {
                        isManager -> s.requesterId != userId && s.status == "PENDING"
                        s.targetShift?.user?.id == userId && s.targetAccepted == null -> true
                        else -> false
                    }
                    if (relevant) {
                        val title = if (isManager) "Novo pedido de troca" else "Troca de turno"
                        val body = "${s.requester?.name ?: "Colega"} pediu uma troca de turno"
                        items += title to body
                    }
                }
            }

            prefs.edit().putInt(KEY_LAST_TIMEOFF_ID, maxTorId).putInt(KEY_LAST_SWAP_ID, maxSwapId).apply()

            if (items.isNotEmpty()) {
                val nm = NotificationManagerCompat.from(ctx)
                if (nm.areNotificationsEnabled()) {
                    items.forEachIndexed { i, (title, body) ->
                        try {
                            nm.notify(NOTIF_BASE_ID + i, buildNotification(ctx, title, body))
                        } catch (_: SecurityException) {
                        }
                    }
                }
            }

            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    private fun buildNotification(ctx: Context, title: String, body: String): Notification {
        val intent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            ctx, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
    }
}

fun createRequestNotificationChannel(ctx: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    if (nm.getNotificationChannel(CHANNEL_ID) != null) return
    val ch = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT).apply {
        description = "Avisos de novos pedidos de folga e de troca"
    }
    nm.createNotificationChannel(ch)
}

fun schedulePollingForRequests(ctx: Context) {
    val req = PeriodicWorkRequestBuilder<RequestPollWorker>(15, TimeUnit.MINUTES).build()
    WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
        WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, req
    )
}

fun stopPollingForRequests(ctx: Context) {
    WorkManager.getInstance(ctx).cancelUniqueWork(WORK_NAME)
    ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply()
}
