# One Dockerfile for both apps:  docker build --build-arg MODULE=gateway-app .
#                                docker build --build-arg MODULE=quality-service .
ARG MODULE=gateway-app

FROM maven:3.9-eclipse-temurin-25 AS build
ARG MODULE
WORKDIR /src
COPY pom.xml .
COPY gateway-app/pom.xml gateway-app/
COPY quality-service/pom.xml quality-service/
COPY gateway-app/src gateway-app/src
COPY quality-service/src quality-service/src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q -pl ${MODULE} -am -DskipTests package \
 && cp ${MODULE}/target/${MODULE}-*.jar /src/app.jar

FROM eclipse-temurin:25-jre
RUN groupadd --system app && useradd --system --gid app app
WORKDIR /app
COPY --from=build /src/app.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
