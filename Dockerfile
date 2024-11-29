FROM eclipse-temurin:21-jdk-alpine as builder
LABEL authors="filipuskovic"
WORKDIR /app
COPY . .
RUN ./gradlew clean build -x test

# Stage 2: Create the final image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar

# Health check to ensure the application is running
HEALTHCHECK --interval=30s --timeout=3s \
CMD curl -f http://localhost:8080/actuator/health || exit 1
# Expose the application port
EXPOSE 8080

# Set the entry point to run the application
ENTRYPOINT ["java", "-jar", "app.jar"]