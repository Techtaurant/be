package com.techtaurant.mainserver.common.util

import java.util.Calendar
import java.util.Date
import java.util.TimeZone

object DateUtils {
    private val utcTimeZone: TimeZone = TimeZone.getTimeZone("UTC")

    /**
     * UTC 기준 현재 날짜를 자정으로 정규화한 Date로 반환합니다.
     *
     * @return UTC 기준 현재 날짜의 00:00:00.000 시각
     */
    fun today(): java.sql.Date = toUtcDate(Date())

    /**
     * UTC 기준으로 Date를 해당 날짜의 자정 시각으로 정규화합니다.
     *
     * @param date 변환할 날짜/시간
     * @return UTC 기준 날짜의 00:00:00.000 시각
     */
    fun toUtcDate(date: Date): java.sql.Date {
        val calendar = newUtcCalendar()
        calendar.time = date
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return java.sql.Date(calendar.timeInMillis)
    }

    private fun newUtcCalendar(): Calendar = Calendar.getInstance(utcTimeZone)
}
