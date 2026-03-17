FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY . .
RUN ./gradlew distZip --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/build/distributions/splitwise-kotlin-app.zip .
RUN unzip splitwise-kotlin-app.zip && rm splitwise-kotlin-app.zip
EXPOSE 8080
CMD ["splitwise-kotlin-app/bin/splitwise-kotlin-app"]
