package org.example.pizzeria.domain.order;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Модульные тесты для класса {@link Order}.
 * Эти тесты не используют Spring Context и проверяют только логику самого класса.
 */
class OrderTest {

    @Test
    @DisplayName("Создание заказа: начальный статус должен быть RECEIVED")
    void constructor_ShouldSetInitialStatusToReceived() {
        // given
        String pizzaDetails = "Пепперони";
        int orderId = 1;

        // when
        Order order = new Order(orderId, pizzaDetails);

        // then
        assertEquals(orderId, order.getId());
        assertEquals(pizzaDetails, order.getPizzaDetails());
        assertEquals(OrderStatus.RECEIVED, order.getStatus(), "Новый заказ должен иметь статус RECEIVED");
    }

    @Test
    @DisplayName("Создание заказа: проверка валидации (негативный ID)")
    void constructor_ShouldThrowExceptionForNegativeId() {
        // then
        assertThrows(IllegalArgumentException.class, () -> {
            // when
            new Order(-1, "Маргарита");
        }, "Должно быть выброшено исключение для отрицательного ID");
    }

    @Test
    @DisplayName("Создание заказа: проверка валидации (пустые детали)")
    void constructor_ShouldThrowExceptionForBlankDetails() {
        // then
        assertThrows(IllegalArgumentException.class, () -> {
            // when
            new Order(1, "  ");
        }, "Должно быть выброшено исключение для пустых деталей пиццы");
    }

    @Test
    @DisplayName("moveToNextStatus: корректная последовательность статусов")
    void moveToNextStatus_ShouldFollowCorrectLifecycle() {
        // given
        Order order = new Order(1, "Тестовая");
        assertEquals(OrderStatus.RECEIVED, order.getStatus());

        // when & then
        assertTrue(order.moveToNextStatus(), "Переход из RECEIVED должен быть успешным");
        assertEquals(OrderStatus.COOKING, order.getStatus());

        assertTrue(order.moveToNextStatus(), "Переход из COOKING должен быть успешным");
        assertEquals(OrderStatus.COOKED, order.getStatus());

        assertTrue(order.moveToNextStatus(), "Переход из COOKED должен быть успешным");
        assertEquals(OrderStatus.DELIVERING, order.getStatus());

        assertTrue(order.moveToNextStatus(), "Переход из DELIVERING должен быть успешным");
        assertEquals(OrderStatus.DELIVERED, order.getStatus());
    }

    @Test
    @DisplayName("moveToNextStatus: невозможность перехода из финального статуса DELIVERED")
    void moveToNextStatus_ShouldFailFromDeliveredStatus() {
        // given
        Order order = new Order(1, "Тестовая");
        // Переводим заказ в финальный статус
        order.moveToNextStatus(); // COOKING
        order.moveToNextStatus(); // COOKED
        order.moveToNextStatus(); // DELIVERING
        order.moveToNextStatus(); // DELIVERED

        // when
        boolean result = order.moveToNextStatus();

        // then
        assertFalse(result, "Переход из DELIVERED не должен быть возможен");
        assertEquals(OrderStatus.DELIVERED, order.getStatus(), "Статус не должен был измениться");
    }

    @Test
    @DisplayName("discard: отмена заказа из активного статуса")
    void discard_ShouldChangeStatusToDiscarded() {
        // given
        Order order = new Order(1, "Тестовая");
        order.moveToNextStatus(); // Статус COOKING
        
        // when
        boolean result = order.discard();

        // then
        assertTrue(result, "Отмена должна быть успешной");
        assertEquals(OrderStatus.DISCARDED, order.getStatus(), "Статус должен измениться на DISCARDED");
    }

    @Test
    @DisplayName("discard: невозможность повторной отмены")
    void discard_ShouldFailWhenAlreadyDiscarded() {
        // given
        Order order = new Order(1, "Тестовая");
        order.discard(); // Отменяем первый раз

        // when
        boolean result = order.discard();

        // then
        assertFalse(result, "Повторная отмена не должна быть возможной");
        assertEquals(OrderStatus.DISCARDED, order.getStatus(), "Статус не должен был измениться");
    }

    @Test
    @DisplayName("moveToNextStatus: невозможность перехода из статуса DISCARDED")
    void moveToNextStatus_ShouldFailFromDiscardedStatus() {
        // given
        Order order = new Order(1, "Тестовая");
        order.discard();

        // when
        boolean result = order.moveToNextStatus();

        // then
        assertFalse(result, "Переход из DISCARDED не должен быть возможен");
        assertEquals(OrderStatus.DISCARDED, order.getStatus(), "Статус не должен был измениться");
    }
}