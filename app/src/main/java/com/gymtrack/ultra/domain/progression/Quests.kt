package com.gymtrack.ultra.domain.progression

enum class QuestMetric { WORKOUTS, WARMUPS, FULL_SESSIONS, RECORDS, PLAN_DAYS, SETS_TOTAL, MUSCLE_SETS }

data class QuestDefinition(
    val id: String,
    val title: String,
    val metric: QuestMetric,
    val target: Int,
    val xp: Int,
    val muscle: MuscleGroup? = null,
)

data class QuestProgress(val quest: QuestDefinition, val current: Int) {
    val isDone: Boolean get() = current >= quest.target
    val fraction: Float get() = if (quest.target <= 0) 1f else (current.toFloat() / quest.target).coerceIn(0f, 1f)
}

/** Co zrobiono w bieżącym tygodniu — wejście dla wyzwań. */
data class WeekActivity(
    val workouts: Int = 0,
    val warmups: Int = 0,
    val fullSessions: Int = 0,
    val records: Int = 0,
    val distinctPlanDays: Int = 0,
    val setsTotal: Int = 0,
    val setsByMuscle: Map<MuscleGroup, Int> = emptyMap(),
)

/**
 * Wyzwania tygodnia: zawsze "zrób swoje treningi" + 2 rotujące, wybierane deterministycznie z numeru tygodnia.
 * Nagrody są ograniczone (łącznie ok. 100 XP tygodniowo) i nie zachęcają do przetrenowania.
 */
object WeeklyQuests {
    const val XP_REASON_PREFIX = "QUEST:"

    private val pool = listOf(
        QuestDefinition("WARMUP_2", "Rozgrzej się przed 2 treningami", QuestMetric.WARMUPS, 2, 25),
        QuestDefinition("FULL_2", "Zalicz komplet serii w 2 treningach", QuestMetric.FULL_SESSIONS, 2, 30),
        QuestDefinition("RECORDS_2", "Pobij 2 rekordy osobiste", QuestMetric.RECORDS, 2, 35),
        QuestDefinition("SETS_30", "Zalicz 30 serii", QuestMetric.SETS_TOTAL, 30, 35),
        QuestDefinition("LEGS_8", "Zalicz 8 serii na nogi", QuestMetric.MUSCLE_SETS, 8, 25, MuscleGroup.LEGS),
        QuestDefinition("BACK_6", "Zalicz 6 serii na plecy", QuestMetric.MUSCLE_SETS, 6, 25, MuscleGroup.BACK),
        QuestDefinition("CHEST_6", "Zalicz 6 serii na klatkę", QuestMetric.MUSCLE_SETS, 6, 25, MuscleGroup.CHEST),
        QuestDefinition("CORE_4", "Zalicz 4 serie na brzuch", QuestMetric.MUSCLE_SETS, 4, 20, MuscleGroup.CORE),
    )

    fun all(planDays: Int): List<QuestDefinition> = listOf(core(planDays), allDays(planDays)) + pool

    fun core(planDays: Int): QuestDefinition {
        val target = planDays.coerceIn(1, 7)
        return QuestDefinition("WORKOUTS", "Ukończ ${target} ${if (target == 1) "trening" else if (target in 2..4) "treningi" else "treningów"}", QuestMetric.WORKOUTS, target, 40)
    }

    private fun allDays(planDays: Int) =
        QuestDefinition("ALL_DAYS", "Zrób każdy dzień planu", QuestMetric.PLAN_DAYS, planDays.coerceAtLeast(1), 30)

    fun forWeek(weekStartEpochDay: Long, planDays: Int): List<QuestDefinition> {
        val rotating = pool + allDays(planDays)
        val week = Math.floorDiv(weekStartEpochDay, 7L)
        val first = Math.floorMod(week, rotating.size.toLong()).toInt()
        var second = Math.floorMod(week * 5 + 3, rotating.size.toLong()).toInt()
        if (second == first) second = (second + 1) % rotating.size
        return listOf(core(planDays), rotating[first], rotating[second])
    }

    fun progress(quests: List<QuestDefinition>, activity: WeekActivity): List<QuestProgress> =
        quests.map { quest ->
            val value = when (quest.metric) {
                QuestMetric.WORKOUTS -> activity.workouts
                QuestMetric.WARMUPS -> activity.warmups
                QuestMetric.FULL_SESSIONS -> activity.fullSessions
                QuestMetric.RECORDS -> activity.records
                QuestMetric.PLAN_DAYS -> activity.distinctPlanDays
                QuestMetric.SETS_TOTAL -> activity.setsTotal
                QuestMetric.MUSCLE_SETS -> activity.setsByMuscle[quest.muscle] ?: 0
            }
            QuestProgress(quest, value.coerceAtMost(quest.target))
        }

    fun xpReason(quest: QuestDefinition): String = XP_REASON_PREFIX + quest.id
}
