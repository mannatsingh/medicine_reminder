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

    companion object {
        const val ACTION_DOSE_TAKEN = "com.mannat.medicine_reminder.ACTION_DOSE_TAKEN"
        const val EXTRA_SCHEDULE_ID = "schedule_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_DOSE_TAKEN) return

        val scheduleId = intent.getLongExtra(EXTRA_SCHEDULE_ID, -1)
        if (scheduleId == -1L) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                logDoseTakenUseCase(scheduleId, LocalDate.now(), DoseStatus.TAKEN)
                NotificationManagerCompat.from(context).cancel(scheduleId.toInt())
            } finally {
                pendingResult.finish()
            }
        }
    }
}
