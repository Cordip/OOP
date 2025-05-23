package org.example.pizzeria.order.service.impl;

// Убрали import jakarta.annotation.PostConstruct;
// SLF4j imports (предполагаем переход на SLF4j)
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.example.pizzeria.domain.order.Order;
import org.example.pizzeria.order.service.OrderRepository;
import org.example.pizzeria.order.service.OrderAcceptor;
import org.example.pizzeria.processing.queue.OrderQueue;
import org.springframework.beans.factory.annotation.Autowired;
// Убрали import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

import java.util.Objects;
// Убрали import java.util.concurrent.atomic.AtomicInteger;

/**
 * Реализация OrderAcceptor, которая помещает заказ в очередь.
 * Генерация ID делегирована репозиторию.
 * Является Spring-бином (@Service).
 * Использует SLF4j для логирования.
 */
@Service // Делаем Spring бином
// Убрали @DependsOn
public class PizzeriaOrderAcceptor implements OrderAcceptor {

    // Статический SLF4j логгер
    private static final Logger log = LoggerFactory.getLogger(PizzeriaOrderAcceptor.class);

    private final OrderQueue orderQueue;
    private final OrderRepository repository;
    // Удалили AtomicInteger lastOrderId

    // Флаг для возможности остановки приема новых заказов
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
        // Удалили инициализацию счетчика ID
    }

    // Удалили метод @PostConstruct initializeOrderIdGenerator()

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
            // Используем ID из заказа для лога
            log.warn("Order acceptor is stopped. Rejecting order {}", order.getId());
            throw new IllegalStateException("Order acceptor is not running or shutting down. Cannot accept new orders.");
        }

        try {
            // Помещаем заказ в очередь
            orderQueue.put(order); // Может бросить InterruptedException
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
        int nextId = repository.getNextOrderId(); // Просто вызываем метод репозитория
        log.debug("Retrieved next order ID from repository: {}", nextId);
        return nextId;
    }

    /**
     * Останавливает прием новых заказов.
     */
    public void stopAccepting() {
        // ... (код без изменений) ...
        if (acceptingOrders) {
            this.acceptingOrders = false;
            log.info("Order acceptor stopped accepting new orders.");
        } else {
             log.info("Order acceptor was already stopped.");
        }
    }

    /**
     * Возобновляет прием заказов.
     */
    public void startAccepting() {
         // ... (код без изменений) ...
         if (!acceptingOrders) {
             this.acceptingOrders = true;
             log.info("Order acceptor started accepting new orders.");
         } else {
              log.info("Order acceptor was already running.");
         }
    }

    /**
     * Проверяет, принимает ли сервис заказы в данный момент.
     */
    public boolean isAcceptingOrders() {
        // ... (код без изменений) ...
        return acceptingOrders;
    }
}