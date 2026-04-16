package com.mannat.medicine_reminder.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.mannat.medicine_reminder.domain.model.DayAdherence
import com.mannat.medicine_reminder.ui.theme.AdherenceFull
import com.mannat.medicine_reminder.ui.theme.AdherenceNoData
import com.mannat.medicine_reminder.ui.theme.AdherenceNone
import com.mannat.medicine_reminder.ui.theme.AdherencePartial

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdherenceChart(
    dailyBreakdown: List<DayAdherence>,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        dailyBreakdown.forEach { day ->
            val color = when {
                day.scheduledCount == 0 -> AdherenceNoData
                day.adherencePercentage >= 100f -> AdherenceFull
                day.adherencePercentage > 0f -> AdherencePartial
                else -> AdherenceNone
            }
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color)
            )
        }
    }
}
