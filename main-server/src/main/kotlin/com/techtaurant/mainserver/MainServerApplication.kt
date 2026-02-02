package com.techtaurant.mainserver

import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "10m")
@EnableJpaAuditing
@ConfigurationPropertiesScan
@SpringBootApplication
class MainServerApplication

fun main(args: Array<String>) {
    runApplication<MainServerApplication>(*args)
}
