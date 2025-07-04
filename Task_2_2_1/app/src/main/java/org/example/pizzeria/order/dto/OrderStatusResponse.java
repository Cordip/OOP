package org.example.pizzeria.order.dto;

/**
 * DTO для ответа на запрос статуса заказа.
 *
 * @param orderId Идентификатор заказа.
 * @param status  Текущий статус заказа в виде строки.
 */
public record OrderStatusResponse(
        int orderId,
        String status
) {}