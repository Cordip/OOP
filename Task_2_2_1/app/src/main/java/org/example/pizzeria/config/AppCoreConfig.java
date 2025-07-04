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
 * <p>
 * Определяет и настраивает ключевые бины приложения, такие как очередь заказов,
 * склад, списки работников (пекарей и курьеров), а также регистрирует
 * метрики для мониторинга.
 */
@Configuration
@EnableConfigurationProperties(PizzeriaConfig.class)
public class AppCoreConfig {

    private final PizzeriaConfig config;

    @Autowired
    public AppCoreConfig(PizzeriaConfig config) {
        this.config = Objects.requireNonNull(config, "PizzeriaConfig cannot be null");
    }

    /**
     * Создает бин потокобезопасной очереди заказов.
     * @param meterRegistry Реестр для регистрации внутренних метрик очереди.
     * @return Экземпляр {@link OrderQueue}.
     */
    @Bean
    public OrderQueue concurrentOrderQueue(MeterRegistry meterRegistry) {
        return new ConcurrentOrderQueue(config, meterRegistry);
    }

    /**
     * Создает бин потокобезопасного склада готовых пицц.
     * @param meterRegistry Реестр для регистрации внутренних метрик склада.
     * @return Экземпляр {@link Warehouse}.
     */
    @Bean
    public Warehouse concurrentWarehouse(MeterRegistry meterRegistry) {
        return new ConcurrentWarehouse(config, meterRegistry);
    }

    /**
     * Регистрирует метрику (Gauge) для отслеживания текущего размера очереди заказов.
     * @param orderQueue Бин очереди, за которым будет наблюдать метрика.
     * @return {@link MeterBinder} для автоматической регистрации метрики.
     */
    @Bean
    public MeterBinder queueSizeGauge(OrderQueue orderQueue) {
        return registry -> Gauge.builder("pizzeria.queue.size", orderQueue::size)
                .description("Current number of orders waiting in the queue")
                .register(registry);
    }

    /**
     * Регистрирует метрику (Gauge) для отслеживания текущего размера склада.
     * @param warehouse Бин склада, за которым будет наблюдать метрика.
     * @return {@link MeterBinder} для автоматической регистрации метрики.
     */
    @Bean
    public MeterBinder warehouseSizeGauge(Warehouse warehouse) {
        return registry -> Gauge.builder("pizzeria.warehouse.size", warehouse::size)
                .description("Current number of pizzas ready in the warehouse")
                .register(registry);
    }

    /**
     * Создает и настраивает список пекарей как исполняемых задач (Runnable).
     * @param orderQueue Очередь, из которой пекари будут брать заказы.
     * @param warehouse  Склад, на который пекари будут класть готовую пиццу.
     * @param repository Репозиторий для обновления статусов заказов.
     * @return Список пекарей, готовых к запуску в отдельных потоках.
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
                        config,
                        orderQueue,
                        warehouse,
                        repository
                ));
            }
        }
        return bakerRunnables;
    }

    /**
     * Создает и настраивает список курьеров как исполняемых задач (Runnable).
     * @param warehouse  Склад, с которого курьеры будут забирать пиццу.
     * @param repository Репозиторий для обновления статусов заказов.
     * @return Список курьеров, готовых к запуску в отдельных потоках.
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
     * Собирает всех работников (пекарей и курьеров) в один общий список.
     * Этот бин будет внедрен в {@code WorkerManagerService} для запуска всех потоков.
     * @param bakers   Список бинов пекарей.
     * @param couriers Список бинов курьеров.
     * @return Объединенный список всех работников ({@code Runnable}).
     */
    @Bean
    @Qualifier("allWorkers")
    public List<Runnable> allWorkers(List<Runnable> bakers, List<Runnable> couriers) {
        List<Runnable> all = new ArrayList<>();
        all.addAll(bakers);
        all.addAll(couriers);
        return all;
    }
}