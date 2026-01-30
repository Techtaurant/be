# Code Conventions & Style Guide

## Language & Comments
- Primary language: Korean
- Comments: Korean language, natural narrative form (no numbered steps)
- Use Kotlin standard conventions (not numbered/step-based)

## Code Structure
- KISS (Keep It Simple, Stupid)
  - Single Responsibility Principle
  - Functions max 50 lines
  - Max 3 levels of nesting
  - Self-explanatory code (clear variable/function names)

- YAGNI (You Aren't Gonna Need It)
  - Implement only what's explicitly required
  - Avoid premature abstraction
  - Rule of Three for generalization

## Database
- Error codes documented in README.md
- DTO classes for request/response
- Custom status codes tracked in README

## Framework: Spring Boot + Kotlin/Java
- Use Spring's dependency injection
- JPA for ORM operations
- Spring Security for authentication
