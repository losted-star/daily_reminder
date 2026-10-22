package com.dailylist.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class DateUtilsTest {

    @Test
    fun `minutesToTime 边界与常规值`() {
        assertEquals("00:00", DateUtils.minutesToTime(0))
        assertEquals("09:30", DateUtils.minutesToTime(9 * 60 + 30))
        assertEquals("23:59", DateUtils.minutesToTime(23 * 60 + 59))
    }

    @Test
    fun `timeToMinutes 常规与补零输入`() {
        assertEquals(570, DateUtils.timeToMinutes("09:30"))
        assertEquals(570, DateUtils.timeToMinutes("9:30"))
        assertEquals(0, DateUtils.timeToMinutes("00:00"))
    }

    @Test
    fun `timeToMinutes 非法输入返回 null`() {
        assertNull(DateUtils.timeToMinutes("24:00"))
        assertNull(DateUtils.timeToMinutes("09:60"))
        assertNull(DateUtils.timeToMinutes("abc"))
        assertNull(DateUtils.timeToMinutes("9"))
    }

    @Test
    fun `parseDate 与 formatDate 往返一致`() {
        val date = LocalDate.of(2026, 10, 21)
        assertEquals(date, DateUtils.parseDate(DateUtils.formatDate(date)))
        assertEquals("2026-10-21", DateUtils.formatDate(date))
    }
}
