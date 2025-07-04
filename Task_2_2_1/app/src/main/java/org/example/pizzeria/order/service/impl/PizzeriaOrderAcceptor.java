package org.example.pizzeria.order.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.example.pizzeria.domain.order.Order;
import org.example.pizzeria.order.service.OrderRepository;
import org.example.pizzeria.order.service.OrderAcceptor;
import org.example.pizzeria.processing.queue.OrderQueue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * Реализация OrderAcceptor, которая помещает заказ в очередь.
 * Генерация ID делегирована репозиторию.
 * Является Spring-бином (@Service).
 * Использует SLF4j для логирования.
 */
@Service
public class PizzeriaOrderAcceptor implements OrderAcceptor {

    private static final Logger log = LoggerFactory.getLogger(PizzeriaOrderAcceptor.class);

    private final OrderQueue orderQueue;
    private final OrderRepository repository;

    private volatile boolean acceptingOrders = true;

    /**
     * Конструктор для внедрения зависимостей Spring.
     * @param orderQueue Бин очереди заказов.
     * @param repository Бин репозитория заказов.
     */
    @Autowired
    public PizzeriaOrderAcceptor(OrderQueue orderQueue, OrderRepository repository) {
        this.orderQueue = Objects.requireNonNull(orderQueue, "Order queue cannot be null");
        this.repository = Objects.requireNonNull(repository, "Order repository cannot be null");
        log.info("PizzeriaOrderAcceptorService bean created.");
    }

    /**
     * Принимает новый заказ и помещает в очередь.
     * ID заказа должен быть сгенерирован ДО вызова этого метода (через getNextOrderId)
     * и установлен в объекте Order.
     *
     * @param order Новый заказ для обработки (с уже установленным ID).
     * @throws InterruptedException Если поток был прерван во время ожидания места в очереди.
     * @throws IllegalStateException Если система не принимает заказы (acceptingOrders = false).
     * @throws IllegalArgumentException Если order равен null.
     * @throws RuntimeException Если возникла ошибка при добавлении в очередь.
     */
    @Override
    public void acceptOrder(Order order) throws InterruptedException, IllegalStateException {
        Objects.requireNonNull(order, "Cannot accept a null order");

        if (!acceptingOrders) {
            log.warn("Order acceptor is stopped. Rejecting order {}", order.getId());
            throw new IllegalStateException("Order acceptor is not running or shutting down. Cannot accept new orders.");
        }

        try {
            orderQueue.put(order);
            log.info("Order {} accepted and placed in queue. Current queue size: {}", order.getId(), orderQueue.size());

        } catch (InterruptedException ie) {
            log.warn("Thread interrupted while putting order {} into the queue.", order.getId());
            Thread.currentThread().interrupt();
            throw ie;
        } catch (Exception e) {
            log.error("Error accepting order {}: {}", order.getId(), e.getMessage(), e);
            if (!(e instanceof IllegalStateException)) {
                 throw new RuntimeException("Failed to accept order " + order.getId(), e);
            } else {
                 throw e;
            }
        }
    }

    /**
     * Делегирует получение следующего уникального ID репозиторию.
     *
     * @return Новый уникальный ID заказа.
     */
    @Override
    public int getNextOrderId() {
        int nextId = repository.getNextOrderId();
        log.debug("Retrieved next order ID from repository: {}", nextId);
        return nextId;
    }

    public void stopAccepting() {
        if (acceptingOrders) {
            this.acceptingOrders = false;
            log.info("Order acceptor stopped accepting new orders.");
        } else {
             log.info("Order acceptor was already stopped.");
        }
    }

    public void startAccepting() {
         if (!acceptingOrders) {
             this.acceptingOrders = true;
             log.info("Order acceptor started accepting new orders.");
         } else {
              log.info("Order acceptor was already running.");
         }
    }

    public boolean isAcceptingOrders() {
        return acceptingOrders;
    }
}