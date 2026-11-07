# GYMTRACK ULTRA

Natywna aplikacja treningowa na Androida — Kotlin, Jetpack Compose, Material 3, Room, DataStore, Hilt.
Styl **Cyber Lime Dark**: prawie czarne tło, jeden neonowy akcent, duże liczby, satysfakcjonujące mikrointerakcje.
Działa w 100% offline, bez konta, wszystkie dane zostają na telefonie.

## Stan projektu

| Etap | Zakres | Status |
|---|---|---|
| 1 | Koncepcja wizualna, design system | ✅ (kanwa Design) |
| 2 | Makiety kluczowych ekranów | ✅ (kanwa Design, część klikalna) |
| 3 | Modele danych, Room, DataStore, architektura, nawigacja, logika XP / rang / osiągnięć | ✅ ten pakiet |
| 4 | Pełny UI: onboarding, Live Workout, Rest Timer, Workout Complete | ⏭ następny |
| 5 | Zapis serii i sesji, przywracanie treningu, timer w tle (foreground service), statystyki z wykresami, eksport/import | ⏭ |
| 6 | Dopamine engine: haptyka, drobinki, konfetti, awans, karty osiągnięć | ⏭ |
| 7 | Testy instrumentacyjne, edge case'y, APK release | ⏭ |

Po Etapie 3 aplikacja **uruchamia się i działa**: dolny pasek z 5 zakładkami, Home z dzisiejszym treningiem z bazy,
tygodniem, poziomem i XP; plan FBW A/B/C z kartami techniki i bezpieczeństwa (19 ćwiczeń po polsku);
Progress Lab z filtrami 7/30/90/cały okres (liczy tylko prawdziwe dane, przy braku historii pokazuje pusty stan);
kolekcja 12 osiągnięć z postępem; profil z rangą i ustawieniami (ograniczenie animacji, haptyka, celebracje, rozgrzewka).

Przycisk na karcie dnia to na razie **ZOBACZ TRENING** (podgląd dnia + technika). Tryb Live Workout z zapisem serii
dochodzi w Etapie 4 — wtedy przycisk staje się START WORKOUT.

## Jak uruchomić

Wymagania: Android Studio (aktualne stabilne), JDK 17+, Android SDK Platform 36.

1. Rozpakuj archiwum, w Android Studio wybierz **File → Open** i wskaż folder `GymTrackUltra`.
2. Poczekaj na **Gradle Sync** (pierwszy raz pobiera zależności — potrzebny internet).
3. Wybierz emulator lub telefon (Android 8.0+) i kliknij **Run ▶**.

Z wiersza poleceń:

```bash
./gradlew assembleDebug          # APK: app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest      # testy logiki (XP, poziomy, osiągnięcia, tydzień, Home)
./gradlew installDebug           # instalacja na podłączonym telefonie
```

> Jeśli Android Studio zaproponuje aktualizację AGP do 9.x — na razie odmów. Hilt 2.59+ wymaga AGP 9,
> a zestaw w `gradle/libs.versions.toml` jest dobrany tak, by wszystko było ze sobą zgodne.

## APK bez Android Studio (GitHub Actions)

Po wrzuceniu projektu na GitHub workflow `.github/workflows/build-apk.yml` sam uruchamia testy, buduje APK
i publikuje go jako release `latest`. Na telefonie wystarczy otworzyć:

`https://github.com/<użytkownik>/<repo>/releases/latest/download/GymTrackUltra.apk`

Build jest podpisany stałym kluczem debug (`app/debug.keystore`), więc kolejne wersje instalują się jako
aktualizacja — bez kasowania zapisanych treningów. To klucz tylko do testów, nie do publikacji w Google Play.

## Architektura

```
UI (Compose)  ──►  ViewModel (StateFlow)  ──►  Repozytorium (interfejs w domain)
                                                  │
                                       data/ (implementacje)
                                        ├── Room  (plan, sesje, serie, XP, osiągnięcia)
                                        └── DataStore (ustawienia)
domain/ = czysty Kotlin: modele, zasady XP, poziomy, osiągnięcia, planowanie dnia — bez Androida, w pełni testowalne.
```

