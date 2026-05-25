# ---------- build stage ----------
FROM gradle:8.12-jdk17-alpine AS builder
WORKDIR /app

COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle
RUN ./gradlew dependencies --no-daemon -q || true

COPY src ./src
RUN ./gradlew bootJar --no-daemon -x test

# ---------- runtime stage ----------
FROM eclipse-temurin:17-jre-alpine AS runner
WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080
USER appuser
ENTRYPOINT ["java", "-jar", "app.jar"]
