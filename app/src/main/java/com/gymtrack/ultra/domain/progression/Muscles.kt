package com.gymtrack.ultra.domain.progression

enum class MuscleGroup(val label: String) {
    CHEST("Klatka"),
    BACK("Plecy"),
    SHOULDERS("Barki"),
    BICEPS("Biceps"),
    TRICEPS("Triceps"),
    LEGS("Nogi"),
    GLUTES("Pośladki"),
    CORE("Brzuch"),
}

data class MuscleTargets(
    val primary: Set<MuscleGroup>,
    val secondary: Set<MuscleGroup> = emptySet(),
)

/** Które partie pracują w każdym wbudowanym ćwiczeniu. */
object MuscleMap {
    private val map: Map<String, MuscleTargets> = mapOf(
        "goblet_squat" to MuscleTargets(setOf(MuscleGroup.LEGS), setOf(MuscleGroup.GLUTES, MuscleGroup.CORE)),
        "db_bench_press" to MuscleTargets(setOf(MuscleGroup.CHEST), setOf(MuscleGroup.TRICEPS, MuscleGroup.SHOULDERS)),
        "seated_cable_row" to MuscleTargets(setOf(MuscleGroup.BACK), setOf(MuscleGroup.BICEPS)),
        "hip_thrust" to MuscleTargets(setOf(MuscleGroup.GLUTES), setOf(MuscleGroup.LEGS)),
        "lateral_raise" to MuscleTargets(setOf(MuscleGroup.SHOULDERS)),
        "dead_bug" to MuscleTargets(setOf(MuscleGroup.CORE)),
        "db_romanian_deadlift" to MuscleTargets(setOf(MuscleGroup.LEGS, MuscleGroup.GLUTES), setOf(MuscleGroup.BACK)),
        "lat_pulldown" to MuscleTargets(setOf(MuscleGroup.BACK), setOf(MuscleGroup.BICEPS)),
        "lunges" to MuscleTargets(setOf(MuscleGroup.LEGS), setOf(MuscleGroup.GLUTES)),
        "seated_db_shoulder_press" to MuscleTargets(setOf(MuscleGroup.SHOULDERS), setOf(MuscleGroup.TRICEPS)),
        "face_pull" to MuscleTargets(setOf(MuscleGroup.SHOULDERS), setOf(MuscleGroup.BACK)),
        "side_plank" to MuscleTargets(setOf(MuscleGroup.CORE)),
        "leg_press" to MuscleTargets(setOf(MuscleGroup.LEGS), setOf(MuscleGroup.GLUTES)),
        "incline_db_press" to MuscleTargets(setOf(MuscleGroup.CHEST), setOf(MuscleGroup.SHOULDERS, MuscleGroup.TRICEPS)),
        "chest_supported_row" to MuscleTargets(setOf(MuscleGroup.BACK), setOf(MuscleGroup.BICEPS, MuscleGroup.SHOULDERS)),
        "leg_curl" to MuscleTargets(setOf(MuscleGroup.LEGS)),
        "db_bicep_curl" to MuscleTargets(setOf(MuscleGroup.BICEPS)),
        "tricep_pushdown" to MuscleTargets(setOf(MuscleGroup.TRICEPS)),
        "plank" to MuscleTargets(setOf(MuscleGroup.CORE)),
    )

    fun targetsOf(exerciseId: String): MuscleTargets = map[exerciseId] ?: MuscleTargets(emptySet())

    fun exercisesTraining(muscle: MuscleGroup): List<String> =
        map.filterValues { muscle in it.primary }.keys.toList()
}
