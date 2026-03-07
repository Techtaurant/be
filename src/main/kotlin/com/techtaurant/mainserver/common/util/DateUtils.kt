package com.techtaurant.mainserver.common.util

import java.time.LocalDate
import java.time.ZoneOffset

object DateUtils {
    /**
     * UTC 기준 현재 날짜를 년-월-일만 포함하는 LocalDate로 반환합니다.
     * 시간 정보는 포함되지 않으며, UTC 시간대 기준입니다.
     *
     * @return UTC 기준 현재 날짜 (년-월-일)
     */
    fun today(): LocalDate = LocalDate.now(ZoneOffset.UTC)
}
