package com.techtaurant.mainserver.common.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

object DateUtils {
    /**
     * UTC 기준 현재 날짜를 반환합니다.
     *
     * @return UTC 기준 현재 날짜
     */
    fun today(): LocalDate = toUtcDate(Instant.now())

    /**
     * UTC 기준으로 Instant를 해당 날짜로 변환합니다.
     *
     * @param instant 변환할 절대 시각
     * @return UTC 기준 날짜
     */
    fun toUtcDate(instant: Instant): LocalDate = instant.atZone(ZoneOffset.UTC).toLocalDate()
}
