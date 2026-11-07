package com.gymtrack.ultra.core.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.gymtrack.ultra.MainActivity
import com.gymtrack.ultra.R
import com.gymtrack.ultra.domain.model.LiveSession
import com.gymtrack.ultra.domain.model.RestTimer

/** Kanały i treść powiadomień treningu. Jedno ciche "w toku" + jedno głośne "koniec przerwy". */
object WorkoutNotifications {
    const val CHANNEL_ONGOING = "workout_ongoing"
    const val CHANNEL_REST = "rest_done"
    const val ID_ONGOING = 7001
    const val ID_REST_DONE = 7002

    private val restPattern = longArrayOf(0, 220, 120, 220)

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ONGOING, "Trening w toku", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Czas treningu i odliczanie przerwy"
                setShowBadge(false)
            },
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_REST, "Koniec przerwy", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Sygnał, że czas na kolejną serię"
                enableVibration(true)
                vibrationPattern = restPattern
            },
        )
    }

    private fun openApp(context: Context): PendingIntent =
        PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    fun ongoing(context: Context, session: LiveSession?, timer: RestTimer?, nowMillis: Long) =
        NotificationCompat.Builder(context, CHANNEL_ONGOING)
            .setSmallIcon(R.drawable.ic_stat_workout)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setContentIntent(openApp(context))
            .apply {
                val title = session?.let { "${it.title} · ${it.completedPlannedSets}/${it.plannedSets} serii" } ?: "Trening w toku"
                setContentTitle(title)
                if (timer != null && timer.endsAtMillis > nowMillis) {
                    setContentText("Przerwa · następnie: ${timer.label}")
                    setUsesChronometer(true)
                    setChronometerCountDown(true)
                    setWhen(timer.endsAtMillis)
                    setShowWhen(true)
                } else if (session != null) {
                    setContentText("Czas treningu")
                    setUsesChronometer(true)
                    setChronometerCountDown(false)
                    setWhen(session.startedAt.toEpochMilli())
                    setShowWhen(true)
                }
            }
            .build()

    fun restDone(context: Context, label: String) =
        NotificationCompat.Builder(context, CHANNEL_REST)
            .setSmallIcon(R.drawable.ic_stat_workout)
            .setContentTitle("Koniec przerwy")
            .setContentText(if (label.isBlank()) "Czas na kolejną serię" else "Czas na serię: $label")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVibrate(restPattern)
            .setAutoCancel(true)
            .setTimeoutAfter(60_000)
            .setContentIntent(openApp(context))
            .build()
}
