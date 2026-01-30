# Suggested Development Commands

## Build & Run
```bash
./gradlew build              # Build project
./gradlew bootRun           # Run Spring Boot application
./gradlew test              # Run all tests
```

## Code Quality
```bash
./gradlew check             # Run checks (tests, linting)
./gradlew clean             # Clean build artifacts
```

## Database
```bash
# Flyway migrations are automatically applied on boot
# Check PostgreSQL logs for migration status
```

## Development
```bash
git status                   # Check Git status
git log --oneline -10       # View recent commits
```

## Relevant Endpoints
- GET /oauth2/authorization/google - Google OAuth login
- API endpoints under /api/*
