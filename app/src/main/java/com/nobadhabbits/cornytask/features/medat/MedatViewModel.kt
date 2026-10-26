package com.nobadhabbits.cornytask.features.medat

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nobadhabbits.cornytask.data.MedatDayType
import com.nobadhabbits.cornytask.data.MedatItemProgress
import com.nobadhabbits.cornytask.features.time_goals.TimeGoalRepository
import com.nobadhabbits.cornytask.features.todo.TodoRepository
import com.nobadhabbits.cornytask.features.widget.ACTION_DATA_UPDATED
import com.nobadhabbits.cornytask.features.widget.TodoWidgetReceiver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.ZoneId

/** A plan item together with what has happened to it. */
data class MedatItemState(val item: MedatPlanItem, val progress: MedatItemProgress?) {
    val day: Int get() = progress?.day ?: item.day
    val isDone: Boolean get() = progress?.isDone == true
    val movedFromDay: Int? get() = item.day.takeIf { it != day }
    val timeGoalId: String? get() = progress?.timeGoalId
    val todoId: String? get() = progress?.todoId
    val completedOn: LocalDate?
        get() = progress?.completedAt?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDate()
}

data class MedatUiState(
    val itemsByDay: Map<Int, List<MedatItemState>> = emptyMap(),
    val superDays: Set<Int> = emptySet(),
    // Plan days can run past 111 when items are pushed off the end.
    val lastDay: Int = MEDAT_PLAN_DAYS,
    // First day with something left to do; null once the whole plan is done.
    val currentDay: Int? = 1,
    val shownDay: Int = 1,
    val doneCount: Int = 0,
    val totalCount: Int = 0,
    val completedByDate: Map<LocalDate, List<MedatItemState>> = emptyMap(),
    val dayTypes: Map<LocalDate, MedatDayType> = emptyMap(),
    // Which plan day lands on which upcoming study day, counting from today.
    val projectedDays: Map<LocalDate, Int> = emptyMap(),
    val projectedFinish: LocalDate? = null,
    // Study days from today up to and including the target date.
    val studyDaysUntilTarget: Int = 0
) {
    val shownItems: List<MedatItemState> get() = itemsByDay[shownDay].orEmpty()

    /** Plan days that still have something left, including the current one. */
    val daysLeft: Int get() = itemsByDay.count { (_, items) -> items.any { !it.isDone } }

    fun dayType(date: LocalDate): MedatDayType = dayTypes[date] ?: MedatDayType.STUDY
}

class MedatViewModel(application: Application) : AndroidViewModel(application) {

    private val plan = MedatPlan.load(application)
    private val medatRepository = MedatRepository()
    private val timeGoalRepository = TimeGoalRepository()
    private val todoRepository = TodoRepository(application)

    // Null follows the current day; set when browsing to another day.
    private val browsedDay = MutableStateFlow<Int?>(null)

    val uiState: StateFlow<MedatUiState> =
        combine(medatRepository.getDataFlow(), browsedDay) { data, browsed ->
            buildState(data, browsed)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), buildState(MedatData(), null))

    private fun buildState(data: MedatData, browsed: Int?): MedatUiState {
        val items = plan.flatMap { day -> day.items.map { MedatItemState(it, data.progress[it.id]) } }
        // Items carried over from earlier days go first, the way they'd be caught up on.
        val itemsByDay = items.groupBy { it.day }
            .mapValues { (_, dayItems) -> dayItems.sortedBy { it.item.day } }
        val lastDay = maxOf(MEDAT_PLAN_DAYS, itemsByDay.keys.maxOrNull() ?: 0)
        val pendingDays = itemsByDay.keys.filter { day -> itemsByDay[day]!!.any { !it.isDone } }.sorted()
        val currentDay = pendingDays.firstOrNull()

        val today = LocalDate.now()
        val isStudyDay = { date: LocalDate -> (data.dayTypes[date] ?: MedatDayType.STUDY) == MedatDayType.STUDY }
        val projectedDays = projectDays(pendingDays, today, isStudyDay)
        val studyDaysUntilTarget = generateSequence(today) { it.plusDays(1) }
            .takeWhile { !it.isAfter(MEDAT_TARGET_DATE) }
            .count(isStudyDay)

        return MedatUiState(
            itemsByDay = itemsByDay,
            superDays = plan.filter { it.isSuperDay }.map { it.day }.toSet(),
            lastDay = lastDay,
            currentDay = currentDay,
            shownDay = (browsed ?: currentDay ?: lastDay).coerceIn(1, lastDay),
            doneCount = items.count { it.isDone },
            totalCount = items.size,
            completedByDate = items.filter { it.isDone && it.completedOn != null }.groupBy { it.completedOn!! },
            dayTypes = data.dayTypes,
            projectedDays = projectedDays,
            projectedFinish = projectedDays.keys.maxOrNull(),
            studyDaysUntilTarget = studyDaysUntilTarget
        )
    }

    /** Lays the remaining plan days onto the coming study days, one per day, starting today. */
    private fun projectDays(
        pendingDays: List<Int>,
        today: LocalDate,
        isStudyDay: (LocalDate) -> Boolean
    ): Map<LocalDate, Int> {
        val projected = LinkedHashMap<LocalDate, Int>()
        var date = today
        val giveUp = today.plusYears(3) // in case nearly everything is marked as a break
        for (planDay in pendingDays) {
            while (!isStudyDay(date) && date.isBefore(giveUp)) date = date.plusDays(1)
            if (!date.isBefore(giveUp)) break
            projected[date] = planDay
            date = date.plusDays(1)
        }
        return projected
    }

    fun showDay(day: Int) {
        browsedDay.value = day
    }

    fun showCurrentDay() {
        browsedDay.value = null
    }

    fun setDone(item: MedatItemState, isDone: Boolean) {
        if (isDone) {
            medatRepository.tick(item.item.id, item.item.displayTitle, item.item.coins)
        } else {
            medatRepository.untick(item.progress ?: MedatItemProgress(id = item.item.id))
        }
    }

    fun postpone(item: MedatItemState) {
        medatRepository.postpone(item.item.id, item.day)
    }

    fun setDayType(date: LocalDate, type: MedatDayType) {
        medatRepository.setDayType(date, type)
    }

    fun createTimeGoal(items: List<MedatItemState>, title: String, minutes: Long, coins: Int) {
        val ids = items.map { it.item.id }
        val goalId = timeGoalRepository.addTimeGoal(title, minutes, coins, ids) ?: return
        medatRepository.linkTimeGoal(ids, goalId)
    }

    fun createTodo(items: List<MedatItemState>, title: String, coins: Int) {
        val ids = items.map { it.item.id }
        val description = items.joinToString("\n") { "• ${it.item.displayTitle} (${it.item.minutes} min)" }
        val todoId = todoRepository.addTodo(title, description, coins, null, ids) ?: return
        medatRepository.linkTodo(ids, todoId)
        val app = getApplication<Application>()
        app.sendBroadcast(Intent(app, TodoWidgetReceiver::class.java).apply { action = ACTION_DATA_UPDATED })
    }
}
