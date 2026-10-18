# syntax=docker/dockerfile:1

# --- build: собираем jar и раскладываем его на слои ---
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
# зависимости отдельным слоем: пересобираются только при изменении pom.xml
RUN --mount=type=cache,target=/root/.m2 ./mvnw -q -B dependency:go-offline
COPY src/ src/
RUN --mount=type=cache,target=/root/.m2 ./mvnw -q -B package -DskipTests \
 && java -Djarmode=tools -jar target/booking-service-0.0.1-SNAPSHOT.jar extract --layers --launcher --destination /extracted

# --- runtime: только JRE и слои приложения ---
FROM eclipse-temurin:21-jre
RUN useradd --system --uid 1001 app
WORKDIR /app
COPY --from=build /extracted/dependencies/ ./
COPY --from=build /extracted/spring-boot-loader/ ./
COPY --from=build /extracted/snapshot-dependencies/ ./
COPY --from=build /extracted/application/ ./
USER app
EXPOSE 8080
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
