# GYMTRACK ULTRA

Natywna aplikacja treningowa na Androida — Kotlin, Jetpack Compose, Material 3, Room, DataStore, Hilt.
Styl **Cyber Lime Dark**: prawie czarne tło, jeden neonowy akcent, duże liczby, satysfakcjonujące mikrointerakcje.
Działa w 100% offline, bez konta, wszystkie dane zostają na telefonie.

## Stan projektu (v0.7)

| Etap | Zakres | Status |
|---|---|---|
| 1–2 | Koncepcja, design system, makiety | ✅ |
| 3 | Room, DataStore, architektura, nawigacja, logika XP | ✅ |
| 4–5 | Trening na żywo, zapis serii, timer przerwy w tle, podsumowanie | ✅ |
| 6 | Gamifikacja: rangi, seria z płomieniem, wyzwania, rekordy, awanse | ✅ (część) |
| 6 | Animacje ćwiczeń, maskotka Iskra, „życie” w interfejsie | ✅ |
| — | Onboarding, wykresy w Statystykach, eksport/import, mapa mięśni | ⏭ następne |

### Co działa

- **START WORKOUT** tworzy sesję z planu i otwiera trening na żywo: ciężar podpowiadany z ostatniego razu,
  duży przycisk zaliczenia z wibracją i drobinkami, COFNIJ, +seria, „Lżej −10%”, notatka, technika.
- **Timer przerwy** startuje sam, ±15 s, pomiń, pełny ekran z pierścieniem. Działa w tle dzięki usłudze
  pierwszoplanowej — powiadomienie z odliczaniem i wibracją na koniec przerwy.
- **Przerwany trening** wraca po ponownym otwarciu aplikacji (karta „Trening w toku”).
- **Podsumowanie**: WORKOUT COMPLETE z liczeniem XP, konfetti, rekordy, punkty rang, wyzwania, osiągnięcia,
  porównanie objętości → **awans rangi** (odznaka obraca się i ląduje) → **ekran serii** (płomień rośnie, licznik przeskakuje).
- **Rangi**: drabinka Żelazo → Brąz → Srebro → Złoto → Platyna → Szmaragd → Diament → Mistrz → Legenda (I–III).
  Ranga konta z poziomu, osobna ranga dla 8 partii mięśni. Autorskie odznaki rysowane w Compose.
- **Seria**: liczy treningi; wystarczą 2 w tygodniu, dni wolne nie przerywają. Tarcza za każde 4 pełne tygodnie.
- **Wyzwania tygodnia**: 3 rotujące zadania z nagrodą XP, liczone z prawdziwych danych.
- **Plan z klasyków**: FBW A/B/C wyłącznie z ćwiczeń, które robi każdy na siłowni (suwnica, wyciskanie hantli,
  ściąganie drążka, maszyna do klatki, wiosłowanie, prostowanie/uginanie nóg, uginanie ramion, unoszenie kolan, deska).
  Stary plan z v0.1–v0.5 podmienia się sam przy aktualizacji; historia zostaje.
- **Animacje ćwiczeń**: każde z 24 ćwiczeń w bibliotece ma własną animowaną postać-manekina (karta techniki, trening
  na żywo, plan). Pracujące partie świecą na lime (klatka, plecy, brzuch, pośladki, barki, ręce, uda), tempo pokazuje
  kontrolowane opuszczanie, pod spodem podpis fazy ruchu. Postać liczona jest odwrotną kinematyką z dwóch póz.
- **Białko**: podajesz wagę (opcjonalnie) i cel — trening siłowy 1,6 g/kg albo podstawowy 1,2 g/kg
  (ISSN 2017: 1,4–2,0 g/kg dla trenujących; Morton i in. 2018: plateau ok. 1,6 g/kg) — albo wpisujesz cel ręcznie.
  Karta „Białko dziś” na Start, dziennik z 20 typowymi produktami jednym kliknięciem, własne wpisy, cofanie,
  wykres 7 dni. Bez kalorii i bez wykresów wagi. Waga zostaje na telefonie i da się ją usunąć.
