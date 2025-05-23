package org.example.pizzeria.order.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO для запроса на создание нового заказа.
 *
 * @param pizzaDetails Описание пиццы. Должно быть непустым.
 */
public record CreateOrderRequest(
        @NotBlank(message = "Pizza details cannot be blank")
        String pizzaDetails
) {}