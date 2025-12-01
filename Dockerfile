# ---------- BUILDER (compila el proyecto) ----------
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /app

# Copiar solo los archivos de configuración primero (mejora cache)
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn

RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline

# Ahora copiar el código
COPY src ./src

# Compilar (esto generará el jar en target/)
RUN ./mvnw package -DskipTests

# ---------- RUNNER (corre la aplicación) ----------
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copiar el jar compilado desde la fase anterior
COPY --from=build /app/target/*.jar app.jar

# Exponer tu puerto
EXPOSE 8080

# Comando final para correr tu app
ENTRYPOINT ["java", "-jar", "app.jar"]
