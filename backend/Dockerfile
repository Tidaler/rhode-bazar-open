FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B package -DskipTests

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/target/rhode-bazar-backend.jar app.jar

ENV SPRING_PROFILES_ACTIVE=mysql
EXPOSE 8085

ENTRYPOINT ["java", "-jar", "app.jar"]
