package com.gymtrack.ultra

import android.app.Application
import com.gymtrack.ultra.core.service.WorkoutNotifications
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class GymTrackApp : Application() {
    override fun onCreate() {
        super.onCreate()
        WorkoutNotifications.createChannels(this)
    }
}
