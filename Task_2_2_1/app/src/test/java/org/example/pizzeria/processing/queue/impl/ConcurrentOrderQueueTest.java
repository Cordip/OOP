package org.example.pizzeria.processing.queue.impl;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.example.pizzeria.config.PizzeriaConfig;
import org.example.pizzeria.domain.order.Order;
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
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConcurrentOrderQueueTest {

    private ConcurrentOrderQueue orderQueue;
    private PizzeriaConfig config;
    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        config = mock(PizzeriaConfig.class);
        when(config.orderQueueCapacity()).thenReturn(10); // Устанавливаем вместимость 10 для тестов
        meterRegistry = new SimpleMeterRegistry();
        orderQueue = new ConcurrentOrderQueue(config, meterRegistry);
    }

    @Test
    @DisplayName("Простая операция put и take")
    void putAndTake_SingleThread_ShouldWork() throws InterruptedException {
        // Given
        Order order = new Order(1, "Тестовый");

        // When
        orderQueue.put(order);
        Order takenOrder = orderQueue.take();

        // Then
        assertEquals(order, takenOrder);
        assertTrue(orderQueue.isEmpty());
    }

    @Test
    @DisplayName("Многопоточный тест: много производителей и один потребитель")
    @Timeout(10) // Тест не должен длиться дольше 10 секунд
    void multiThread_ManyProducersOneConsumer_ShouldProcessAllOrders() throws InterruptedException {
        // Given
        int producersCount = 5;
        int ordersPerProducer = 20;
        int totalOrders = producersCount * ordersPerProducer;
        ExecutorService executor = Executors.newFixedThreadPool(producersCount + 1);
        CountDownLatch startLatch = new CountDownLatch(1); // Чтобы все потоки стартовали одновременно
        CountDownLatch finishLatch = new CountDownLatch(totalOrders); // Чтобы дождаться всех операций
        AtomicInteger orderIdCounter = new AtomicInteger(1);
        List<Order> consumedOrders = Collections.synchronizedList(new ArrayList<>());

        // When
        // Создаем производителей
        for (int i = 0; i < producersCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // Ждем сигнала к старту
                    for (int j = 0; j < ordersPerProducer; j++) {
                        orderQueue.put(new Order(orderIdCounter.getAndIncrement(), "Пицца"));
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        // Создаем потребителя
        executor.submit(() -> {
            try {
                startLatch.await(); // Ждем сигнала к старту
                while (consumedOrders.size() < totalOrders) {
                    consumedOrders.add(orderQueue.take());
                    finishLatch.countDown(); // Уменьшаем счетчик
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        startLatch.countDown(); // Сигнал к старту!
        boolean finishedInTime = finishLatch.await(5, TimeUnit.SECONDS); // Ждем завершения всех операций
        executor.shutdownNow(); // Завершаем все потоки

        // Then
        assertTrue(finishedInTime, "Тест не завершился за отведенное время");
        assertEquals(totalOrders, consumedOrders.size(), "Количество полученных заказов не совпадает с отправленными");
        assertTrue(orderQueue.isEmpty(), "Очередь должна быть пуста после теста");
    }
    
    @Test
    @DisplayName("drainTo должен перемещать все элементы в другую коллекцию")
    void drainTo_ShouldMoveAllElements() throws InterruptedException {
        // Given
        orderQueue.put(new Order(1, "Пицца 1"));
        orderQueue.put(new Order(2, "Пицца 2"));
        orderQueue.put(new Order(3, "Пицца 3"));
        List<Order> targetList = new ArrayList<>();
        
        // When
        int drainedCount = orderQueue.drainTo(targetList);
        
        // Then
        assertEquals(3, drainedCount);
        assertEquals(3, targetList.size());
        assertTrue(orderQueue.isEmpty(), "Очередь должна быть пуста после drainTo");
    }
}