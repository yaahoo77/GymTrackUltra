package com.gymtrack.ultra.features.home

import com.gymtrack.ultra.core.format.Formatters
import com.gymtrack.ultra.ui.components.IskraMood

data class IskraLine(val text: String, val mood: IskraMood)

/**
 * Co mówi Iskra na ekranie Start. Najpierw zdanie dopasowane do dnia, potem — po dotknięciu — wskazówki.
 * Ton: wspierający, bez presji, bez straszenia utratą serii (zgodnie z zasadą "brak kar za odpoczynek").
 */
internal object IskraLines {

    val tips: List<String> = listOf(
        "Dotknij miniaturki ćwiczenia, a pokażę Ci cały ruch.",
        "Tempo: w dół powoli, w górę pewnie.",
        "Ostatnie powtórzenia mają być ciężkie, ale czyste.",
        "„Lżej −10%” to mądry trening, nie porażka.",
        "Dni wolne nie gaszą serii — wystarczą 2 treningi w tygodniu.",
        "Sen i woda robią połowę roboty.",
        "Coś boli? Przerwij. Nie trenujemy przez ból.",
        "Rekord liczy się tylko przy czystej technice.",
        "Białko najłatwiej rozłożyć na 3–4 posiłki. Jajka, skyr i twaróg robią robotę.",
    )

    fun headline(state: HomeUiState): IskraLine {
        val active = state.activeSession
        val today = state.today
        val streak = state.streak
        return when {
            active != null -> IskraLine("${active.title} w toku · ${active.done}/${active.planned} serii. Wracamy?", IskraMood.FOCUS)
            streak.shieldUsedLastWeek -> IskraLine("Tarcza uratowała Twoją serię. Płomień dalej płonie!", IskraMood.CHEER)
            today == null -> IskraLine("Ułóż plan, a ja dopilnuję reszty.", IskraMood.HAPPY)
            today.isToday && today.doneToday -> IskraLine("Zrobione! Teraz regeneracja — to też część treningu.", IskraMood.CHEER)
            today.isToday && streak.isLit -> IskraLine(
                "Dziś ${today.title}. Seria: ${Formatters.countLabel(streak.current, "trening", "treningi", "treningów")} — dokładamy?",
                IskraMood.HAPPY,
            )
            today.isToday -> IskraLine("Dziś ${today.title}. Zaczynamy od rozgrzewki?", IskraMood.HAPPY)
            else -> IskraLine("Dzień wolny. Mięśnie rosną właśnie teraz.", IskraMood.SLEEPY)
        }
    }

    /** Linia numer [index]: 0 = zdanie dnia, potem wskazówki w kółko. */
    fun lineAt(state: HomeUiState, index: Int): IskraLine {
        val head = headline(state)
        if (index <= 0) return head
        val tip = tips[(index - 1) % tips.size]
        val mood = if (head.mood == IskraMood.SLEEPY) IskraMood.HAPPY else head.mood
        return IskraLine(tip, mood)
    }
}
