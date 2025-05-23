package org.example.pizzeria.config;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.example.pizzeria.order.service.OrderRepository;
import org.example.pizzeria.processing.delivery.Courier;
import org.example.pizzeria.processing.kitchen.Baker;
import org.example.pizzeria.processing.queue.OrderQueue;
import org.example.pizzeria.processing.queue.impl.ConcurrentOrderQueue;
import org.example.pizzeria.processing.warehouse.Warehouse;
import org.example.pizzeria.processing.warehouse.impl.ConcurrentWarehouse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Основной конфигурационный класс Spring.
 * Включает загрузку свойств из PizzeriaConfig и определяет бины
 * для основных компонентов симуляции (очередь, склад), списки работников
 * и регистрирует метрики для наблюдаемости.
 */
@Configuration
@EnableConfigurationProperties(PizzeriaConfig.class) // Активирует @ConfigurationProperties для PizzeriaConfig
public class AppCoreConfig {

    private final PizzeriaConfig config;
    private final MeterRegistry meterRegistry;

    @Autowired
    public AppCoreConfig(PizzeriaConfig config, MeterRegistry meterRegistry) {
        this.config = Objects.requireNonNull(config, "PizzeriaConfig cannot be null");
        this.meterRegistry = Objects.requireNonNull(meterRegistry, "MeterRegistry cannot be null");
    }

    // --- Компоненты ядра (Очередь, Склад) ---
    // Репозиторий создается через @Repository

    /**
     * Создает бин потокобезопасной очереди заказов с ограниченной емкостью.
     * Передает MeterRegistry для внутренних счетчиков.
     */
    @Bean
    public OrderQueue concurrentOrderQueue() {
        try {
            return new ConcurrentOrderQueue(config, meterRegistry);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create ConcurrentOrderQueue bean", e);
        }
    }

    /**
     * Создает бин потокобезопасного склада пицц с ограниченной емкостью.
     * Передает MeterRegistry для внутренних счетчиков.
     */
    @Bean
    public Warehouse concurrentWarehouse() {
        try {
            return new ConcurrentWarehouse(config, meterRegistry);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create ConcurrentWarehouse bean", e);
        }
    }

    // --- Метрики для Очереди и Склада (Gauge) ---

    /**
     * Регистрирует метрику Gauge для отслеживания текущего размера очереди заказов.
     * Использует MeterBinder для чистой регистрации.
     *
     * @param orderQueue    Бин очереди.
     * @param meterRegistry Реестр метрик.
     * @return MeterBinder для регистрации метрики.
     */
    @Bean
    public MeterBinder queueSizeGauge(OrderQueue orderQueue, MeterRegistry meterRegistry) {
        return registry -> Gauge.builder("pizzeria.queue.size", orderQueue::size)
                .description("Current number of orders waiting in the queue")
                .tags("component", "orderQueue") // Теги для лучшей фильтрации
                .register(registry);
    }

    /**
     * Регистрирует метрику Gauge для отслеживания текущего размера склада пицц.
     * Использует MeterBinder для чистой регистрации.
     *
     * @param warehouse     Бин склада.
     * @param meterRegistry Реестр метрик.
     * @return MeterBinder для регистрации метрики.
     */
    @Bean
    public MeterBinder warehouseSizeGauge(Warehouse warehouse, MeterRegistry meterRegistry) {
        return registry -> Gauge.builder("pizzeria.warehouse.size", warehouse::size)
                .description("Current number of pizzas ready in the warehouse")
                .tags("component", "warehouse") // Теги для лучшей фильтрации
                .register(registry);
    }


    // --- Работники (как списки Runnable) ---

    /**
     * Создает список бинов пекарей (как Runnable).
     * Зависит от репозитория, очереди и склада.
     *
     * @param orderQueue Очередь заказов.
     * @param warehouse  Склад пицц.
     * @param repository Репозиторий заказов.
     * @return Список Runnable пекарей.
     */
    @Bean
    public List<Runnable> bakers(OrderQueue orderQueue,
                                 Warehouse warehouse,
                                 OrderRepository repository) {
        List<Runnable> bakerRunnables = new ArrayList<>();
        if (config.bakers() != null) {
            for (PizzeriaConfig.BakerConfig bakerCfg : config.bakers()) {
                bakerRunnables.add(new Baker(
                        bakerCfg.id(),
                        bakerCfg,
                        config, // Передаем весь конфиг (например, для расчета времени)
                        orderQueue,
                        warehouse,
                        repository
                ));
            }
        }
        return bakerRunnables;
    }

    /**
     * Создает список бинов курьеров (как Runnable).
     * Зависит от репозитория и склада.
     *
     * @param warehouse  Склад пицц.
     * @param repository Репозиторий заказов.
     * @return Список Runnable курьеров.
     */
    @Bean
    public List<Runnable> couriers(Warehouse warehouse,
                                   OrderRepository repository) {
        List<Runnable> courierRunnables = new ArrayList<>();
        if (config.couriers() != null) {
            for (PizzeriaConfig.CourierConfig courierCfg : config.couriers()) {
                courierRunnables.add(new Courier(
                        courierCfg.id(),
                        courierCfg,
                        warehouse,
                        repository
                ));
            }
        }
        return courierRunnables;
    }

    /**
     * Собирает всех работников (пекарей и курьеров) в один список Runnable.
     * Используется для внедрения в WorkerManagerService.
     *
     * @param bakers   Список бинов пекарей.
     * @param couriers Список бинов курьеров.
     * @return Объединенный список всех работников.
     */
    @Bean
    @Qualifier("allWorkers") // Имя объединенного списка для внедрения по квалификатору
    public List<Runnable> allWorkers(List<Runnable> bakers, List<Runnable> couriers) {
        List<Runnable> all = new ArrayList<>();
        if (bakers != null) {
            all.addAll(bakers);
        }
        if (couriers != null) {
            all.addAll(couriers);
        }
        return all;
    }

    // ================================================================================
    // Компоненты, создаваемые через аннотации (@Repository, @Service, @Component):
    // - FileOrderRepository (@Repository)
    // - PizzeriaOrderAcceptorService (@Service)
    // - WorkerManagerService (@Service)
    // - ApplicationLifecycleManager (@Component)
    // ================================================================================

}