package com.gymtrack.ultra.domain.progression

/** Co mierzy dane osiągnięcie. */
enum class AchievementMetric {
    COMPLETED_SETS,
    COMPLETED_WORKOUTS,
    WARMUP_WORKOUTS,
    /** Ile różnych dni planu (A/B/C) zrobiono w jednym tygodniu — maksimum. */
    PLAN_DAYS_IN_ONE_WEEK,
    /** Najdłuższa seria kolejnych tygodni z min. 2 treningami. */
    WEEK_STREAK,
    /** Przeczytana technika ćwiczeń z planu (cel = liczba ćwiczeń w planie). */
    TECHNIQUE_VIEWED,
    /** Odblokowywane bezpośrednio akcją (np. świadome zmniejszenie ciężaru). */
    ACTION,
}

data class AchievementDefinition(
    val code: String,
    val title: String,
    val description: String,
    val metric: AchievementMetric,
    /** Cel; dla [AchievementMetric.TECHNIQUE_VIEWED] liczony dynamicznie z planu. */
    val target: Int,
)

/**
 * Bez rankingów, bez porównywania ciał, bez kar.
 * Nagradzamy: pierwszy krok, regularność w tygodniach, rozgrzewkę, technikę i rozsądne obciążenie.
 */
object Achievements {
    const val FIRST_REP = "FIRST_REP"
    const val FIRST_WORKOUT = "FIRST_WORKOUT"
    const val WARMUP_RITUAL = "WARMUP_RITUAL"
    const val FULL_CYCLE = "FULL_CYCLE"
    const val WORKOUTS_10 = "WORKOUTS_10"
    const val CONSISTENT_BUILDER = "CONSISTENT_BUILDER"
    const val SMART_LOAD = "SMART_LOAD"
    const val TECHNIQUE_MASTER = "TECHNIQUE_MASTER"
    const val WORKOUTS_50 = "WORKOUTS_50"
    const val STEADY_QUARTER = "STEADY_QUARTER"
    const val PLAN_ARCHITECT = "PLAN_ARCHITECT"
    const val WORKOUTS_100 = "WORKOUTS_100"

    val all: List<AchievementDefinition> = listOf(
        AchievementDefinition(FIRST_REP, "FIRST REP", "Pierwsza zaliczona seria", AchievementMetric.COMPLETED_SETS, 1),
        AchievementDefinition(FIRST_WORKOUT, "FIRST WORKOUT", "Pierwszy ukończony trening", AchievementMetric.COMPLETED_WORKOUTS, 1),
        AchievementDefinition(WARMUP_RITUAL, "WARM-UP RITUAL", "Rozgrzewka przed 10 treningami", AchievementMetric.WARMUP_WORKOUTS, 10),
        AchievementDefinition(FULL_CYCLE, "FULL CYCLE", "Workout A, B i C w jednym tygodniu", AchievementMetric.PLAN_DAYS_IN_ONE_WEEK, 3),
        AchievementDefinition(WORKOUTS_10, "10 WORKOUTS", "10 ukończonych treningów", AchievementMetric.COMPLETED_WORKOUTS, 10),
        AchievementDefinition(CONSISTENT_BUILDER, "CONSISTENT BUILDER", "4 tygodnie z rzędu, min. 2 treningi", AchievementMetric.WEEK_STREAK, 4),
        AchievementDefinition(SMART_LOAD, "SMART LOAD", "Świadomie zmniejszony ciężar", AchievementMetric.ACTION, 1),
        AchievementDefinition(TECHNIQUE_MASTER, "TECHNIQUE MASTER", "Przeczytaj technikę wszystkich ćwiczeń z planu", AchievementMetric.TECHNIQUE_VIEWED, 0),
        AchievementDefinition(WORKOUTS_50, "50 WORKOUTS", "50 ukończonych treningów", AchievementMetric.COMPLETED_WORKOUTS, 50),
        AchievementDefinition(STEADY_QUARTER, "STEADY QUARTER", "12 tygodni z rzędu, min. 2 treningi", AchievementMetric.WEEK_STREAK, 12),
        AchievementDefinition(PLAN_ARCHITECT, "PLAN ARCHITECT", "Dostosuj swój plan", AchievementMetric.ACTION, 1),
        AchievementDefinition(WORKOUTS_100, "100 WORKOUTS", "100 ukończonych treningów", AchievementMetric.COMPLETED_WORKOUTS, 100),
    )

    fun byCode(code: String): AchievementDefinition? = all.firstOrNull { it.code == code }
}

data class AchievementStats(
    val completedSets: Int = 0,
    val completedWorkouts: Int = 0,
    val warmupWorkouts: Int = 0,
    val maxPlanDaysInOneWeek: Int = 0,
    val longestWeekStreak: Int = 0,
    val techniqueViewed: Int = 0,
    val techniqueTotal: Int = 0,
)

data class AchievementProgress(
    val definition: AchievementDefinition,
    val current: Int,
    val target: Int,
    /** Moment zapisania odblokowania w bazie; null gdy jeszcze nie zapisane. */
    val unlockedAt: Long?,
) {
    val isUnlocked: Boolean get() = unlockedAt != null || current >= target
    val fraction: Float get() = if (target <= 0) 0f else (current.toFloat() / target).coerceIn(0f, 1f)
    val showsProgressBar: Boolean get() = !isUnlocked && target > 1
}

object AchievementEvaluator {

    fun evaluate(stats: AchievementStats, unlocked: Map<String, Long>): List<AchievementProgress> =
        Achievements.all.map { definition ->
            val target = when (definition.metric) {
                AchievementMetric.TECHNIQUE_VIEWED -> stats.techniqueTotal.coerceAtLeast(1)
                else -> definition.target
            }
            val current = when (definition.metric) {
                AchievementMetric.COMPLETED_SETS -> stats.completedSets
                AchievementMetric.COMPLETED_WORKOUTS -> stats.completedWorkouts
                AchievementMetric.WARMUP_WORKOUTS -> stats.warmupWorkouts
                AchievementMetric.PLAN_DAYS_IN_ONE_WEEK -> stats.maxPlanDaysInOneWeek
                AchievementMetric.WEEK_STREAK -> stats.longestWeekStreak
                AchievementMetric.TECHNIQUE_VIEWED -> stats.techniqueViewed
                AchievementMetric.ACTION -> if (definition.code in unlocked) 1 else 0
            }
            AchievementProgress(
                definition = definition,
                current = current.coerceIn(0, target),
                target = target,
                unlockedAt = unlocked[definition.code],
            )
        }

    /** Osiągnięcia spełnione, ale jeszcze niezapisane — do zapisania i pokazania w podsumowaniu. */
    fun newlyReached(stats: AchievementStats, unlocked: Map<String, Long>): List<AchievementDefinition> =
        evaluate(stats, unlocked)
            .filter { it.current >= it.target && it.unlockedAt == null }
            .map { it.definition }
}
