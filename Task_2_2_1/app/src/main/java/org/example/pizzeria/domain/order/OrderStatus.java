package org.example.pizzeria.domain.order;

public enum OrderStatus {
    RECEIVED,
    COOKING,
    COOKED,
    DELIVERING,
    DELIVERED,
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