# Стадия сборки - используем более легкий образ
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Копируем только pom.xml сначала для кэширования зависимостей
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Копируем исходный код и собираем
COPY src ./src
RUN mvn clean package -DskipTests -Dmaven.javadoc.skip=true -Dmaven.test.skip=true

# Продакшн стадия - минимальный образ
FROM eclipse-temurin:21-jre-alpine

# Устанавливаем только необходимые пакеты (добавлен curl для healthcheck)
RUN apk add --no-cache --update tzdata curl && \
    apk cache clean && \
    rm -rf /var/cache/apk/*

# Создаем пользователя для безопасности
RUN addgroup --system --gid 1001 spring && \
    adduser --system --uid 1001 --ingroup spring spring

# Создаем директории с правильными правами
RUN mkdir -p /app /tmp && \
    chown -R spring:spring /app /tmp

USER spring:spring
WORKDIR /app

# Копируем JAR файл из стадии сборки
COPY --from=build --chown=spring:spring /app/target/cleopatra-0.0.1-SNAPSHOT.jar app.jar

# КРИТИЧЕСКИ ВАЖНЫЕ JVM настройки для Render Free (512MB лимит)
ENV JAVA_OPTS="-server \
    -Xmx280m \
    -Xms64m \
    -XX:+UseSerialGC \
    -XX:MaxMetaspaceSize=80m \
    -XX:CompressedClassSpaceSize=16m \
    -XX:ReservedCodeCacheSize=16m \
    -XX:+UseCompressedOops \
    -XX:+UseCompressedClassPointers \
    -XX:+TieredCompilation \
    -XX:TieredStopAtLevel=1 \
    -XX:+UseContainerSupport \
    -XX:InitialRAMPercentage=12.0 \
    -XX:MaxRAMPercentage=55.0 \
    -Djava.awt.headless=true \
    -Djava.security.egd=file:/dev/./urandom \
    -Dfile.encoding=UTF-8 \
    -Duser.timezone=UTC \
    -Djava.net.preferIPv4Stack=true \
    -XX:+ExitOnOutOfMemoryError"

# Переменные среды для Spring Boot
ENV SPRING_PROFILES_ACTIVE=prod
ENV SERVER_PORT=10000

EXPOSE 10000

# Healthcheck для контейнера (исправлен на curl)
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:10000/actuator/health || exit 1

# Запуск приложения с оптимизированными настройками
CMD ["sh", "-c", "exec java $JAVA_OPTS -Dspring.profiles.active=$SPRING_PROFILES_ACTIVE -Dserver.port=$SERVER_PORT -jar app.jar"]