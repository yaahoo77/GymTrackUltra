package com.gymtrack.ultra.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.gymtrack.ultra.MainActivity
import com.gymtrack.ultra.core.time.TimeProvider
import com.gymtrack.ultra.domain.repository.NutritionRepository
import com.gymtrack.ultra.domain.repository.PlanRepository
import com.gymtrack.ultra.domain.repository.ProgressRepository
import com.gymtrack.ultra.domain.repository.SettingsRepository
import com.gymtrack.ultra.features.home.HomeStateFactory
import com.gymtrack.ultra.features.nutrition.ProteinStateFactory
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first

/** Dostęp do repozytoriów z widżetu (widżet nie jest tworzony przez Hilt). */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun plan(): PlanRepository
    fun progress(): ProgressRepository
    fun settings(): SettingsRepository
    fun nutrition(): NutritionRepository
    fun time(): TimeProvider
}

/** Co pokazuje widżet — liczone z tych samych danych co ekran Start. */
internal data class WidgetData(
    val headline: String,
    val detail: String,
    val streak: Int,
    val streakLit: Boolean,
    val proteinEaten: Int?,
    val proteinTarget: Int?,
)

private val Bg = Color(0xFF1B1D21)
private val Lime = Color(0xFFB6FF4D)
private val Fire = Color(0xFFFF8A2B)
private val TextPrimary = Color(0xFFF4F5F6)
private val TextSecondary = Color(0xFF9AA1A8)
private val Track = Color(0xFF25282D)

class GymWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = runCatching { load(context) }.getOrNull()
        provideContent { WidgetContent(data) }
    }

    private suspend fun load(context: Context): WidgetData {
        val entry = EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
        val time = entry.time()
        val settings = entry.settings().settings.first()
        val plan = entry.plan().observePlan().first()
        val progress = entry.progress().observeProgress().first()
        val food = entry.nutrition().observeEntries(1).first()
        val home = HomeStateFactory.build(plan, progress, settings, time.now(), time.zone())
        val today = home.today
        val (headline, detail) = when {
            today == null -> "Brak planu" to "Otwórz aplikację, żeby go ułożyć"
            today.isToday && today.doneToday -> "Trening zrobiony" to "Teraz regeneracja"
            today.isToday -> "Dziś: ${today.title}" to "${today.exerciseCount} ćwiczeń · ~${today.minutes} min"
            else -> "Dzień wolny" to "Następny: ${today.title}, ${today.whenText}"
        }
        val protein = ProteinStateFactory.card(settings, food, time.today())
        return WidgetData(
            headline = headline,
            detail = detail,
            streak = home.streak.current,
            streakLit = home.streak.isLit,
            proteinEaten = protein?.eaten,
            proteinTarget = protein?.target,
        )
    }
}

@Composable
private fun WidgetContent(data: WidgetData?) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(20.dp)
            .background(Bg)
            .padding(14.dp)
            .clickable(actionStartActivity<MainActivity>()),
    ) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "GYMTRACK",
                style = TextStyle(color = ColorProvider(Lime), fontSize = 11.sp, fontWeight = FontWeight.Bold),
                modifier = GlanceModifier.defaultWeight(),
            )
            if (data != null) {
                Text(
                    if (data.streakLit) "🔥 ${data.streak}" else "🔥 0",
                    style = TextStyle(
                        color = ColorProvider(if (data.streakLit) Fire else TextSecondary),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }
        Spacer(GlanceModifier.height(8.dp))
        Text(
            data?.headline ?: "GymTrack Ultra",
            style = TextStyle(color = ColorProvider(TextPrimary), fontSize = 17.sp, fontWeight = FontWeight.Bold),
            maxLines = 1,
        )
        Text(
            data?.detail ?: "Dotknij, żeby otworzyć",
            style = TextStyle(color = ColorProvider(TextSecondary), fontSize = 12.sp),
            maxLines = 1,
        )
        val target = data?.proteinTarget
        if (data?.proteinEaten != null) {
            Spacer(GlanceModifier.height(10.dp))
            Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Białko",
                    style = TextStyle(color = ColorProvider(TextSecondary), fontSize = 12.sp),
                    modifier = GlanceModifier.defaultWeight(),
                )
                Text(
                    if (target != null) "${data.proteinEaten} / $target g" else "${data.proteinEaten} g",
                    style = TextStyle(color = ColorProvider(TextPrimary), fontSize = 12.sp, fontWeight = FontWeight.Medium),
                )
            }
            if (target != null) {
                Spacer(GlanceModifier.height(4.dp))
                LinearProgressIndicator(
                    progress = (data.proteinEaten.toFloat() / target).coerceIn(0f, 1f),
                    modifier = GlanceModifier.fillMaxWidth().height(6.dp),
                    color = ColorProvider(Lime),
                    backgroundColor = ColorProvider(Track),
                )
            }
        }
    }
}

class GymWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GymWidget()
}
