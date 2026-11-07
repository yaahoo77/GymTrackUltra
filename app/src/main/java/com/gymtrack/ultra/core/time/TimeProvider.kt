package com.gymtrack.ultra.core.time

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/** Źródło czasu wstrzykiwane wszędzie, gdzie liczy się "dziś" — dzięki temu logika jest testowalna. */
interface TimeProvider {
    fun now(): Instant
    fun zone(): ZoneId
    fun today(): LocalDate = now().atZone(zone()).toLocalDate()
}

class SystemTimeProvider @Inject constructor() : TimeProvider {
    override fun now(): Instant = Instant.now()
    override fun zone(): ZoneId = ZoneId.systemDefault()
}
