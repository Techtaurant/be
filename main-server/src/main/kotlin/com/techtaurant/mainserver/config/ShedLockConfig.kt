package com.techtaurant.mainserver.config

import net.javacrumbs.shedlock.core.LockProvider
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.JdbcTemplate
import javax.sql.DataSource

/**
 * ShedLock 설정 클래스입니다.
 * 분산 환경에서 스케줄러가 중복 실행되지 않도록 데이터베이스 기반 락을 제공합니다.
 */
@Configuration
class ShedLockConfig {

    /**
     * JDBC 기반 Lock Provider를 생성합니다.
     * shedlock 테이블을 사용하여 분산 락을 관리합니다.
     *
     * @param dataSource PostgreSQL 데이터소스
     * @return JDBC 기반 LockProvider
     */
    @Bean
    fun lockProvider(dataSource: DataSource): LockProvider {
        return JdbcTemplateLockProvider(
            JdbcTemplateLockProvider.Configuration.builder()
                .withJdbcTemplate(JdbcTemplate(dataSource))
                .usingDbTime() // 데이터베이스 시간을 기준으로 락 시간 계산
                .build()
        )
    }
}
