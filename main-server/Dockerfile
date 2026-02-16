# Build stage
FROM eclipse-temurin:17-jdk AS builder

WORKDIR /app

# 의존성 캐시 레이어: 빌드 설정 파일만 먼저 복사하여 의존성 다운로드
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .

RUN chmod +x ./gradlew
RUN ./gradlew dependencies --no-daemon

# 소스코드 복사 후 빌드 (의존성 캐시 활용)
COPY src src

RUN ./gradlew bootJar --no-daemon

# Runtime stage
FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
