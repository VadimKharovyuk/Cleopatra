# Многоэтапная сборка для оптимизации размера образа
FROM maven:3.9.5-openjdk-21-slim AS build

# Установка рабочей директории
WORKDIR /app

# Копирование файлов проекта
COPY pom.xml .
COPY src ./src

# Сборка приложения (пропускаем тесты для ускорения)
RUN mvn clean package -DskipTests

# Финальный этап - создание образа для запуска
FROM openjdk:21-jdk-slim


# Создание пользователя для безопасности
RUN addgroup --system spring && adduser --system spring --ingroup spring
USER spring:spring

# Рабочая директория
WORKDIR /app

# Копирование JAR файла из этапа сборки
COPY --from=build /app/target/cleopatra-0.0.1-SNAPSHOT.jar app.jar

# Настройка JVM для оптимальной работы в контейнере
ENV JAVA_OPTS="-Xmx512m -Xms256m -Djava.security.egd=file:/dev/./urandom"

# Порт, который будет слушать приложение (Render передает через переменную PORT)
EXPOSE 10000

# Команда запуска с активацией продакшен профиля
CMD ["sh", "-c", "java $JAVA_OPTS -Dspring.profiles.active=prod -Dserver.port=${PORT:-10000} -jar app.jar"]