FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

ENV PIZZERIA_CONFIG_PATH="/app/config/config.json"
ENV PIZZERIA_DATA_DIR="/app/data"
ENV PIZZERIA_LOGS_DIR="/app/logs"
ENV PIZZERIA_PORT="8080"

# Создаем группу и пользователя 'appuser' (UID/GID важны для скрипта)
RUN groupadd -r appgroup --gid 1001 && \
    useradd -r -g appgroup --uid 1001 appuser && \
    mkdir -p /app/config /app/data /app/logs # Создаем директории

# Копируем скрипт entrypoint и делаем исполняемым
COPY entrypoint.sh /usr/local/bin/entrypoint.sh
RUN chmod +x /usr/local/bin/entrypoint.sh

# Копируем JAR
COPY build/libs/app-*-all.jar /app/app.jar

# Устанавливаем владельца ТОЛЬКО для JAR, т.к. скрипт разберется с томами
RUN chown appuser:appgroup /app/app.jar

EXPOSE ${PIZZERIA_PORT}

# Точка входа - наш скрипт
ENTRYPOINT ["entrypoint.sh"]

# CMD можно оставить пустым или передать дефолтные аргументы для java -jar, если нужно
# CMD []