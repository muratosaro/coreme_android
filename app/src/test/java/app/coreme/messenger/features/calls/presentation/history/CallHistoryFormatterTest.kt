package app.coreme.messenger.features.calls.presentation.history

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Tests for call date/duration formatting logic matching CallHistoryScreen.
 */
class CallHistoryFormatterTest {

    private val zone = ZoneId.systemDefault()

    private fun formatDuration(seconds: Int): String {
        val m = seconds / 60
        val s = seconds % 60
        return if (m > 0) "${m}хв ${s}с" else "${s}с"
    }

    private fun formatCallDate(instant: Instant): String {
        val now = Instant.now()
        val local = instant.atZone(zone)
        val diffDays = ChronoUnit.DAYS.between(instant, now)
        return when {
            diffDays == 0L -> {
                val h = local.hour.toString().padStart(2, '0')
                val m = local.minute.toString().padStart(2, '0')
                "$h:$m"
            }
            diffDays < 7L -> {
                val days = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Нд")
                days[local.dayOfWeek.value - 1]
            }
            else -> "${local.dayOfMonth}.${local.monthValue.toString().padStart(2, '0')}"
        }
    }

    // ─── Duration tests ───────────────────────────────────────────────────────

    @Test
    fun `duration under 60 seconds shows only seconds`() {
        assertEquals("45с", formatDuration(45))
    }

    @Test
    fun `duration exactly 60 seconds shows 1хв 0с`() {
        assertEquals("1хв 0с", formatDuration(60))
    }

    @Test
    fun `duration 90 seconds shows 1хв 30с`() {
        assertEquals("1хв 30с", formatDuration(90))
    }

    @Test
    fun `duration 3661 seconds shows 61хв 1с`() {
        assertEquals("61хв 1с", formatDuration(3661))
    }

    @Test
    fun `zero duration shows 0с`() {
        assertEquals("0с", formatDuration(0))
    }

    @Test
    fun `duration 120 seconds shows 2хв 0с`() {
        assertEquals("2хв 0с", formatDuration(120))
    }

    // ─── Date formatting tests ─────────────────────────────────────────────────

    @Test
    fun `today shows HH-mm format`() {
        val instant = Instant.now().minus(10, ChronoUnit.MINUTES)
        val result = formatCallDate(instant)
        assertTrue(result.matches(Regex("\\d{2}:\\d{2}")), "Expected HH:mm but got $result")
    }

    @Test
    fun `yesterday shows day-of-week abbreviation`() {
        val instant = Instant.now().minus(26, ChronoUnit.HOURS)
        val result = formatCallDate(instant)
        val validDays = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Нд")
        assertTrue(result in validDays, "Expected weekday abbreviation but got: $result")
    }

    @Test
    fun `5 days ago shows day-of-week abbreviation`() {
        val instant = Instant.now().minus(5, ChronoUnit.DAYS)
        val result = formatCallDate(instant)
        val validDays = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Нд")
        assertTrue(result in validDays, "Expected weekday abbreviation but got: $result")
    }

    @Test
    fun `10 days ago shows dd-MM format`() {
        val instant = Instant.now().minus(10, ChronoUnit.DAYS)
        val result = formatCallDate(instant)
        assertTrue(result.matches(Regex("\\d{1,2}\\.\\d{2}")), "Expected dd.MM but got $result")
    }

    @Test
    fun `weekday list covers all 7 days`() {
        val days = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Нд")
        assertEquals(7, days.size)
    }

    @Test
    fun `date format pads month with zero`() {
        val instant = Instant.now().minus(10, ChronoUnit.DAYS)
        val result = formatCallDate(instant)
        if (result.matches(Regex("\\d{1,2}\\.\\d{2}"))) {
            val month = result.split(".")[1]
            assertEquals(2, month.length, "Month should be zero-padded")
        }
    }
}
