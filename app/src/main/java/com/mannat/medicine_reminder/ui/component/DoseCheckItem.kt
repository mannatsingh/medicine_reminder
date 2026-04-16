package com.mannat.medicine_reminder.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.mannat.medicine_reminder.domain.model.DailyDoseItem
import com.mannat.medicine_reminder.domain.model.DoseStatus
import com.mannat.medicine_reminder.ui.theme.AdherenceFull
import com.mannat.medicine_reminder.ui.theme.AdherenceNone
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun DoseCheckItem(
    item: DailyDoseItem,
    selectedDate: LocalDate,
    onToggle: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isTaken = item.status == DoseStatus.TAKEN
    val isSkipped = item.status == DoseStatus.SKIPPED
    val isPending = item.status == null
    val isOverdue = isPending
            && selectedDate == LocalDate.now()
            && LocalTime.now().isAfter(item.scheduledTime)

    val containerColor by animateColorAsState(
        targetValue = when {
            isTaken -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
            isSkipped -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            isOverdue -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            else -> MaterialTheme.colorScheme.surface
        },
        label = "containerColor"
    )

    val checkScale by animateFloatAsState(
        targetValue = if (isTaken) 1f else 0.8f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "checkScale"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                IconButton(
                    onClick = onToggle,
                    modifier = Modifier.scale(checkScale)
                ) {
                    if (isTaken) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = "Taken",
                            tint = AdherenceFull,
                            modifier = Modifier.size(28.dp)
                        )
                    } else {
                        Icon(
                            Icons.Outlined.Circle,
                            contentDescription = "Not taken",
                            tint = if (isOverdue) AdherenceNone
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Column(modifier = Modifier.padding(start = 4.dp)) {
                    Text(
                        text = item.medicineName,
                        style = MaterialTheme.typography.bodyLarge,
                        textDecoration = if (isTaken) TextDecoration.LineThrough else null
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${item.dosage} - ${formatTime(item.scheduledTime)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (isOverdue) {
                            Text(
                                text = "Overdue",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            if (isPending || isOverdue) {
                TextButton(onClick = onSkip) {
                    Text("Skip", style = MaterialTheme.typography.labelMedium)
                }
            }

            if (isSkipped) {
                TextButton(onClick = onSkip) {
                    Text("Undo skip", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

private fun formatTime(time: LocalTime): String {
    val hour = if (time.hour % 12 == 0) 12 else time.hour % 12
    val amPm = if (time.hour < 12) "AM" else "PM"
    return "%d:%02d %s".format(hour, time.minute, amPm)
}
