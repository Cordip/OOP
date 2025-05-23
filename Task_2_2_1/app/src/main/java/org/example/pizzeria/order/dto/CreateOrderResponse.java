package org.example.pizzeria.order.dto;

/**
 * DTO для ответа после успешного создания заказа.
 *
 * @param orderId Уникальный идентификатор созданного заказа.
 */
public record CreateOrderResponse(
        int orderId
) {}