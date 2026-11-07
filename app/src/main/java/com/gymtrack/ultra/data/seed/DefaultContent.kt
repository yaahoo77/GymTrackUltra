package com.gymtrack.ultra.data.seed

import java.time.DayOfWeek

/** Ćwiczenie wbudowane — źródło danych startowych bazy. */
data class SeedExercise(
    val id: String,
    val namePl: String,
    val nameEn: String,
    val muscleGroup: String,
    val timed: Boolean = false,
    val sideNote: String? = null,
    val technique: List<String>,
    val safety: List<String>,
)

data class SeedPlanItem(
    val exerciseId: String,
    val sets: Int,
    val min: Int,
    val max: Int,
    val restSeconds: Int,
)

data class SeedPlanDay(
    val code: String,
    val title: String,
    val dayOfWeek: DayOfWeek,
    val items: List<SeedPlanItem>,
)

/**
 * Gotowy plan FBW 3 dni + biblioteka 19 ćwiczeń z techniką i bezpieczeństwem.
 * Teksty pisane z myślą także o nastolatkach: zero prób maksymalnych, ból = sygnał, by przerwać.
 */
object DefaultContent {

    private const val PER_SIDE = "na stronę"
    private const val PER_LEG = "na nogę"

    val exercises: List<SeedExercise> = listOf(
        // ── WORKOUT A ──────────────────────────────────────────────────
        SeedExercise(
            id = "goblet_squat",
            namePl = "Przysiad goblet",
            nameEn = "Goblet Squat",
            muscleGroup = "Uda, pośladki, core",
            technique = listOf(
                "Trzymaj hantlę pionowo przy mostku, łokcie skierowane w dół.",
                "Stopy na szerokość barków lub odrobinę szerzej, palce lekko na zewnątrz.",
                "Schodź w dół, siadając między pięty; kolana prowadź w linii palców.",
                "Plecy proste, klatka uniesiona, pięty cały czas na podłodze.",
                "Wstań, wypychając podłogę całą stopą.",
            ),
            safety = listOf(
                "Schodź tylko tak nisko, jak pozwala ci prosta sylwetka.",
                "Kolana nie mogą uciekać do środka.",
                "Ból kolan lub pleców = przerwij serię i zmniejsz ciężar.",
            ),
        ),
        SeedExercise(
            id = "db_bench_press",
            namePl = "Wyciskanie hantli na ławce płaskiej",
            nameEn = "Dumbbell Bench Press",
            muscleGroup = "Klatka, triceps, przód barków",
            technique = listOf(
                "Połóż się na ławce, stopy pewnie na podłodze, łopatki ściągnięte i opuszczone.",
                "Hantle nad klatką, nadgarstki proste.",
                "Opuszczaj powoli do boków klatki, łokcie ok. 45° od tułowia.",
                "Wyciśnij w górę bez uderzania hantlami o siebie.",
            ),
            safety = listOf(
                "Kontroluj ciężar na dole — bez odbijania od klatki.",
                "Po serii usiądź z hantlami na udach; nie upuszczaj ich na boki.",
                "Ból barku = mniejszy zakres i lżejsze hantle.",
            ),
        ),
        SeedExercise(
            id = "seated_cable_row",
            namePl = "Wiosłowanie siedząc na wyciągu",
            nameEn = "Seated Cable Row",
            muscleGroup = "Plecy, tył barków, biceps",
            technique = listOf(
                "Usiądź prosto, kolana lekko ugięte, stopy na podporach.",
                "Zacznij ruch od ściągnięcia łopatek, potem przyciągnij uchwyt do brzucha.",
                "Łokcie prowadź blisko tułowia.",
                "Wracaj powoli, bez zaokrąglania pleców.",
            ),
            safety = listOf(
                "Nie bujaj tułowiem, żeby dociągnąć ciężar.",
                "Plecy neutralne przez cały ruch.",
            ),
        ),
        SeedExercise(
            id = "hip_thrust",
            namePl = "Wypychanie bioder (hip thrust)",
            nameEn = "Hip Thrust",
            muscleGroup = "Pośladki, tył ud",
            technique = listOf(
                "Oprzyj górną część pleców o ławkę, stopy na szerokość bioder.",
                "Ciężar połóż na biodrach — zawsze z miękką podkładką.",
                "Wypchnij biodra w górę, aż tułów i uda utworzą linię.",
                "Na górze zaciśnij pośladki, broda lekko przy klatce.",
                "Opuść biodra pod kontrolą.",
            ),
            safety = listOf(
                "Nie przeprostowuj lędźwi na górze — ruch kończą pośladki.",
                "Pierwsze serie zrób bez obciążenia, żeby poczuć ruch.",
            ),
        ),
        SeedExercise(
            id = "lateral_raise",
            namePl = "Unoszenie hantli bokiem",
            nameEn = "Lateral Raises",
            muscleGroup = "Boczne części barków",
            technique = listOf(
                "Stań prosto, hantle przy udach, łokcie lekko ugięte.",
                "Unieś ręce bokiem do wysokości barków.",
                "Prowadź ruch łokciami, dłonie skierowane w dół.",
                "Opuszczaj wolniej, niż unosisz.",
            ),
            safety = listOf(
                "Lekki ciężar — to małe mięśnie.",
                "Nie podciągaj barków do uszu i nie bujaj tułowiem.",
            ),
        ),
        SeedExercise(
            id = "dead_bug",
            namePl = "Martwy robak (dead bug)",
            nameEn = "Dead Bug",
            muscleGroup = "Brzuch, stabilizacja",
            sideNote = PER_SIDE,
            technique = listOf(
                "Połóż się na plecach, ręce w górę, nogi zgięte 90° w biodrach i kolanach.",
                "Dociśnij lędźwie do podłogi.",
                "Powoli opuść przeciwną rękę i nogę, wydychając powietrze.",
                "Wróć i zmień stronę.",
            ),
            safety = listOf(
                "Jeśli lędźwie odrywają się od podłogi, skróć zakres ruchu.",
                "Ruch powolny, bez szarpania.",
            ),
        ),

        // ── WORKOUT B ──────────────────────────────────────────────────
        SeedExercise(
            id = "db_romanian_deadlift",
            namePl = "Martwy ciąg rumuński z hantlami",
            nameEn = "Dumbbell Romanian Deadlift",
            muscleGroup = "Tył ud, pośladki, prostowniki",
            technique = listOf(
                "Stań na szerokość bioder, hantle przed udami.",
                "Lekko ugnij kolana i utrzymaj ten kąt przez cały ruch.",
                "Cofaj biodra do tyłu, hantle suną blisko nóg.",
                "Zejdź do wyraźnego rozciągnięcia z tyłu ud, plecy proste.",
                "Wróć, wypychając biodra do przodu.",
            ),
            safety = listOf(
                "Plecy nigdy się nie zaokrąglają — skróć zakres, jeśli trzeba.",
                "Hantle cały czas blisko ciała.",
                "Ból krzyża = przerwij i zgłoś to trenerowi lub dorosłemu.",
            ),
        ),
        SeedExercise(
            id = "lat_pulldown",
            namePl = "Ściąganie drążka wyciągu do klatki",
            nameEn = "Lat Pulldown",
            muscleGroup = "Najszersze grzbietu, biceps",
            technique = listOf(
                "Usiądź, zablokuj uda pod wałkami.",
                "Chwyć drążek nieco szerzej niż barki.",
                "Ściągnij łopatki w dół, potem przyciągnij drążek do górnej części klatki.",
                "Wracaj powoli do pełnego wyprostu rąk.",
            ),
            safety = listOf(
                "Nie ściągaj drążka za głowę.",
                "Nie odchylaj się mocno do tyłu.",
            ),
        ),
        SeedExercise(
            id = "lunges",
            namePl = "Wykroki",
            nameEn = "Lunges",
            muscleGroup = "Uda, pośladki, równowaga",
            sideNote = PER_LEG,
            technique = listOf(
                "Stań prosto i zrób krok w przód.",
                "Opuść tylne kolano w stronę podłogi; przednie kolano nad stopą.",
                "Tułów pionowo.",
                "Odepchnij się przednią nogą i wróć do pozycji wyjściowej.",
            ),
            safety = listOf(
                "Zacznij bez ciężaru; trzymaj się czegoś stabilnego, jeśli tracisz równowagę.",
                "Przednie kolano nie ucieka do środka.",
            ),
        ),
        SeedExercise(
            id = "seated_db_shoulder_press",
            namePl = "Wyciskanie hantli nad głowę siedząc",
            nameEn = "Seated Dumbbell Shoulder Press",
            muscleGroup = "Barki, triceps",
            technique = listOf(
                "Usiądź na ławce z oparciem, hantle na wysokości barków.",
                "Wyciśnij w górę, aż ręce będą prawie wyprostowane.",
                "Opuść powoli do wysokości barków.",
                "Brzuch napięty, plecy przy oparciu.",
            ),
            safety = listOf(
                "Nie wyginaj lędźwi w łuk.",
                "Ból barku = mniejszy ciężar lub krótszy zakres.",
            ),
        ),
        SeedExercise(
            id = "face_pull",
            namePl = "Przyciąganie liny do twarzy (face pull)",
            nameEn = "Face Pull",
            muscleGroup = "Tył barków, góra pleców",
            technique = listOf(
                "Ustaw wyciąg na wysokości twarzy, chwyć linę nachwytem.",
                "Przyciągnij linę w kierunku czoła, rozciągając końce na boki.",
                "Łokcie wysoko, łopatki ściągnięte.",
                "Wracaj powoli.",
            ),
            safety = listOf(
                "Lekki ciężar, pełna kontrola.",
                "Nie odchylaj się do tyłu.",
            ),
        ),
        SeedExercise(
            id = "side_plank",
            namePl = "Deska bokiem",
            nameEn = "Side Plank",
            muscleGroup = "Mięśnie skośne brzucha, stabilizacja",
            timed = true,
            sideNote = PER_SIDE,
            technique = listOf(
                "Połóż się na boku, łokieć dokładnie pod barkiem.",
                "Unieś biodra — ciało w linii prostej od głowy do stóp.",
                "Oddychaj spokojnie, nie wstrzymuj powietrza.",
            ),
            safety = listOf(
                "Łatwiejsza wersja: kolana na podłodze.",
                "Ból barku = przerwij.",
            ),
        ),

        // ── WORKOUT C ──────────────────────────────────────────────────
        SeedExercise(
            id = "leg_press",
            namePl = "Wypychanie nóg na suwnicy",
            nameEn = "Leg Press",
            muscleGroup = "Uda, pośladki",
            technique = listOf(
                "Usiądź, plecy i głowa przy oparciu, stopy na platformie na szerokość bioder.",
                "Zwolnij blokady i opuść platformę, zginając kolana.",
                "Zejdź do ok. 90° w kolanach; lędźwie zostają przy oparciu.",
                "Wypchnij całą stopą, bez blokowania kolan na górze.",
            ),
            safety = listOf(
                "Nie prostuj kolan do końca — bez \"zatrzaśnięcia\".",
                "Nie schodź tak nisko, że miednica odrywa się od oparcia.",
                "Po serii zawsze zamknij blokady.",
            ),
        ),
        SeedExercise(
            id = "incline_db_press",
            namePl = "Wyciskanie hantli na ławce skośnej",
            nameEn = "Incline Dumbbell Press",
            muscleGroup = "Górna część klatki, barki, triceps",
            technique = listOf(
                "Ustaw ławkę pod kątem 30–45°.",
                "Hantle nad górną częścią klatki, łopatki ściągnięte.",
                "Opuść powoli do boków klatki.",
                "Wyciśnij w górę bez uderzania hantlami.",
            ),
            safety = listOf(
                "Kontroluj ciężar na dole.",
                "Po serii usiądź z hantlami na udach.",
            ),
        ),
        SeedExercise(
            id = "chest_supported_row",
            namePl = "Wiosłowanie hantlami w oparciu o ławkę",
            nameEn = "Chest Supported Row",
            muscleGroup = "Plecy, tył barków",
            technique = listOf(
                "Połóż się klatką na ławce skośnej, hantle zwisają swobodnie.",
                "Przyciągnij hantle do bioder, łokcie blisko ciała.",
                "Na górze ściągnij łopatki.",
                "Opuść powoli.",
            ),
            safety = listOf(
                "Klatka cały czas na ławce.",
                "Nie szarp ciężarem.",
            ),
        ),
        SeedExercise(
            id = "leg_curl",
            namePl = "Uginanie nóg na maszynie",
            nameEn = "Leg Curl",
            muscleGroup = "Tył ud",
            technique = listOf(
                "Ustaw wałek tuż nad piętami.",
                "Ugnij kolana, przyciągając wałek w stronę pośladków.",
                "Wracaj powoli.",
            ),
            safety = listOf(
                "Bez szarpania i odbijania.",
                "Biodra dociśnięte do siedziska lub ławki.",
            ),
        ),
        SeedExercise(
            id = "db_bicep_curl",
            namePl = "Uginanie ramion z hantlami",
            nameEn = "Dumbbell Bicep Curl",
            muscleGroup = "Biceps",
            technique = listOf(
                "Stań prosto, łokcie przy tułowiu.",
                "Uginaj ręce, obracając dłonie do góry.",
                "Opuść powoli do pełnego wyprostu.",
            ),
            safety = listOf(
                "Nie bujaj tułowiem.",
                "Łokcie nie wędrują do przodu.",
            ),
        ),
        SeedExercise(
            id = "tricep_pushdown",
            namePl = "Prostowanie ramion na wyciągu",
            nameEn = "Tricep Pushdown",
            muscleGroup = "Triceps",
            technique = listOf(
                "Stań przodem do wyciągu, chwyć drążek lub linę.",
                "Łokcie przy bokach — prostuj ręce w dół.",
                "Wracaj powoli do kąta ok. 90°.",
            ),
            safety = listOf(
                "Łokcie zostają w miejscu.",
                "Nie pochylaj się nad ciężarem.",
            ),
        ),
        SeedExercise(
            id = "plank",
            namePl = "Deska (plank)",
            nameEn = "Plank",
            muscleGroup = "Brzuch, stabilizacja",
            timed = true,
            technique = listOf(
                "Przedramiona na podłodze, łokcie pod barkami.",
                "Ciało w linii prostej, brzuch i pośladki napięte.",
                "Oddychaj spokojnie.",
            ),
            safety = listOf(
                "Biodra ani nie opadają, ani nie idą wysoko.",
                "Zacznij od krótszych serii i wydłużaj stopniowo.",
            ),
        ),
    )

