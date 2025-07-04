package org.example.pizzeria.processing.warehouse.impl;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.example.pizzeria.config.PizzeriaConfig;
import org.example.pizzeria.domain.order.Order;
import org.example.pizzeria.domain.pizza.Pizza;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConcurrentWarehouseTest {

    private ConcurrentWarehouse warehouse;
    private PizzeriaConfig config;
    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        config = mock(PizzeriaConfig.class);
        when(config.warehouseCapacity()).thenReturn(5); // Небольшая вместимость для теста
        meterRegistry = new SimpleMeterRegistry();
        warehouse = new ConcurrentWarehouse(config, meterRegistry);
    }

    @Test
    @DisplayName("take(maxAmount) должен забирать не более maxAmount пицц")
    void take_shouldRespectMaxAmount() throws InterruptedException {
        // Given
        warehouse.put(new Pizza(new Order(1, "p1")));
        warehouse.put(new Pizza(new Order(2, "p2")));
        warehouse.put(new Pizza(new Order(3, "p3")));

        // When
        List<Pizza> takenPizzas = warehouse.take(2);

        // Then
        assertEquals(2, takenPizzas.size());
        assertEquals(1, warehouse.size());
    }

    @Test
    @DisplayName("Многопоточный тест: один производитель, много потребителей")
    @Timeout(10)
    void multiThread_OneProducerManyConsumers_ShouldProcessAllPizzas() throws InterruptedException {
        // Given
        int consumersCount = 3;
        int totalPizzas = 20;
        ExecutorService executor = Executors.newFixedThreadPool(consumersCount + 1);
        CountDownLatch finishLatch = new CountDownLatch(totalPizzas);
        List<Pizza> consumedPizzas = Collections.synchronizedList(new ArrayList<>());
        AtomicBoolean producerFinished = new AtomicBoolean(false);

        // When
        // Производитель (пекарь)
        executor.submit(() -> {
            try {
                for (int i = 1; i <= totalPizzas; i++) {
                    warehouse.put(new Pizza(new Order(i, "Пицца")));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                producerFinished.set(true); // Сообщаем, что производитель закончил
            }
        });

        // Потребители (курьеры)
        for (int i = 0; i < consumersCount; i++) {
            executor.submit(() -> {
                try {
                    // Потребители работают, пока производитель не закончит И склад не опустеет
                    while (!producerFinished.get() || !warehouse.isEmpty()) {
                        List<Pizza> taken = warehouse.take(2); // Пытаются взять по 2 пиццы
                        if (!taken.isEmpty()) {
                            consumedPizzas.addAll(taken);
                            taken.forEach(p -> finishLatch.countDown());
                        } else if (producerFinished.get() && warehouse.isEmpty()) {
                            // Если производитель закончил и склад пуст, выходим из цикла,
                            // чтобы не ждать вечно в take()
                            break;
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        boolean finishedInTime = finishLatch.await(5, TimeUnit.SECONDS);
        executor.shutdownNow();

        // Then
        assertTrue(finishedInTime, "Тест не завершился за отведенное время. Обработано " + consumedPizzas.size() + " из " + totalPizzas);
        // Проверяем, что все пиццы уникальны и их количество верно
        long distinctPizzas = consumedPizzas.stream().map(Pizza::getOrderId).distinct().count();
        assertEquals(totalPizzas, distinctPizzas, "Количество уникальных пицц не совпадает");
        assertEquals(totalPizzas, consumedPizzas.size(), "Общее количество полученных пицц не совпадает");
        assertTrue(warehouse.isEmpty(), "Склад должен быть пуст после теста");
    }
}