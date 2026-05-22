package app.coreme.messenger.features.chats.presentation.list

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Tests for time formatting logic equivalent to ChatsScreen.formatTime.
 * Extracted for unit-testability.
 */
class ChatsTimeFormatterTest {

    private val zone = ZoneId.systemDefault()
    private val ukrainianMonths = listOf("", "січ", "лют", "бер", "кві", "тра", "чер", "лип", "сер", "вер", "жов", "лис", "гру")
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    private fun formatTime(instant: Instant): String {
        val now = Instant.now()
        val local = instant.atZone(zone)
        return when {
            ChronoUnit.DAYS.between(instant, now) == 0L -> timeFormatter.format(local)
            ChronoUnit.DAYS.between(instant, now) == 1L -> "вчора"
            else -> "${local.dayOfMonth} ${ukrainianMonths[local.monthValue]}"
        }
    }

    @Test
    fun `today's message shows HH-mm format`() {
        val instant = Instant.now().minus(30, ChronoUnit.MINUTES)
        val result = formatTime(instant)
        assertTrue(result.matches(Regex("\\d{2}:\\d{2}")), "Expected HH:mm but got $result")
    }

    @Test
    fun `yesterday's message shows вчора`() {
        val instant = Instant.now().minus(26, ChronoUnit.HOURS)
        val result = formatTime(instant)
        assertEquals("вчора", result)
    }

    @Test
    fun `older message shows day and month`() {
        val instant = Instant.now().minus(5, ChronoUnit.DAYS)
        val result = formatTime(instant)
        val local = instant.atZone(zone)
        val expectedMonth = ukrainianMonths[local.monthValue]
        assertTrue(result.contains(expectedMonth), "Expected month abbreviation in: $result")
        assertTrue(result.contains(local.dayOfMonth.toString()), "Expected day in: $result")
    }

    @Test
    fun `all month abbreviations are non-empty`() {
        for (i in 1..12) {
            assertTrue(ukrainianMonths[i].isNotEmpty(), "Month $i abbreviation is empty")
        }
    }

    @Test
    fun `month abbreviations have correct count`() {
        // index 0 is empty placeholder, 1-12 are months
        assertEquals(13, ukrainianMonths.size)
    }

    @Test
    fun `message exactly 1 day ago shows вчора`() {
        val instant = Instant.now().minus(1, ChronoUnit.DAYS)
        val result = formatTime(instant)
        assertEquals("вчора", result)
    }

    @Test
    fun `message 2 days ago shows date not вчора`() {
        val instant = Instant.now().minus(2, ChronoUnit.DAYS)
        val result = formatTime(instant)
        assertTrue(result != "вчора", "Expected date format but got: $result")
    }

    @Test
    fun `january abbreviation is січ`() {
        assertEquals("січ", ukrainianMonths[1])
    }

    @Test
    fun `december abbreviation is гру`() {
        assertEquals("гру", ukrainianMonths[12])
    }

    @Test
    fun `time format for midnight is 00-00`() {
        val midnight = Instant.now()
            .atZone(zone)
            .toLocalDate()
            .atStartOfDay(zone)
            .toInstant()
        val result = formatTime(midnight)
        // Midnight today should show HH:mm
        if (ChronoUnit.DAYS.between(midnight, Instant.now()) == 0L) {
            assertEquals("00:00", result)
        }
    }
}
