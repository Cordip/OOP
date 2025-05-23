package org.example.pizzeria.domain.worker;

/**
 * Общий интерфейс для работников (Пекари, Курьеры).
 * Конкретные работники также будут реализовывать Runnable.
 */
public interface Worker {
    /**
     * @return Уникальный идентификатор работника.
     */
    int getId();
}