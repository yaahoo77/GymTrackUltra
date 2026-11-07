package com.gymtrack.ultra.core.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.gymtrack.ultra.domain.repository.RestTimerRepository
import com.gymtrack.ultra.domain.repository.WorkoutRepository
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Usługa pierwszoplanowa na czas treningu: trzyma proces przy życiu, pokazuje czas
 * i odliczanie przerwy w powiadomieniu oraz wibruje na koniec przerwy, gdy aplikacja jest w tle.
 * Kończy się sama, gdy nie ma aktywnego treningu.
 */
@AndroidEntryPoint
class WorkoutService : Service() {

    @Inject lateinit var workoutRepository: WorkoutRepository
    @Inject lateinit var restTimerRepository: RestTimerRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var observing = false
    private var lastAlertedEndsAt = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val initial = WorkoutNotifications.ongoing(this, null, null, System.currentTimeMillis())
        ServiceCompat.startForeground(
            this,
            WorkoutNotifications.ID_ONGOING,
            initial,
            if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0,
        )
        if (!observing) {
            observing = true
            observe()
        }
        // Po zabiciu procesu nie wskrzeszamy usługi — aplikacja sama przywróci trening przy otwarciu.
        return START_NOT_STICKY
    }

    private fun observe() {
        scope.launch {
            combine(workoutRepository.observeActiveSession(), restTimerRepository.timer) { s, t -> s to t }
                .collectLatest { (session, timer) ->
                    if (session == null) {
                        ServiceCompat.stopForeground(this@WorkoutService, ServiceCompat.STOP_FOREGROUND_REMOVE)
                        stopSelf()
                        return@collectLatest
                    }
                    post(WorkoutNotifications.ongoing(this@WorkoutService, session, timer, System.currentTimeMillis()))
                    if (timer != null) {
                        val wait = timer.endsAtMillis - System.currentTimeMillis()
                        if (wait > 0) {
                            delay(wait)
                            if (timer.endsAtMillis != lastAlertedEndsAt && !isAppVisible()) {
                                lastAlertedEndsAt = timer.endsAtMillis
                                post(WorkoutNotifications.restDone(this@WorkoutService, timer.label), WorkoutNotifications.ID_REST_DONE)
                            }
                            post(WorkoutNotifications.ongoing(this@WorkoutService, session, null, System.currentTimeMillis()))
                        }
                    }
                }
        }
    }

    private fun isAppVisible(): Boolean =
        ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)

    @SuppressLint("MissingPermission")
    private fun post(notification: android.app.Notification, id: Int = WorkoutNotifications.ID_ONGOING) {
        val allowed = Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (allowed) NotificationManagerCompat.from(this).notify(id, notification)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}

/** Uruchamia usługę z pierwszego planu (np. przy otwarciu ekranu treningu). */
@Singleton
class WorkoutServiceController @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun ensureRunning() {
        runCatching {
            ContextCompat.startForegroundService(context, Intent(context, WorkoutService::class.java))
        }
    }
}
