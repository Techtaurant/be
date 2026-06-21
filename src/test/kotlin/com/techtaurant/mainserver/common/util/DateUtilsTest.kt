package com.techtaurant.mainserver.common.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate

@DisplayName("DateUtils UTC 날짜 테스트")
class DateUtilsTest {
    @Test
    @DisplayName("Instant를 서버 로컬 시간이 아니라 UTC 날짜로 변환한다")
    fun toUtcDate_usesUtcDateBoundary() {
        val instantNearUtcEndOfDay = Instant.parse("2026-03-01T23:30:00Z")

        val result = DateUtils.toUtcDate(instantNearUtcEndOfDay)

        assertThat(result).isEqualTo(LocalDate.parse("2026-03-01"))
    }

    @Test
    @DisplayName("UTC 자정 이후 Instant는 다음 UTC 날짜로 변환한다")
    fun toUtcDate_afterUtcMidnight_returnsNextUtcDate() {
        val instantAtUtcMidnight = Instant.parse("2026-03-02T00:00:00Z")

        val result = DateUtils.toUtcDate(instantAtUtcMidnight)

        assertThat(result).isEqualTo(LocalDate.parse("2026-03-02"))
    }
}
