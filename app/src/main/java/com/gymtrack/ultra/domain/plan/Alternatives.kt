package com.gymtrack.ultra.domain.plan

import com.gymtrack.ultra.domain.model.Exercise
import com.gymtrack.ultra.domain.progression.MuscleGroup
import com.gymtrack.ultra.domain.progression.MuscleMap

/**
 * Zamienniki ćwiczeń — gdy maszyna zajęta albo jej nie ma. Najpierw ręcznie dobrane odpowiedniki,
 * potem reszta biblioteki na tę samą partię. Ćwiczenia "nietypowe" nie są podpowiadane jako zamiennik.
 */
object ExerciseAlternatives {

    val unusual: Set<String> = setOf("hip_thrust", "dead_bug", "side_plank", "face_pull", "lunges", "chest_supported_row")

    private val curated: Map<String, List<String>> = mapOf(
        "leg_press" to listOf("goblet_squat", "leg_extension"),
        "goblet_squat" to listOf("leg_press", "leg_extension"),
        "leg_extension" to listOf("leg_press", "goblet_squat"),
        "leg_curl" to listOf("db_romanian_deadlift"),
        "db_romanian_deadlift" to listOf("leg_curl"),
        "db_bench_press" to listOf("chest_press_machine", "incline_db_press"),
        "chest_press_machine" to listOf("db_bench_press", "incline_db_press"),
        "incline_db_press" to listOf("db_bench_press", "chest_press_machine"),
        "lat_pulldown" to listOf("seated_cable_row", "one_arm_db_row"),
        "seated_cable_row" to listOf("one_arm_db_row", "lat_pulldown"),
        "one_arm_db_row" to listOf("seated_cable_row", "lat_pulldown"),
        "seated_db_shoulder_press" to listOf("lateral_raise"),
        "lateral_raise" to listOf("seated_db_shoulder_press"),
        "db_bicep_curl" to listOf("hammer_curl"),
        "hammer_curl" to listOf("db_bicep_curl"),
        "tricep_pushdown" to listOf("overhead_tricep_extension"),
        "overhead_tricep_extension" to listOf("tricep_pushdown"),
        "plank" to listOf("knee_raise"),
        "knee_raise" to listOf("plank"),
        "hip_thrust" to listOf("db_romanian_deadlift", "goblet_squat"),
        "dead_bug" to listOf("plank", "knee_raise"),
        "side_plank" to listOf("plank", "knee_raise"),
        "face_pull" to listOf("lateral_raise", "one_arm_db_row"),
        "lunges" to listOf("goblet_squat", "leg_press"),
        "chest_supported_row" to listOf("one_arm_db_row", "seated_cable_row"),
    )

    /** Zamienniki dla [currentId] w kolejności: dobrane ręcznie → ta sama partia (także własne ćwiczenia). */
    fun forExercise(currentId: String, library: List<Exercise>): List<Exercise> {
        val byId = library.associateBy { it.id }
        val picked = curated[currentId].orEmpty().mapNotNull { byId[it] }
        val primary = MuscleMap.targetsOf(currentId).primary
        val sameMuscle = library.filter { candidate ->
            candidate.id != currentId &&
                candidate.id !in unusual &&
                candidate !in picked &&
                MuscleMap.targetsOf(candidate.id).primary.any { it in primary }
        }
        return picked + sameMuscle
    }

    /** Cała biblioteka pogrupowana po głównej partii (do dodawania ćwiczeń do planu). */
    fun grouped(library: List<Exercise>): List<Pair<MuscleGroup, List<Exercise>>> =
        MuscleGroup.entries.mapNotNull { muscle ->
            val items = library
                .filter { MuscleMap.targetsOf(it.id).primary.firstOrNull() == muscle }
                .sortedWith(compareBy<Exercise> { it.id in unusual }.thenBy { it.namePl })
            if (items.isEmpty()) null else muscle to items
        }
}

/** Własne ćwiczenia: partia zapisana w id, dzięki czemu rangi mięśni działają bez zmian w bazie. */
object CustomExercises {
    private const val PREFIX = "custom_"

    fun newId(muscle: MuscleGroup, nowMillis: Long): String = "$PREFIX${muscle.name}_$nowMillis"

    fun isCustom(id: String): Boolean = id.startsWith(PREFIX)

    fun muscleOf(id: String): MuscleGroup? {
        if (!isCustom(id)) return null
        val name = id.removePrefix(PREFIX).substringBefore('_')
        return MuscleGroup.entries.firstOrNull { it.name == name }
    }
}
