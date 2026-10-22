package com.ahmedzayed.zasnake.age

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.GregorianCalendar

class AgePolicyTest {
    private fun today(y: Int, m: Int, d: Int): Calendar = GregorianCalendar(y, m - 1, d)

    @Test fun childJustBeforeThirteenthBirthday() {
        val age = AgePolicy.classify(10, 5, 2013, today(2026, 5, 9))!!
        assertEquals(AgeBand.CHILD, age.band)
        assertEquals(AgeBand.TEEN, AgePolicy.refresh(age, today(2026, 5, 10).timeInMillis).band)
    }

    @Test fun teenAtThirteenAndAdultAtEighteen() {
        assertEquals(AgeBand.TEEN, AgePolicy.classify(10, 5, 2013, today(2026, 5, 10))!!.band)
        assertEquals(AgeBand.TEEN, AgePolicy.classify(10, 5, 2008, today(2026, 5, 9))!!.band)
        assertEquals(AgeBand.ADULT, AgePolicy.classify(10, 5, 2008, today(2026, 5, 10))!!.band)
    }

    @Test fun advancesFromChildThroughTeenToAdultWithoutBirthDate() {
        val child = AgePolicy.classify(12, 7, 2015, today(2026, 1, 1))!!
        val teen = AgePolicy.refresh(child, today(2028, 7, 12).timeInMillis)
        assertEquals(AgeBand.TEEN, teen.band)
        assertEquals(AgeBand.ADULT, AgePolicy.refresh(child, today(2033, 7, 12).timeInMillis).band)
        assertEquals(AgeBand.ADULT, AgePolicy.refresh(teen, today(2033, 7, 12).timeInMillis).band)
    }

    @Test fun invalidFutureAndImplausibleDatesRejected() {
        val now = today(2026, 10, 19)
        assertNull(AgePolicy.classify(31, 4, 2015, now))
        assertNull(AgePolicy.classify(29, 2, 2015, now))
        assertNull(AgePolicy.classify(1, 1, 2027, now))
        assertNull(AgePolicy.classify(19, 10, 1880, now))
        assertNull(AgePolicy.classify(0, 1, 2010, now))
        assertNull(AgePolicy.classify(1, 13, 2010, now))
    }

    @Test fun leapDayAcceptedAndTransitionHasFutureBoundary() {
        val age = AgePolicy.classify(29, 2, 2020, today(2026, 10, 19))!!
        assertEquals(AgeBand.CHILD, age.band)
        assertTrue(age.nextTransitionEpochMillis!! > today(2026, 10, 19).timeInMillis)
    }
}
