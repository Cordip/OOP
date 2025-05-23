#!/bin/sh
# Устанавливаем жесткую проверку ошибок
set -e

# Получаем UID/GID пользователя appuser ИЗНУТРИ контейнера
# (мы задали их как 1001 в Dockerfile, но можно получить и динамически)
APP_UID=$(id -u appuser)
APP_GID=$(id -g appuser)

# Проверяем, существуют ли директории (могут быть не смонтированы)
# и устанавливаем владельца на UID/GID пользователя appuser
# Используем find, чтобы изменить права только если директория существует
# Это более безопасно, чем просто chown /app/data, если вдруг том не смонтирован
echo "Checking/Setting ownership and permissions for /app/data..."
# Проверяем, существует ли директория
if [ -d "/app/data" ]; then
    # Проверяем текущего владельца. Меняем, если это root.
    if [ "$(stat -c %u /app/data)" = "0" ]; then
        echo "Changing ownership of /app/data to ${APP_UID}:${APP_GID}..."
        chown "${APP_UID}:${APP_GID}" /app/data
    else
        # Если владелец не root, но не наш appuser, тоже стоит поменять
        # Или просто убедиться, что владелец наш (более безопасно?)
        current_uid=$(stat -c %u /app/data)
        if [ "$current_uid" != "$APP_UID" ]; then
             echo "Warning: /app/data is owned by UID $current_uid, not root or $APP_UID. Attempting chown anyway..."
             # Возможно, chown не сработает если у root нет прав на директорию с хоста, но стоит попробовать
             chown "${APP_UID}:${APP_GID}" /app/data || echo "Chown failed, proceeding..."
        else
             echo "Ownership of /app/data is already correct ($APP_UID)."
        fi
    fi
    # --- ВАЖНО: Устанавливаем права на запись для владельца ---
    echo "Ensuring owner has write permissions on /app/data..."
    # u+w добавляет право на запись для владельца, не затрагивая другие права
    chmod u+w /app/data
    echo "Current permissions for /app/data:"
    ls -ld /app/data
else
    echo "/app/data directory not found/mounted. Cannot set ownership/permissions."
    # Решите, критична ли эта ошибка. Если да, можно выйти: exit 1
fi

echo "Checking/Setting ownership and permissions for /app/logs..."
if [ -d "/app/logs" ]; then
    if [ "$(stat -c %u /app/logs)" = "0" ]; then
        echo "Changing ownership of /app/logs to ${APP_UID}:${APP_GID}..."
        chown "${APP_UID}:${APP_GID}" /app/logs
    else
        current_uid=$(stat -c %u /app/logs)
        if [ "$current_uid" != "$APP_UID" ]; then
             echo "Warning: /app/logs is owned by UID $current_uid, not root or $APP_UID. Attempting chown anyway..."
             chown "${APP_UID}:${APP_GID}" /app/logs || echo "Chown failed, proceeding..."
        else
             echo "Ownership of /app/logs is already correct ($APP_UID)."
        fi
    fi
    echo "Ensuring owner has write permissions on /app/logs..."
    chmod u+w /app/logs
    echo "Current permissions for /app/logs:"
    ls -ld /app/logs
else
    echo "/app/logs directory not found/mounted. Cannot set ownership/permissions."
    # exit 1
fi


echo "Setup complete. Executing command as appuser: java -jar /app/app.jar $@"
# Вариант с setpriv (предпочтителен, если доступен)
exec setpriv --reuid=appuser --regid=appgroup --clear-groups java -jar /app/app.jar "$@"
