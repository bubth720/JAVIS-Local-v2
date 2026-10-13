package com.stark.jarvislocal

import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
import java.util.Locale
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Test

class LocalTimeReplyTest {
    private val utc = TimeZone.getTimeZone("UTC")

    @Test
    fun midnightUsesZeroHours() {
        assertEquals("Il est 00:00.", localTimeReply(instant(0, 0), utc))
    }

    @Test
    fun singleDigitHoursAndMinutesArePaddedAndSecondsAreOmitted() {
        assertEquals("Il est 09:05.", localTimeReply(instant(9, 5, 42), utc))
    }

    @Test
    fun eveningUsesTwentyFourHourFormat() {
        assertEquals("Il est 23:59.", localTimeReply(instant(23, 59), utc))
    }

    @Test
    fun localTimeIncludesFractionalHourOffset() {
        assertEquals(
            "Il est 05:30.",
            localTimeReply(instant(0, 0), TimeZone.getTimeZone("GMT+05:30"))
        )
    }

    @Test
    fun daylightSavingTransitionUsesTheOffsetAtTheRequestedInstant() {
        val paris = TimeZone.getTimeZone("Europe/Paris")
        assertEquals("Il est 01:59.", localTimeReply(instant(0, 59), paris))
        assertEquals("Il est 03:00.", localTimeReply(instant(1, 0), paris))
    }

    @Test
    fun defaultTimeZoneIsReadOnEachCall() {
        val original = TimeZone.getDefault()
        try {
            TimeZone.setDefault(utc)
            assertEquals("Il est 09:05.", localTimeReply(instant(9, 5)))
            TimeZone.setDefault(TimeZone.getTimeZone("GMT-04:00"))
            assertEquals("Il est 05:05.", localTimeReply(instant(9, 5)))
        } finally {
            TimeZone.setDefault(original)
        }
    }

    @Test
    fun nonLatinDefaultLocalePreservesAsciiTimeFormat() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("ar-EG"))
            assertEquals("Il est 09:05.", localTimeReply(instant(9, 5), utc))
        } finally {
            Locale.setDefault(original)
        }
    }

    private fun instant(hour: Int, minute: Int, second: Int = 0): Date =
        GregorianCalendar(utc).apply {
            clear()
            set(2024, Calendar.MARCH, 31, hour, minute, second)
        }.time
}
