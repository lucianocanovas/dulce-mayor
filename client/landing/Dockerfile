# ==============================================================================
# Stage 1: Build Spring Boot + Thymeleaf Landing Storefront (Java 21)
# ==============================================================================
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

COPY src/ src/
RUN ./mvnw clean package -DskipTests -B

# ==============================================================================
# Stage 2: Lightweight Production Runtime (Java 21 JRE)
# ==============================================================================
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring

COPY --from=build /app/target/*.jar app.jar
RUN chown spring:spring /app/app.jar

ENV SERVER_PORT=8081
ENV BACKEND_API_URL=http://server:8080/api

USER spring:spring
EXPOSE 8081

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]

