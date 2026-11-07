package com.gymtrack.ultra.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.gymtrack.ultra.core.time.TimeProvider
import com.gymtrack.ultra.domain.model.RestTimer
import com.gymtrack.ultra.domain.repository.RestTimerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Przerwa zapisana jako "koniec o godzinie X" — dzięki temu nie resetuje się
 * przy obrocie ekranu, wyjściu do tła ani po zabiciu procesu przez system.
 */
@Singleton
class RestTimerRepositoryImpl @Inject constructor(
    private val store: DataStore<Preferences>,
    private val time: TimeProvider,
) : RestTimerRepository {

    private object Keys {
        val ENDS_AT = longPreferencesKey("rest_ends_at")
        val TOTAL = intPreferencesKey("rest_total_seconds")
        val LABEL = stringPreferencesKey("rest_label")
    }

    override val timer: Flow<RestTimer?> = store.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs ->
            val endsAt = prefs[Keys.ENDS_AT] ?: return@map null
            RestTimer(endsAt, prefs[Keys.TOTAL] ?: 0, prefs[Keys.LABEL].orEmpty())
        }
        .distinctUntilChanged()

    override suspend fun start(seconds: Int, label: String) {
        val total = seconds.coerceIn(5, MAX_SECONDS)
        store.edit {
            it[Keys.ENDS_AT] = time.now().toEpochMilli() + total * 1000L
            it[Keys.TOTAL] = total
            it[Keys.LABEL] = label
        }
    }

    override suspend fun adjust(deltaSeconds: Int) {
        store.edit { prefs ->
            val endsAt = prefs[Keys.ENDS_AT] ?: return@edit
            val now = time.now().toEpochMilli()
            val remaining = ((endsAt - now) / 1000L).toInt()
            val newRemaining = (remaining + deltaSeconds).coerceIn(0, MAX_SECONDS)
            prefs[Keys.ENDS_AT] = now + newRemaining * 1000L
            prefs[Keys.TOTAL] = ((prefs[Keys.TOTAL] ?: 0) + deltaSeconds).coerceIn(newRemaining.coerceAtLeast(1), MAX_SECONDS)
        }
    }

    override suspend fun clear() {
        store.edit {
            it.remove(Keys.ENDS_AT)
            it.remove(Keys.TOTAL)
            it.remove(Keys.LABEL)
        }
    }

    private companion object {
        const val MAX_SECONDS = 15 * 60
    }
}
