package com.aritxonly.deadliner

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.ui.theme.DeadlinerTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ExcludeDatesActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val startDateStr = intent.getStringExtra(EXTRA_START_DATE)
        val endDateStr = intent.getStringExtra(EXTRA_END_DATE)
        if (startDateStr.isNullOrBlank() || endDateStr.isNullOrBlank()) {
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }

        val startDate = LocalDate.parse(startDateStr)
        val endDate = LocalDate.parse(endDateStr)

        val excludedDatesList =
            intent.getStringArrayListExtra(EXTRA_EXCLUDED_DATES)?.mapNotNull {
                runCatching { LocalDate.parse(it) }.getOrNull()
            } ?: emptyList()

        val initialExcluded = excludedDatesList
            .filter { !it.isBefore(startDate) && !it.isAfter(endDate) }
            .toSet()

        setContent {
            DeadlinerTheme {
                ExcludeDatesScreen(
                    startDate = startDate,
                    endDate = endDate,
                    initialExcludedDates = initialExcluded,
                    onConfirm = { dates ->
                        val result = Intent().apply {
                            putStringArrayListExtra(
                                EXTRA_EXCLUDED_DATES,
                                ArrayList(dates.sorted().map { it.toString() })
                            )
                        }
                        setResult(Activity.RESULT_OK, result)
                        finishAfterTransition()
                    },
                    onCancel = {
                        setResult(Activity.RESULT_CANCELED)
                        finishAfterTransition()
                    }
                )
            }
        }
    }

    companion object {
        const val EXTRA_START_DATE = "extra_start_date"
        const val EXTRA_END_DATE = "extra_end_date"
        const val EXTRA_EXCLUDED_DATES = "extra_excluded_dates"
    }
}

private data class DateRange(
    val start: LocalDate,
    val end: LocalDate
)

