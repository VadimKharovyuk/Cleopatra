FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache tzdata

RUN addgroup --system spring && adduser --system spring --ingroup spring
USER spring:spring
WORKDIR /app
COPY --from=build /app/target/cleopatra-0.0.1-SNAPSHOT.jar app.jar

# ✅ ИСПРАВЛЕННЫЕ JVM настройки для Render Free (512MB лимит)
ENV JAVA_OPTS="-Xmx280m -Xms128m -XX:+UseG1GC -XX:+UseStringDeduplication -XX:MaxGCPauseMillis=100 -Djava.security.egd=file:/dev/./urandom"

EXPOSE 10000

CMD ["sh", "-c", "java $JAVA_OPTS -Dspring.profiles.active=prod -jar app.jar"]