    val plan: List<SeedPlanDay> = listOf(
        SeedPlanDay(
            code = "A",
            title = "WORKOUT A",
            dayOfWeek = DayOfWeek.MONDAY,
            items = listOf(
                SeedPlanItem("goblet_squat", 3, 8, 10, 120),
                SeedPlanItem("db_bench_press", 3, 8, 12, 120),
                SeedPlanItem("seated_cable_row", 3, 10, 12, 90),
                SeedPlanItem("hip_thrust", 2, 10, 12, 90),
                SeedPlanItem("lateral_raise", 2, 12, 15, 60),
                SeedPlanItem("dead_bug", 2, 8, 10, 60),
            ),
        ),
        SeedPlanDay(
            code = "B",
            title = "WORKOUT B",
            dayOfWeek = DayOfWeek.WEDNESDAY,
            items = listOf(
                SeedPlanItem("db_romanian_deadlift", 3, 8, 10, 120),
                SeedPlanItem("lat_pulldown", 3, 8, 12, 90),
                SeedPlanItem("lunges", 2, 8, 10, 90),
                SeedPlanItem("seated_db_shoulder_press", 2, 8, 12, 90),
                SeedPlanItem("face_pull", 2, 12, 15, 60),
                SeedPlanItem("side_plank", 2, 20, 30, 60),
            ),
        ),
        SeedPlanDay(
            code = "C",
            title = "WORKOUT C",
            dayOfWeek = DayOfWeek.FRIDAY,
            items = listOf(
                SeedPlanItem("leg_press", 3, 10, 12, 120),
                SeedPlanItem("incline_db_press", 3, 8, 12, 120),
                SeedPlanItem("chest_supported_row", 3, 10, 12, 90),
                SeedPlanItem("leg_curl", 2, 10, 15, 60),
                SeedPlanItem("db_bicep_curl", 2, 10, 12, 60),
                SeedPlanItem("tricep_pushdown", 2, 10, 12, 60),
                SeedPlanItem("plank", 2, 20, 40, 60),
            ),
        ),
    )
}
