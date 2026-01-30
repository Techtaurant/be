# Techtaurant Main Server Project Overview

## Purpose
Spring Boot 기반 Restaurant Management Backend API Server

## Tech Stack
- Language: Kotlin, Java 17
- Framework: Spring Boot 3.5.7
- Database: PostgreSQL + Redis
- ORM: JPA
- Security: OAuth2 (Google), JWT, Spring Security
- Build: Gradle (Kotlin DSL)
- Monitoring: p6spy (SQL logging)
- Testing: JUnit 5, MockK
- API Documentation: Swagger/OpenAPI

## Architecture
- src/ - Source code directory
- build/ - Gradle build output
- gradle/ - Gradle wrapper
- Dockerfile - Container configuration
- Flyway - Database migration management

## Database
- PostgreSQL with Flyway migrations
- Redis integration
- p6spy for SQL query logging/monitoring

## Current Branch
- perf/user-nickname - Performance optimization for user nickname search
- Recent focus: User search API with nickname optimization