- **Iskra**: płomyk-maskotka na ekranie Start — mruga, kołysze się, mówi, co dziś w planie, po dotknięciu podskakuje
  i podaje wskazówkę. Na podsumowaniu treningu skacze z radości. W dzień wolny drzemie.
- **Więcej życia**: karty wjeżdżają po kolei, paski i liczniki napełniają się od zera, połysk na odznakach rang
  i przycisku START, podskakujące ikony w dolnym pasku, puls dzisiejszego dnia, automatyczne przewijanie
  do następnego ćwiczenia w trakcie treningu. Wszystko wyłącza się przy ograniczonym ruchu.

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
│   ├── seed/          DefaultContent (plan FBW + 24 ćwiczenia) · DatabaseSeeder (podmiana starego planu)
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
    ├── components/    NeonButton · GtCard · GradientProgressBar · Iskra · Life (połysk, wjazd, liczniki) · …
    ├── rank/          odznaki rang rysowane w Compose
    └── exercise/      ExerciseRig (szkielet + IK) · ExerciseMotions (24 ćwiczenia) · ExerciseDrawing · ExerciseAnimation
```

## Baza danych (Room, wersja 2)

| Tabela | Po co |
|---|---|
| `exercises` | biblioteka ćwiczeń: nazwa PL/EN, partia, technika, bezpieczeństwo, powtórzenia vs czas |
| `plan_days`, `plan_exercises` | edytowalny plan (dzień tygodnia, serie, zakres, przerwa) |
| `workout_sessions` | sesje ze statusem `ACTIVE` / `COMPLETED` / `ABANDONED` — `ACTIVE` pozwala przywrócić przerwany trening |
| `session_exercises` | kopia celu w chwili startu — późniejsza edycja planu nie psuje historii |
| `set_logs` | ciężar, powtórzenia lub czas, zaliczenie, znacznik czasu |
| `xp_events` | księga XP — każdy wpis osobno (limit dzienny, premia tygodniowa) |
| `achievement_unlocks`, `technique_views` | odblokowania i przeczytane techniki |
| `food_entries` | dziennik białka: dzień, nazwa, gramy białka (migracja 1 → 2 w `Migrations.kt`) |

Bez `fallbackToDestructiveMigration` — każda zmiana schematu dostanie jawną migrację. Schematy trafiają do `app/schemas/`.

## Progresja — zasady

**XP za trening** (maks. 120): +50 za ukończenie (≥ 50% planowanych serii), do +50 proporcjonalnie do realizacji planu,
+10 za rozgrzewkę, +10 za komplet serii. Serie ponad plan dają 0 XP. XP za trening liczy się raz dziennie.
+50 raz w tygodniu za osiągnięcie celu tygodnia. Pominięty dzień nic nie odbiera.

**Poziomy**: przejście z n na n+1 kosztuje `100 + 25·(n−1)` XP. Przy ~3 treningach tygodniowo LVL 5 ≈ 2 tygodnie,
LVL 10 ≈ 5 tygodni, LVL 50 ≈ 1,5–2 lata.

**Rangi konta**: z poziomu — LVL 1 Żelazo I, LVL 10 Brąz II, LVL 20 Złoto I, LVL 30 Platyna III, LVL 50 Legenda.

**Rangi mięśni**: seria z planu +10 pkt (partia główna) / +5 (pomocnicza), rekord osobisty +15, maks. 60 pkt
na trening. Dywizja n → n+1 kosztuje 40 + 15·n pkt (Złoto ~3 mies., Legenda ~1,5 roku regularnych treningów).

**Rekordy osobiste**: rekord ciężaru tylko przy czystej serii (powtórzenia ≥ dolnej granicy zakresu),
powtórzenia ponad górną granicę nie dają rekordu — to sygnał, żeby dołożyć ciężaru, a nie iść do upadku.

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
