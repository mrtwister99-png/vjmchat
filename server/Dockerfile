FROM gradle:8.8-jdk21 AS build
WORKDIR /app
COPY . .
RUN chmod +x gradlew && ./gradlew buildFatJar -x test -Pproduction --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/libs/server.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]