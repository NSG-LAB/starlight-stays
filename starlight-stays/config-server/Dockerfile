FROM eclipse-temurin:17-jre-alpine
# Install Git inside Alpine so the Config Server can read the local repo
RUN apk add --no-cache git
WORKDIR /app
COPY target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
