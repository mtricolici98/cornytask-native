package com.nobadhabbits.cornytask.features.cycle

import com.nobadhabbits.cornytask.data.cycle.CycleEntry
import com.nobadhabbits.cornytask.data.cycle.CycleType
import java.time.LocalDate
import kotlin.math.roundToLong

class CyclePredictor {

    fun predict(entries: List<CycleEntry>): Map<LocalDate, PredictionType> {

        val starts = extractCycleStarts(entries)
        if (starts.isEmpty()) return emptyMap()

        val avgCycle = estimateCycleLength(starts)
        val periodLength = estimatePeriodLength(entries)

        val lutealPhase = 13L
        val lastStart = starts.last()

        val result = mutableMapOf<LocalDate, PredictionType>()

        var nextStart = lastStart.plusDays(avgCycle)

        repeat(6) { cycleIndex ->

            val confidence = when (cycleIndex) {
                0 -> 1.0
                1 -> 0.9
                2 -> 0.75
                3 -> 0.6
                else -> 0.45
            }

            // 🩸 predicted period
            repeat(periodLength) { day ->
                result[nextStart.plusDays(day.toLong())] =
                    PredictionType.PREDICTED_PERIOD
            }

            // 🌿 fertile window
            val ovulation = nextStart.minusDays(lutealPhase)

            for (offset in -4..1) {
                result[ovulation.plusDays(offset.toLong())] =
                    PredictionType.FERTILE
            }
            // 5 days before ovulation and 1 day after ovulation chances are high.
            nextStart = nextStart.plusDays(avgCycle)
        }

        // current cycle fertile prediction
        val currentOvulation = lastStart.plusDays(avgCycle - lutealPhase)
        for (offset in -4..1) {
            result[currentOvulation.plusDays(offset.toLong())] =
                PredictionType.FERTILE
        }

        return result
    }

    /**
     * ⭐ Weighted adaptive cycle estimation
     */
    private fun estimateCycleLength(starts: List<LocalDate>): Long {

        if (starts.size < 2) return 28L

        val rawLengths = starts.zipWithNext { a, b ->
            b.toEpochDay() - a.toEpochDay()
        }

        // remove biological outliers
        val filtered = rawLengths.filter { it in 18..45 }

        if (filtered.isEmpty()) return 28L

        val recent = filtered.takeLast(6)

        // weighted moving average
        var weightedSum = 0.0
        var weightTotal = 0.0

        recent.forEachIndexed { index, value ->
            val weight = (index + 1).toDouble()
            weightedSum += value * weight
            weightTotal += weight
        }

        return (weightedSum / weightTotal).roundToLong()
    }

    /**
     * Extract FIRST bleeding day of each cycle
     */
    private fun extractCycleStarts(entries: List<CycleEntry>): List<LocalDate> {

        val periodDays = entries
            .filter { it.type == CycleType.PERIOD }
            .map { it.date }
            .sorted()

        if (periodDays.isEmpty()) return emptyList()

        val periodSet = periodDays.toSet()
        val starts = mutableListOf<LocalDate>()

        for (date in periodDays) {
            if (starts.isEmpty() || !periodSet.contains(date.minusDays(1))) {
                starts.add(date)
            }
        }

        return starts
    }

    /**
     * Estimate average bleeding duration
     */
    private fun estimatePeriodLength(entries: List<CycleEntry>): Int {

        val periodDays = entries
            .filter { it.type == CycleType.PERIOD }
            .map { it.date }
            .sorted()

        if (periodDays.isEmpty()) return 5

        val lengths = mutableListOf<Int>()
        var current = 1

        for (i in 1 until periodDays.size) {
            if (periodDays[i] == periodDays[i - 1].plusDays(1)) {
                current++
            } else {
                lengths.add(current)
                current = 1
            }
        }

        lengths.add(current)

        return lengths
            .average()
            .roundToLong()
            .toInt()
            .coerceIn(3, 7)
    }
}

enum class PredictionType {
    PREDICTED_PERIOD,
    FERTILE
}