FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app
COPY build.gradle settings.gradle gradlew ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon

COPY backend/src backend/src
RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/sip-backend-0.1.0.jar app.jar
ENV PORT=10000
EXPOSE 10000
ENTRYPOINT ["sh", "-c", "java -XX:+UseG1GC -Xmx512m -Dserver.port=${PORT} -jar app.jar"]