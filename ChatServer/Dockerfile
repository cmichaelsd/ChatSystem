FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY gradle.properties .
RUN ./gradlew dependencies --no-daemon

COPY src src
RUN ./gradlew installDist --no-daemon

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S chatserver && adduser -S chatserver -G chatserver
WORKDIR /app
COPY --from=build /app/build/install/ChatServer .
RUN chown -R chatserver:chatserver /app
USER chatserver
EXPOSE 8080
ENTRYPOINT ["bin/ChatServer"]
