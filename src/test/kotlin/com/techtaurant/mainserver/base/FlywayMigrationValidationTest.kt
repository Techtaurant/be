package com.techtaurant.mainserver.base

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate

@DisplayName("Flyway 마이그레이션 검증 테스트")
class FlywayMigrationValidationTest : IntegrationTest() {
    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Test
    @DisplayName("전체 마이그레이션은 fresh PostgreSQL에 적용되고 링크 통계 마이그레이션 버전이 충돌하지 않는다")
    fun flywayMigrationsApplySuccessfullyWithoutLinkStatsVersionConflicts() {
        val failedMigrationCount =
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = false",
                Long::class.java,
            ) ?: 0L
        val appliedVersions =
            jdbcTemplate.queryForList(
                "SELECT version FROM flyway_schema_history WHERE version IS NOT NULL ORDER BY installed_rank",
                String::class.java,
            )

        assertThat(failedMigrationCount).isZero()
        assertThat(appliedVersions).doesNotHaveDuplicates()
        assertThat(appliedVersions).contains("32", "33")
    }
}
