package com.mannat.medicine_reminder.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.mannat.medicine_reminder.domain.model.DoseStatus
import com.mannat.medicine_reminder.domain.usecase.doselog.LogDoseTakenUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@AndroidEntryPoint
class DoseActionReceiver : BroadcastReceiver() {

    @Inject lateinit var logDoseTakenUseCase: LogDoseTakenUseCase
    @Inject lateinit var alarmScheduler: AlarmScheduler

    companion object {
        const val ACTION_DOSE_TAKEN = "com.mannat.medicine_reminder.ACTION_DOSE_TAKEN"
        const val EXTRA_SCHEDULE_ID = "schedule_id"
        private const val FOLLOW_UP_NOTIFICATION_OFFSET = 200_000
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_DOSE_TAKEN) return

        val scheduleId = intent.getLongExtra(EXTRA_SCHEDULE_ID, -1)
        if (scheduleId == -1L) return

        // Cancel notifications synchronously so they disappear immediately —
        // before the suspending DB call and before goAsync() finishes.
        val nm = NotificationManagerCompat.from(context)
        nm.cancel(scheduleId.toInt())
        nm.cancel(scheduleId.toInt() + FOLLOW_UP_NOTIFICATION_OFFSET)
        // Cancel any pending follow-up alarm so it doesn't fire after this
        alarmScheduler.cancelFollowUpAlarm(scheduleId)

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                logDoseTakenUseCase(scheduleId, LocalDate.now(), DoseStatus.TAKEN)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
