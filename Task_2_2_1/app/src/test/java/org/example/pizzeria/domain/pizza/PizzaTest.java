package org.example.pizzeria.domain.pizza;

import org.example.pizzeria.domain.order.Order;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PizzaTest {

    @Test
    @DisplayName("Конструктор должен корректно создавать пиццу из заказа")
    void constructor_ShouldCreatePizzaFromOrder() {
        // Given
        Order order = new Order(123, "Тестовая пицца");

        // When
        Pizza pizza = new Pizza(order);

        // Then
        assertEquals(order.getId(), pizza.getOrderId());
        assertEquals(order.getPizzaDetails(), pizza.getPizzaDetails());
    }

    @Test
    @DisplayName("Конструктор должен выбрасывать исключение, если заказ равен null")
    void constructor_ShouldThrowExceptionForNullOrder() {
        // Then
        assertThrows(IllegalArgumentException.class, () -> {
            // When
            new Pizza(null);
        }, "Должно быть исключение для null order");
    }
}