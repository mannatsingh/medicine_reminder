package com.mannat.medicine_reminder.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mannat.medicine_reminder.domain.model.DayAdherence
import com.mannat.medicine_reminder.ui.theme.AdherenceFull
import com.mannat.medicine_reminder.ui.theme.AdherenceNoData
import com.mannat.medicine_reminder.ui.theme.AdherenceNone
import com.mannat.medicine_reminder.ui.theme.AdherencePartial
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun MonthCalendar(
    yearMonth: YearMonth,
    dayData: List<DayAdherence>,
    selectedDay: LocalDate?,
    onDayClick: (LocalDate) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dayMap = dayData.associateBy { it.date }
    val firstDayOfMonth = yearMonth.atDay(1)
    val lastDay = yearMonth.lengthOfMonth()
    val startDayOfWeek = firstDayOfMonth.dayOfWeek.value // 1=Mon, 7=Sun
    val today = LocalDate.now()

    Column(modifier = modifier) {
        // Month header with navigation
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPreviousMonth) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous month")
            }
            Text(
                "${yearMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${yearMonth.year}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            IconButton(
                onClick = onNextMonth,
                enabled = yearMonth.isBefore(YearMonth.now())
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next month")
            }
        }

        // Day-of-week headers
        Row(modifier = Modifier.fillMaxWidth()) {
            for (i in 1..7) {
                val day = DayOfWeek.of(i)
                Text(
                    text = day.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Calendar grid
        var dayCounter = 1
        val totalCells = ((startDayOfWeek - 1) + lastDay)
        val rows = (totalCells + 6) / 7

        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 1..7) {
                    val cellIndex = row * 7 + col
                    val dayNum = cellIndex - (startDayOfWeek - 1)

                    if (dayNum in 1..lastDay) {
                        val date = yearMonth.atDay(dayNum)
                        val adherence = dayMap[date]
                        val isToday = date == today
                        val isSelected = date == selectedDay
                        val isFuture = date.isAfter(today)

                        val bgColor = when {
                            isSelected -> MaterialTheme.colorScheme.primary
                            isFuture -> Color.Transparent
                            adherence == null -> Color.Transparent
                            adherence.scheduledCount == 0 -> Color.Transparent
                            adherence.adherencePercentage >= 100f -> AdherenceFull.copy(alpha = 0.3f)
                            adherence.adherencePercentage > 0f -> AdherencePartial.copy(alpha = 0.3f)
                            else -> AdherenceNone.copy(alpha = 0.3f)
                        }

                        val textColor = when {
                            isSelected -> MaterialTheme.colorScheme.onPrimary
                            isFuture -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            else -> MaterialTheme.colorScheme.onSurface
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .clip(CircleShape)
                                .background(bgColor)
                                .then(
                                    if (!isFuture) Modifier.clickable { onDayClick(date) }
                                    else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = dayNum.toString(),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                    color = textColor
                                )
                                // Small dot indicator
                                if (!isFuture && adherence != null && adherence.scheduledCount > 0 && !isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(4.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    adherence.adherencePercentage >= 100f -> AdherenceFull
                                                    adherence.adherencePercentage > 0f -> AdherencePartial
                                                    else -> AdherenceNone
                                                }
                                            )
                                    )
                                }
                            }
                        }
                    } else {
                        Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                    }
                }
            }
        }
    }
}
