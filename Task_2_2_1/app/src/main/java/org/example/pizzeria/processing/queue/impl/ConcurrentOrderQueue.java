package org.example.pizzeria.processing.queue.impl;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.example.pizzeria.config.PizzeriaConfig;
import org.example.pizzeria.domain.order.Order;
import org.example.pizzeria.processing.queue.OrderQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Потокобезопасная реализация очереди заказов {@link OrderQueue}
 * с ограниченной емкостью.
 * <p>
 * Реализация использует {@link LinkedList} для хранения элементов и
 * встроенный монитор Java (ключевое слово {@code synchronized} и методы
 * {@code wait()}/{@code notifyAll()}) для обеспечения потокобезопасности.
 */
public class ConcurrentOrderQueue implements OrderQueue {

    private static final Logger log = LoggerFactory.getLogger(ConcurrentOrderQueue.class);

    private final LinkedList<Order> queue;
    private final int capacity;
    private final Object lock = new Object(); // Монитор для синхронизации доступа

    private final Counter putsCounter;
    private final Counter takesCounter;
    private final Counter putAttemptsCounter;
    private final Counter takeAttemptsCounter;

    /**
     * Конструктор для создания очереди.
     *
     * @param config        конфигурация пиццерии для определения вместимости.
     * @param meterRegistry реестр для регистрации метрик Micrometer.
     */
    public ConcurrentOrderQueue(PizzeriaConfig config, MeterRegistry meterRegistry) {
        Objects.requireNonNull(config, "PizzeriaConfig cannot be null");
        Objects.requireNonNull(meterRegistry, "MeterRegistry cannot be null");

        this.capacity = config.orderQueueCapacity();
        if (this.capacity <= 0) {
            log.error("Invalid queue capacity configured: {}. Must be positive.", this.capacity);
            throw new IllegalArgumentException("Queue capacity must be positive.");
        }
        this.queue = new LinkedList<>();

        this.putsCounter = meterRegistry.counter("pizzeria.queue.operations.total", "type", "put", "status", "success");
        this.takesCounter = meterRegistry.counter("pizzeria.queue.operations.total", "type", "take", "status", "success");
        this.putAttemptsCounter = meterRegistry.counter("pizzeria.queue.operations.total", "type", "put", "status", "attempt");
        this.takeAttemptsCounter = meterRegistry.counter("pizzeria.queue.operations.total", "type", "take", "status", "attempt");

        log.info("ConcurrentOrderQueue initialized with capacity: {}. Metrics registered.", capacity);
    }

    @Override
    public void put(Order order) throws InterruptedException {
        Objects.requireNonNull(order, "Cannot put a null order into the queue.");
        putAttemptsCounter.increment();

        synchronized (lock) {
            // Используем 'while' для защиты от "spurious wakeups" (ложных пробуждений).
            // Поток должен перепроверить условие после пробуждения.
            while (queue.size() >= capacity) {
                log.debug("Order Queue is full ({}/{}), waiting to put order {}...", queue.size(), capacity, order.getId());
                lock.wait(); // Освобождает монитор и уходит в ожидание.

                // Эта проверка необходима для быстрой реакции на прерывание,
                // если поток был пробужден, но перед этим был прерван.
                if (Thread.currentThread().isInterrupted()) {
                    log.warn("Thread interrupted while waiting to put order {}. Throwing InterruptedException.", order.getId());
                    throw new InterruptedException("Interrupted while waiting to put in queue");
                }
            }

            queue.addLast(order);
            putsCounter.increment();
            log.trace("Order {} added to queue. New size: {}", order.getId(), queue.size());
            lock.notifyAll(); // Пробуждает ВСЕ потоки, ожидающие на этом мониторе (и put, и take).
        }
    }

    @Override
    public Order take() throws InterruptedException {
        takeAttemptsCounter.increment();
        Order order;

        synchronized (lock) {
            // Цикл while для защиты от ложных пробуждений.
            while (queue.isEmpty()) {
                log.debug("Order Queue is empty. Waiting for an order...");
                lock.wait();

                // Эта проверка необходима для быстрой реакции на прерывание.
                if (Thread.currentThread().isInterrupted()) {
                    log.warn("Thread interrupted while waiting to take an order. Throwing InterruptedException.");
                    throw new InterruptedException("Interrupted while waiting to take from queue");
                }
            }
            order = queue.removeFirst();
            takesCounter.increment();
            log.trace("Order {} taken from queue. Remaining size: {}", order.getId(), queue.size());
            lock.notifyAll();
        }
        return order;
    }

    @Override
    public int drainTo(Collection<? super Order> collection) {
        Objects.requireNonNull(collection, "Target collection cannot be null");
        int numberOfElementsDrained;
        synchronized (lock) {
            if (collection == this.queue) {
                throw new IllegalArgumentException("Cannot drain queue to itself");
            }
            numberOfElementsDrained = queue.size();
            if (numberOfElementsDrained > 0) {
                try {
                    log.debug("Draining {} elements to target collection...", numberOfElementsDrained);
                    
                    boolean changed = collection.addAll(queue);
                    if (!changed && !queue.isEmpty()) {
                        log.warn("Target collection reported no change after addAll, but elements were present in the queue.");
                    }
                    
                    queue.clear();
                    log.debug("Drain complete. Queue size is now 0.");
                    lock.notifyAll();
                } catch (UnsupportedOperationException | ClassCastException |
                         NullPointerException | IllegalArgumentException e) {
                    log.error("Error adding elements to target collection during drainTo: {}. Queue might be partially drained or inconsistent.", e.getMessage(), e);
                    numberOfElementsDrained = 0;
                }
            } else {
                 log.trace("drainTo called on empty queue.");
            }
        }
        return numberOfElementsDrained;
    }

    @Deprecated
    @Override
    public List<Order> drainRemaining() {
        List<Order> remaining = new ArrayList<>();
        int count = drainTo(remaining);
        log.warn("drainRemaining() called (deprecated). Drained {} orders.", count);
        return remaining;
    }

    @Override
    public int size() {
        synchronized (lock) {
            return queue.size();
        }
    }

    @Override
    public boolean isEmpty() {
        synchronized (lock) {
            return queue.isEmpty();
        }
    }

    @Override
    public int capacity() {
        return capacity;
    }
}