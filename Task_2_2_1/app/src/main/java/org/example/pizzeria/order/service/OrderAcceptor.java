package org.example.pizzeria.order.service;

import org.example.pizzeria.domain.order.Order;

/**
 * Интерфейс для компонента, отвечающего за прием нового заказа
 * и его передачу в дальнейшую обработку (например, в очередь).
 */
public interface OrderAcceptor {
    /**
     * Принимает новый заказ и инициирует его обработку.
     * Реализация должна быть потокобезопасной и обрабатывать возможные прерывания,
     * если система находится в процессе остановки.
     *
     * @param order Новый заказ для обработки.
     * @throws InterruptedException Если поток был прерван во время ожидания.
     * @throws IllegalStateException Если система не может принять заказ.
     * @throws Exception Если произошла другая ошибка при приеме заказа.
     */
    void acceptOrder(Order order) throws InterruptedException, IllegalStateException, Exception;

    /**
     * Генерирует следующий уникальный ID для заказа.
     * Реализация должна быть потокобезопасной.
     * @return Новый уникальный ID заказа.
     */
    int getNextOrderId();
}