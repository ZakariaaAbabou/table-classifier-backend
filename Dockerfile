# Build Stage
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copy pom.xml and download dependencies (cache layer)
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy source code and build
COPY src ./src
RUN mvn package -DskipTests

# Runtime Stage
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Copy the built JAR from the build stage
COPY --from=build /app/target/table-classifier-0.0.1-SNAPSHOT.jar app.jar

# Expose the port used by Hugging Face Spaces
EXPOSE 7860

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