@Composable
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
private fun ExcludeDatesScreen(
    startDate: LocalDate,
    endDate: LocalDate,
    initialExcludedDates: Set<LocalDate>,
    onConfirm: (Set<LocalDate>) -> Unit,
    onCancel: () -> Unit
) {
    var excludedDates by remember { mutableStateOf(initialExcludedDates.toSet()) }
    var tempRangeStart by remember { mutableStateOf<LocalDate?>(null) }
    var tempRangeEnd by remember { mutableStateOf<LocalDate?>(null) }

    val monthFormatter = remember { DateTimeFormatter.ofPattern("yyyy-MM") }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd") }

    val months = remember(startDate, endDate) {
        generateSequence(startDate.withDayOfMonth(1)) { it.plusMonths(1) }
            .takeWhile { !it.isAfter(endDate) }
            .toList()
    }

    val ranges = remember(excludedDates) {
        buildRanges(excludedDates)
    }

    val deadlineRangeText = "${startDate.format(dateFormatter)} ~ ${endDate.format(dateFormatter)}"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.exclude_specific_dates),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_close),
                            contentDescription = stringResource(R.string.close)
                        )
                    }
                },
                actions = {
                    Text(
                        text = stringResource(R.string.save),
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .clickable { onConfirm(excludedDates) },
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(
                    R.string.exclude_specific_dates_with_range,
                    deadlineRangeText
                ),
                style = MaterialTheme.typography.bodyMedium
            )

            if (ranges.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.exclude_dates_summary_days, excludedDates.size),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ranges.forEach { range ->
                        RangeChip(
                            range = range,
                            formatter = dateFormatter,
                            onRemove = {
                                excludedDates = excludedDates.filterNot {
                                    !it.isBefore(range.start) && !it.isAfter(range.end)
                                }.toSet()
                            }
                        )
                    }
                }
            }

            if (tempRangeStart != null) {
                val start = tempRangeStart!!
                val end = tempRangeEnd ?: tempRangeStart!!
                val text = if (start == end) {
                    start.format(dateFormatter)
                } else {
                    "${start.format(dateFormatter)} ~ ${end.format(dateFormatter)}"
                }
                Text(
                    text = stringResource(R.string.current_select_range, text),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Button(
                onClick = {
                    val start = tempRangeStart ?: return@Button
                    val end = tempRangeEnd ?: start
                    val normalizedStart = minOf(start, end)
                    val normalizedEnd = maxOf(start, end)
                    val newDates = generateSequence(normalizedStart) { it.plusDays(1) }
                        .takeWhile { !it.isAfter(normalizedEnd) }
                        .toSet()
                    excludedDates = (excludedDates + newDates)
                        .filter { !it.isBefore(startDate) && !it.isAfter(endDate) }
                        .toSet()
                    tempRangeStart = null
                    tempRangeEnd = null
                },
                enabled = tempRangeStart != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.add_exclude_range))
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(months) { monthStart ->
                    MonthCalendar(
                        monthStart = monthStart,
                        startDate = startDate,
                        endDate = endDate,
                        excludedDates = excludedDates,
                        tempRangeStart = tempRangeStart,
                        tempRangeEnd = tempRangeEnd,
                        onDateClick = { date ->
                            if (date.isBefore(startDate) || date.isAfter(endDate)) return@MonthCalendar
                            if (tempRangeStart == null || tempRangeEnd != null) {
                                tempRangeStart = date
                                tempRangeEnd = null
                            } else {
                                tempRangeEnd = date
                            }
                        },
                        monthFormatter = monthFormatter,
                        dateFormatter = dateFormatter
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun RangeChip(
    range: DateRange,
    formatter: DateTimeFormatter,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val text = if (range.start == range.end) {
            range.start.format(formatter)
        } else {
            "${range.start.format(formatter)} ~ ${range.end.format(formatter)}"
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(18.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_close),
                contentDescription = stringResource(R.string.delete),
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun MonthCalendar(
    monthStart: LocalDate,
    startDate: LocalDate,
    endDate: LocalDate,
    excludedDates: Set<LocalDate>,
    tempRangeStart: LocalDate?,
    tempRangeEnd: LocalDate?,
    onDateClick: (LocalDate) -> Unit,
    monthFormatter: DateTimeFormatter,
    dateFormatter: DateTimeFormatter
) {
    val monthTitle = monthStart.format(monthFormatter)

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = monthTitle,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        // 周标题
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val weekLabels = listOf("一", "二", "三", "四", "五", "六", "日")
            weekLabels.forEach { label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        val firstDayOfMonth = monthStart
        val lastDayOfMonth = monthStart.plusMonths(1).minusDays(1)

        val firstDayOfWeekIndex = ((firstDayOfMonth.dayOfWeek.value + 6) % 7)
        val totalDays = lastDayOfMonth.dayOfMonth
        val totalCells = firstDayOfWeekIndex + totalDays
        val rows = (totalCells + 6) / 7

        val currentRangeStart = tempRangeStart
        val currentRangeEnd = tempRangeEnd ?: tempRangeStart

        for (row in 0 until rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (col in 0 until 7) {
                    val cellIndex = row * 7 + col
                    val date = if (cellIndex >= firstDayOfWeekIndex &&
                        cellIndex < firstDayOfWeekIndex + totalDays
                    ) {
                        firstDayOfMonth.plusDays((cellIndex - firstDayOfWeekIndex).toLong())
                    } else {
                        null
                    }

                    if (date == null) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                        )
                    } else {
                        val isInDeadlineRange =
                            !date.isBefore(startDate) && !date.isAfter(endDate)
                        val isExcluded = date in excludedDates
                        val inTempRange = if (currentRangeStart != null && currentRangeEnd != null) {
                            val s = minOf(currentRangeStart, currentRangeEnd)
                            val e = maxOf(currentRangeStart, currentRangeEnd)
                            !date.isBefore(s) && !date.isAfter(e)
                        } else {
                            false
                        }
                        val isStart = date == startDate
                        val isEnd = date == endDate

                        val backgroundColor: Color
                        val textColor: Color

                        when {
                            inTempRange -> {
                                backgroundColor = MaterialTheme.colorScheme.primaryContainer
                                textColor = MaterialTheme.colorScheme.onPrimaryContainer
                            }

                            isExcluded -> {
                                backgroundColor = MaterialTheme.colorScheme.secondaryContainer
                                textColor = MaterialTheme.colorScheme.onSecondaryContainer
                            }

                            else -> {
                                backgroundColor = Color.Transparent
                                textColor = MaterialTheme.colorScheme.onSurface
                            }
                        }

                        val enabled = isInDeadlineRange

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(backgroundColor)
                                .clickable(enabled = enabled) { onDateClick(date) }
                                .padding(vertical = 2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = date.dayOfMonth.toString(),
                                color = if (enabled) textColor
                                else MaterialTheme.colorScheme.outline,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (isStart || isEnd) {
                                Text(
                                    text = if (isStart) stringResource(R.string.deadline_start_label) else stringResource(
                                        R.string.deadline_end_label
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = textColor,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun buildRanges(excludedDates: Set<LocalDate>): List<DateRange> {
    if (excludedDates.isEmpty()) return emptyList()
    val sorted = excludedDates.toList().sorted()

    val ranges = mutableListOf<DateRange>()
    var rangeStart = sorted.first()
    var prev = rangeStart

    for (date in sorted.drop(1)) {
        if (date != prev.plusDays(1)) {
            ranges.add(DateRange(rangeStart, prev))
            rangeStart = date
        }
        prev = date
    }
    ranges.add(DateRange(rangeStart, prev))

    return ranges
}
