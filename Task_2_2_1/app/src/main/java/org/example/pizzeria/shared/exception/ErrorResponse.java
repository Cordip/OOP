package org.example.pizzeria.shared.exception;

import java.time.Instant;

/**
 * Стандартизированный DTO для возврата ошибок API.
 *
 * @param timestamp Временная метка ошибки.
 * @param status    HTTP статус код.
 * @param error     Общее описание ошибки (из HTTP статуса).
 * @param message   Конкретное сообщение об ошибке.
 * @param path      Путь запроса, на котором произошла ошибка.
 */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path
) {}