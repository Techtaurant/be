package com.techtaurant.mainserver.common.base

import com.github.f4b6a3.uuid.UuidCreator
import org.hibernate.annotations.IdGeneratorType
import org.hibernate.engine.spi.SharedSessionContractImplementor
import org.hibernate.generator.BeforeExecutionGenerator
import org.hibernate.generator.EventType
import org.hibernate.generator.EventTypeSets
import java.util.EnumSet
import java.util.UUID

/**
 * UUID V7 생성을 위한 커스텀 어노테이션
 * Entity의 @Id 필드에 사용
 */
@IdGeneratorType(UuidV7Generator::class)
@Target(AnnotationTarget.FIELD, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class UuidV7

/**
 * Hibernate 6.2+ 방식의 UUID V7 생성기
 * 시간 기반 정렬 가능한 UUID 생성
 */
class UuidV7Generator : BeforeExecutionGenerator {
    override fun generate(
        session: SharedSessionContractImplementor,
        owner: Any?,
        currentValue: Any?,
        eventType: EventType
    ): UUID {
        return UuidCreator.getTimeOrderedEpoch()
    }

    override fun getEventTypes(): EnumSet<EventType> {
        return EventTypeSets.INSERT_ONLY
    }
}
