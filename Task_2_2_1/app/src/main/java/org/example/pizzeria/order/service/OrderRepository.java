package org.example.pizzeria.order.service;

import java.util.Collection;
import org.example.pizzeria.domain.order.Order;
import org.example.pizzeria.domain.order.OrderStatus;
import java.util.Optional;

/**
 * Интерфейс для потокобезопасного репозитория заказов.
 * Хранит все заказы системы для отслеживания их статуса и генерации ID.
 */
public interface OrderRepository {

    /**
     * Добавляет новый заказ в репозиторий.
     * Реализация должна обеспечить потокобезопасность и запись в лог (событие CREATE).
     * Статус заказа при добавлении должен быть RECEIVED.
     * @param order Новый заказ.
     * @throws IllegalArgumentException если заказ с таким ID уже существует или order is null.
     * @throws RuntimeException при ошибках записи в лог или хранилище.
     */
    void addOrder(Order order);

    /**
     * Находит и возвращает заказ по его ID из внутреннего кэша/хранилища.
     * Реализация должна быть потокобезопасной.
     * @param orderId ID искомого заказа.
     * @return Объект Order или null, если заказ с таким ID не найден (включая финальные статусы).
     */
    Order getOrderById(int orderId);

    /**
     * Возвращает коллекцию всех *активных* заказов, хранящихся в репозитории.
     * Используется, например, при завершении работы для обработки оставшихся заказов.
     * Реализация должна быть потокобезопасной.
     * @return Коллекция активных заказов.
     */
     Collection<Order> getAllOrders();

     /**
      * Возвращает общее количество заказов (активных + финальных) в репозитории.
      * Реализация должна быть потокобезопасной.
      * @return общее количество заказов.
      */
     int count();

     /**
      * Возвращает следующий уникальный ID для нового заказа.
      * Реализация должна быть потокобезопасной и гарантировать уникальность ID.
      * @return Следующий уникальный ID заказа.
      */
     int getNextOrderId();

    /**
     * Записывает событие изменения статуса заказа в лог персистентности.
     * Вызывается *после* фактического изменения статуса в объекте Order.
     * Реализация должна обеспечить потокобезопасность и запись в лог (событие STATUS_UPDATE).
     *
     * @param orderId ID заказа, чей статус изменился.
     * @param newStatus Новый статус, который был установлен в объекте Order.
     * @throws RuntimeException при ошибках записи в лог.
     */
    void logStatusUpdate(int orderId, OrderStatus newStatus);

    /**
     * Находит и возвращает статус заказа по его ID.
     * Может вернуть статус и для заказов в финальном состоянии.
     * @param orderId ID искомого заказа.
     * @return Optional, содержащий OrderStatus, если заказ найден,
     *         или Optional.empty(), если заказ не найден.
     */
    Optional<OrderStatus> findOrderStatusById(int orderId);

}