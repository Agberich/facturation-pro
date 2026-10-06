# Etape 1 : compilation
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

# Etape 2 : exécution
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

# Utilisateur sans privilèges : en cas de faille, l'attaquant n'est pas root dans le conteneur
RUN addgroup -S app && adduser -S app -G app
USER app

ENTRYPOINT ["java", "-jar", "app.jar"]