```
app/src/main/java/com/gymtrack/ultra/
├── GymTrackApp.kt · MainActivity.kt · MainViewModel.kt · GymTrackRoot.kt
├── core/
│   ├── di/            DataModule (Room, DataStore) · BindingsModule (repozytoria, czas)
│   ├── format/        Formatters — polskie dni, odmiana, czas, kg z przecinkiem
│   └── time/          TimeProvider · WeekMath (tygodnie ISO, serie tygodniowe)
├── database/
│   ├── GymDatabase.kt
│   ├── entity/        Entities.kt — 9 tabel
│   ├── relation/      Relations.kt — plan z ćwiczeniami, sesja z seriami
│   └── dao/           ExerciseDao · PlanDao · SessionDao · ProgressDao
├── data/
│   ├── seed/          DefaultContent (plan FBW + 19 ćwiczeń) · DatabaseSeeder
│   ├── mapper/        encje ↔ modele domeny
│   ├── repository/    PlanRepositoryImpl · ProgressRepositoryImpl
│   └── settings/      SettingsRepositoryImpl (DataStore)
├── domain/
│   ├── model/         Exercise · Plan · Session · Settings · Progress
│   ├── plan/          TodayPlanner
│   ├── progression/   LevelSystem (rangi) · XpCalculator · Achievements · AchievementStatsBuilder
│   └── repository/    interfejsy repozytoriów
├── navigation/        Routes (trasy typowane) · GymAppScaffold (dolny pasek + NavHost)
├── features/
│   ├── home/          Command Center
│   ├── workout/       plan A/B/C · podgląd dnia · karta techniki
│   ├── statistics/    Progress Lab
│   ├── achievements/  kolekcja
│   └── profile/       ranga + ustawienia
└── ui/
    ├── theme/         Color · Type · Shape · Motion (ograniczenie ruchu) · Theme
    └── components/    NeonButton · GtCard · GradientProgressBar · HexBadge · GtIcons · stany puste/ładowania
```

## Baza danych (Room, wersja 1)

| Tabela | Po co |
|---|---|
| `exercises` | biblioteka ćwiczeń: nazwa PL/EN, partia, technika, bezpieczeństwo, powtórzenia vs czas |
| `plan_days`, `plan_exercises` | edytowalny plan (dzień tygodnia, serie, zakres, przerwa) |
| `workout_sessions` | sesje ze statusem `ACTIVE` / `COMPLETED` / `ABANDONED` — `ACTIVE` pozwala przywrócić przerwany trening |
| `session_exercises` | kopia celu w chwili startu — późniejsza edycja planu nie psuje historii |
| `set_logs` | ciężar, powtórzenia lub czas, zaliczenie, znacznik czasu |
| `xp_events` | księga XP — każdy wpis osobno (limit dzienny, premia tygodniowa) |
| `achievement_unlocks`, `technique_views` | odblokowania i przeczytane techniki |

Bez `fallbackToDestructiveMigration` — każda zmiana schematu dostanie jawną migrację. Schematy trafiają do `app/schemas/`.

## Progresja — zasady

**XP za trening** (maks. 120): +50 za ukończenie (≥ 50% planowanych serii), do +50 proporcjonalnie do realizacji planu,
+10 za rozgrzewkę, +10 za komplet serii. Serie ponad plan dają 0 XP. XP za trening liczy się raz dziennie.
+50 raz w tygodniu za osiągnięcie celu tygodnia. Pominięty dzień nic nie odbiera.

**Poziomy**: przejście z n na n+1 kosztuje `100 + 25·(n−1)` XP. Przy ~3 treningach tygodniowo LVL 5 ≈ 2 tygodnie,
LVL 10 ≈ 5 tygodni, LVL 50 ≈ 1,5–2 lata.

**Rangi** (progi ze specyfikacji, nazwy autorskie): IGNITE (1) · PATHFINDER (5) · FORGE (10) · VANGUARD (20) ·
COLOSSUS (30) · ASCENDANT (50). Nazwy i motta zmienisz w `domain/progression/LevelSystem.kt`.

**Osiągnięcia** (12): FIRST REP, FIRST WORKOUT, WARM-UP RITUAL, FULL CYCLE, 10/50/100 WORKOUTS,
CONSISTENT BUILDER (4 tyg. z rzędu ≥ 2 treningi), STEADY QUARTER (12 tyg.), SMART LOAD (świadome zmniejszenie
ciężaru), TECHNIQUE MASTER (technika wszystkich ćwiczeń z planu), PLAN ARCHITECT. Bez rankingów i porównywania ciał.

## Dostępność i ruch

- Systemowe „usuń animacje” i przełącznik w Profilu → tylko krótkie przenikania, bez sprężyn, drobinek i konfetti.
- Cele dotyku ≥ 48 dp, etykiety TalkBack dla zakładek, dni tygodnia, odznak i przełączników.
- Cyfry tabelaryczne — liczniki nie „skaczą” w poziomie.

## Typografia

Makiety używają Space Grotesk (nagłówki, liczby) i Inter (treść). Kod korzysta na razie z systemowego sans-serif.
Żeby podpiąć fonty: pobierz pliki `.ttf` z fonts.google.com do `app/src/main/res/font/`
i zmień `DisplayFamily` / `BodyFamily` w `ui/theme/Type.kt`.
