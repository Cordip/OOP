package org.example.pizzeria.processing.warehouse.impl;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

import org.example.pizzeria.config.PizzeriaConfig;
import org.example.pizzeria.domain.pizza.Pizza;
import org.example.pizzeria.processing.warehouse.Warehouse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Потокобезопасная реализация склада пицц {@link Warehouse}
 * с ограниченной емкостью на основе {@link LinkedList} и встроенных
 * мониторов Java (synchronized, wait, notifyAll).
 * Использует SLF4j для логирования и Micrometer для метрик.
 */
public class ConcurrentWarehouse implements Warehouse {

    private static final Logger log = LoggerFactory.getLogger(ConcurrentWarehouse.class);

    private final LinkedList<Pizza> storage;
    private final int capacity;
    private final Object lock = new Object(); // Монитор для синхронизации

    private final Counter putsCounter;
    private final Counter takesCounter; // Общий счетчик успешно взятых пицц
    private final Counter putAttemptsCounter;
    private final Counter takeAttemptsCounter; // Общий счетчик попыток взятия

    /**
     * Конструктор.
     * @param config Конфигурация пиццерии.
     * @param meterRegistry Реестр метрик Micrometer.
     */
    public ConcurrentWarehouse(PizzeriaConfig config, MeterRegistry meterRegistry) {
        Objects.requireNonNull(config, "PizzeriaConfig cannot be null");
        Objects.requireNonNull(meterRegistry, "MeterRegistry cannot be null");

        this.capacity = config.warehouseCapacity();
        if (this.capacity <= 0) {
            log.error("Invalid warehouse capacity configured: {}. Must be positive.", this.capacity);
            throw new IllegalArgumentException("Warehouse capacity must be positive.");
        }
        this.storage = new LinkedList<>();

        this.putsCounter = meterRegistry.counter("pizzeria.warehouse.operations.total", "type", "put", "status", "success");
        this.takesCounter = meterRegistry.counter("pizzeria.warehouse.operations.total", "type", "take", "status", "success");
        this.putAttemptsCounter = meterRegistry.counter("pizzeria.warehouse.operations.total", "type", "put", "status", "attempt");
        this.takeAttemptsCounter = meterRegistry.counter("pizzeria.warehouse.operations.total", "type", "take", "status", "attempt");

        log.info("ConcurrentWarehouse initialized with capacity: {}. Metrics registered.", capacity);
    }

    @Override
    public void put(Pizza pizza) throws InterruptedException {
        if (pizza == null) {
            log.warn("Attempted to put a null pizza into the warehouse. Ignoring.");
            return;
        }
        putAttemptsCounter.increment();

        synchronized (lock) {
            while (storage.size() >= capacity) {
                log.debug("Warehouse is full ({}/{}), waiting to put pizza for order {}...", storage.size(), capacity, pizza.getOrderId());
                lock.wait();

                if (Thread.currentThread().isInterrupted()) {
                    log.warn("Thread interrupted while waiting to put pizza for order {}. Throwing InterruptedException.", pizza.getOrderId());
                    throw new InterruptedException("Interrupted while waiting to put in warehouse");
                }
            }
            storage.addLast(pizza);
            putsCounter.increment();
            log.trace("Pizza for order {} added to warehouse. Storage size: {}", pizza.getOrderId(), storage.size());
            lock.notifyAll();
        }
    }

    @Override
    public List<Pizza> take(int maxAmount) throws InterruptedException {
        if (maxAmount <= 0) {
            log.warn("Attempted to take non-positive amount of pizza: {}. Returning empty list.", maxAmount);
            return new ArrayList<>();
        }
        takeAttemptsCounter.increment();
        List<Pizza> takenPizzas;

        synchronized (lock) {
            while (storage.isEmpty()) {
                log.debug("Warehouse is empty. Waiting for pizza (Courier wants up to {})...", maxAmount);
                lock.wait();

                if (Thread.currentThread().isInterrupted()) {
                    log.warn("Thread interrupted while waiting to take pizzas. Throwing InterruptedException.");
                    throw new InterruptedException("Interrupted while waiting to take from warehouse");
                }
            }
            int takeCount = Math.min(maxAmount, storage.size());
            takenPizzas = new ArrayList<>(takeCount);

            for (int i = 0; i < takeCount; i++) {
                takenPizzas.add(storage.removeFirst());
            }

            if (takeCount > 0) {
                takesCounter.increment(takeCount); // Считаем количество успешно взятых ПИЦЦ
                log.trace("Courier took {} pizzas. Storage size: {}", takeCount, storage.size());
                lock.notifyAll(); // Оповещаем ожидающие потоки
            } else {
                 log.warn("Warehouse was not empty, but took 0 pizzas (maxAmount: {})", maxAmount);
            }
        }
        return takenPizzas;
    }

    @Override
    public int drainTo(Collection<? super Pizza> collection) {
        Objects.requireNonNull(collection, "Target collection cannot be null");
        takeAttemptsCounter.increment();
        int numberOfElementsDrained = 0;

        synchronized (lock) {
            if (collection == this.storage) {
                throw new IllegalArgumentException("Cannot drain warehouse to itself");
            }
            numberOfElementsDrained = storage.size();
            if (numberOfElementsDrained > 0) {
                try {
                    log.debug("Draining {} pizzas to target collection...", numberOfElementsDrained);
                    boolean changed = collection.addAll(storage);
                     if (!changed && !storage.isEmpty()) {
                        log.warn("Target collection reported no change after addAll, but pizzas were present in the warehouse.");
                     }
                    storage.clear();
                    takesCounter.increment(numberOfElementsDrained); // Считаем успешно взятые ПИЦЦЫ
                    log.debug("Drain complete. Warehouse size is now 0.");
                    lock.notifyAll(); // Уведомляем потоки, ждущие места
                } catch (UnsupportedOperationException | ClassCastException |
                         NullPointerException | IllegalArgumentException e) {
                    log.error("Error adding elements to target collection during drainTo: {}. Warehouse might be partially drained or inconsistent.", e.getMessage(), e);
                    numberOfElementsDrained = 0; // Операция не удалась
                }
            } else {
                log.trace("drainTo called on empty warehouse.");
            }
        }
        return numberOfElementsDrained;
    }

    @Deprecated
    @Override
    public List<Pizza> drainRemaining() {
        List<Pizza> remaining = new ArrayList<>();
        int count = drainTo(remaining);
        log.warn("drainRemaining() called (deprecated). Drained {} pizzas.", count);
        return remaining;
    }

    @Override
    public int size() {
        synchronized (lock) {
            return storage.size();
        }
    }

    @Override
    public boolean isEmpty() {
        synchronized (lock) {
            return storage.isEmpty();
        }
    }

    @Override
    public int capacity() {
        return capacity;
    }
}