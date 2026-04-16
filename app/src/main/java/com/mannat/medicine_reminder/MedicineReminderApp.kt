package com.mannat.medicine_reminder

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.mannat.medicine_reminder.domain.usecase.alarm.RescheduleAllAlarmsUseCase
import com.mannat.medicine_reminder.notification.NotificationHelper
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class MedicineReminderApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var rescheduleAllAlarmsUseCase: RescheduleAllAlarmsUseCase

    override fun onCreate() {
        super.onCreate()
        notificationHelper.createNotificationChannel()

        // Reschedule all alarms on every app launch — catches medicines added
        // before alarm wiring, reinstalls, and any missed rescheduling
        CoroutineScope(Dispatchers.IO).launch {
            rescheduleAllAlarmsUseCase()
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
