package org.example.pizzeria.domain.order;

/**
 * Перечисление, представляющее все возможные статусы заказа в системе.
 */
public enum OrderStatus {
    /** Заказ принят, но еще не в работе. */
    RECEIVED,
    /** Пекарь начал готовить заказ. */
    COOKING,
    /** Заказ приготовлен и находится на складе. */
    COOKED,
    /** Курьер забрал заказ и доставляет его. */
    DELIVERING,
    /** Заказ успешно доставлен клиенту. Финальный статус. */
    DELIVERED,
    /** Заказ отменен по какой-либо причине. Финальный статус. */
    DISCARDED;

    /**
     * Проверяет, является ли данный статус финальным (доставлен или отменен).
     *
     * @return {@code true} если статус DELIVERED или DISCARDED, иначе {@code false}.
     */
    public boolean isFinal() {
        return this == DELIVERED || this == DISCARDED;
    }
}