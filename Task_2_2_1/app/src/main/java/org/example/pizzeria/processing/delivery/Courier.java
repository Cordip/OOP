package org.example.pizzeria.processing.delivery;

import org.example.pizzeria.config.PizzeriaConfig;
import org.example.pizzeria.domain.order.Order;
import org.example.pizzeria.domain.order.OrderStatus;
import org.example.pizzeria.domain.pizza.Pizza;
import org.example.pizzeria.domain.worker.Worker;
import org.example.pizzeria.order.service.OrderRepository;
import org.example.pizzeria.processing.warehouse.Warehouse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Курьер - работник пиццерии.
 * Забирает пиццы, меняет статус заказов в памяти, логирует через репозиторий,
 * доставляет, меняет статус, логирует.
 * Использует SLF4j для логирования и MDC для трассировки orderId.
 */
public class Courier implements Runnable, Worker {

    private static final Logger log = LoggerFactory.getLogger(Courier.class);
    private static final String MDC_ORDER_ID_KEY = "orderId"; // Ключ для MDC

    private final int id;
    private final PizzeriaConfig.CourierConfig courierConfig;
    private final Warehouse warehouse;
    private final OrderRepository repository;

    private volatile boolean running = true; // Флаг для грациозной остановки

    public Courier(int id,
                   PizzeriaConfig.CourierConfig courierConfig,
                   Warehouse warehouse,
                   OrderRepository repository) {
        this.id = id;
        this.courierConfig = courierConfig;
        this.warehouse = warehouse;
        this.repository = repository;
        log.info("Courier {} initialized. Capacity: {}", id, courierConfig.capacity());
    }

    @Override
    public void run() {
        log.info("Courier {} run loop started.", id);
        List<Pizza> pizzasInBag = Collections.emptyList();
        List<Order> ordersInDelivery = new ArrayList<>(); // Заказы, для которых УСПЕШНО залогирован статус DELIVERING

        try {
            while (running && !Thread.currentThread().isInterrupted()) {
                ordersInDelivery.clear();

                try {
                    log.debug("Courier {} waiting for pizzas at warehouse (capacity: {})...", id, courierConfig.capacity());
                    pizzasInBag = warehouse.take(courierConfig.capacity()); // Блокирующий вызов

                    if (Thread.currentThread().isInterrupted()) {
                        log.warn("Courier {} interrupted after taking pizzas (or during wait).", id);
                        if (!pizzasInBag.isEmpty()) {
                            discardPizzasInBag("interruption after take", pizzasInBag);
                        }
                        break;
                    }

                    if (pizzasInBag.isEmpty()) {
                        log.trace("Courier {} took 0 pizzas. Warehouse might be empty. Waiting again.", id);
                        continue;
                    }

                    String pizzaOrderIds = pizzasInBag.stream()
                                                      .map(p -> String.valueOf(p.getOrderId()))
                                                      .collect(Collectors.joining(", "));
                    log.info("Courier {} took {} pizzas (Orders: {}). Warehouse size: {}", id, pizzasInBag.size(), pizzaOrderIds, warehouse.size());

                    boolean startDeliveryFailed = false;
                    for (Pizza pizza : pizzasInBag) {
                        Order order = null;
                        MDC.put(MDC_ORDER_ID_KEY, String.valueOf(pizza.getOrderId())); // MDC для операций с этим заказом
                        try {
                            order = repository.getOrderById(pizza.getOrderId());
                            if (order != null) {
                                if (order.getStatus() == OrderStatus.COOKED) {
                                    if (order.moveToNextStatus()) { // Переход в DELIVERING
                                        try {
                                            repository.logStatusUpdate(order.getId(), order.getStatus());
                                            ordersInDelivery.add(order); // Добавляем только если статус успешно залогирован
                                            log.info("Status logged as DELIVERING.");
                                        } catch (Exception logEx) {
                                            log.error("Courier {} FAILED to log status DELIVERING. Order removed from this delivery.", id, logEx);
                                            handleDiscard(order);
                                            startDeliveryFailed = true;
                                        }
                                    } else {
                                        log.error("Courier {} failed to change status from COOKED for order. Discarding.", id);
                                        handleDiscard(order);
                                        startDeliveryFailed = true;
                                    }
                                } else {
                                     log.warn("Courier {} found order {} but it's not in COOKED state (state: {}). Skipping for this delivery.", id, order.getId(), order.getStatus());
                                }
                            } else {
                                log.error("Courier {} could not find order for pizza taken from warehouse!", id);
                                startDeliveryFailed = true;
                            }
                        } catch (Exception findEx) {
                             log.error("Courier {} error finding/processing order for pizza {}: {}", id, pizza.getOrderId(), findEx.getMessage(), findEx);
                             startDeliveryFailed = true;
                        } finally {
                             MDC.remove(MDC_ORDER_ID_KEY);
                        }
                    }

                    if (startDeliveryFailed || ordersInDelivery.isEmpty()) {
                        log.error("Courier {} failed to properly start delivery for one or more orders, or no orders collected. Discarding current batch.", id);
                        if (!startDeliveryFailed) {
                             log.info("No orders were ready or found for delivery.");
                        } else {
                             discardPizzasInBag("start delivery failure", pizzasInBag);
                        }
                        ordersInDelivery.clear();
                        continue;
                    }

                    long deliveryTime = calculateDeliveryTime();
                    String deliveringOrderIds = ordersInDelivery.stream()
                            .map(o -> String.valueOf(o.getId()))
                            .collect(Collectors.joining(", "));
                    log.info("Courier {} started delivering {} orders ({}). Estimated time: {}ms", id, ordersInDelivery.size(), deliveringOrderIds, deliveryTime);

                    TimeUnit.MILLISECONDS.sleep(deliveryTime); // Может бросить InterruptedException

                    log.info("Courier {} finished delivery trip for orders ({}).", id, deliveringOrderIds);
                    for (Order deliveredOrder : ordersInDelivery) {
                        MDC.put(MDC_ORDER_ID_KEY, String.valueOf(deliveredOrder.getId())); // MDC для завершения этого заказа
                        try {
                             if (deliveredOrder.moveToNextStatus()) { // Переход в DELIVERED
                                try {
                                    repository.logStatusUpdate(deliveredOrder.getId(), deliveredOrder.getStatus());
                                    log.info("Status logged as DELIVERED.");
                                } catch (Exception logEx) {
                                    log.error("Courier {} CRITICAL: FAILED to log status DELIVERED after successful delivery.", id, logEx);
                                }
                            } else {
                                log.warn("Courier {} could not change status from {} to DELIVERED.", id, deliveredOrder.getStatus());
                            }
                        } catch (Exception finalEx) {
                             log.error("Courier {} error during final status update for order: {}", id, finalEx.getMessage(), finalEx);
                        } finally {
                             MDC.remove(MDC_ORDER_ID_KEY);
                        }
                    }

                    pizzasInBag = Collections.emptyList();
                    ordersInDelivery.clear();

                } catch (InterruptedException e) {
                    log.warn("Courier {} interrupted during delivery processing.", id);
                    if (!ordersInDelivery.isEmpty()) {
                        String discardingOrderIds = ordersInDelivery.stream()
                                .map(o -> String.valueOf(o.getId()))
                                .collect(Collectors.joining(", "));
                        log.warn("Courier {} discarding {} orders ({}) in delivery due to interruption.", id, ordersInDelivery.size(), discardingOrderIds);
                        for (Order orderToDiscard : ordersInDelivery) {
                            handleDiscard(orderToDiscard);
                        }
                    }
                    Thread.currentThread().interrupt();
                    running = false;
                } catch (Exception e) {
                    log.error("Courier {} caught UNEXPECTED exception in run loop.", id, e);
                    if (!ordersInDelivery.isEmpty()) {
                        log.error("Attempting to discard {} orders in delivery due to unexpected error.", ordersInDelivery.size());
                        for (Order orderToDiscard : ordersInDelivery) {
                            handleDiscard(orderToDiscard);
                        }
                    }
                     try { TimeUnit.SECONDS.sleep(1); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); running = false; }
                } finally {
                    MDC.clear(); // Гарантированно очищаем MDC
                    pizzasInBag = Collections.emptyList();
                    ordersInDelivery.clear();
                }
            }
        } finally {
            log.info("Courier {} run loop finished.", id);
            MDC.clear(); // Очистка MDC при завершении потока
        }
    }

    /**
     * Вспомогательный метод для отмены списка заказов по объектам Pizza.
     * Устанавливает и очищает MDC для каждого заказа.
     * @param reason Причина отмены (для лога).
     * @param pizzas Список пицц, чьи заказы нужно отменить.
     */
    private void discardPizzasInBag(String reason, List<Pizza> pizzas) {
        if (pizzas == null || pizzas.isEmpty()) return;
         String pizzaOrderIds = pizzas.stream()
                                       .map(p -> String.valueOf(p.getOrderId()))
                                       .collect(Collectors.joining(", "));
         log.warn("Courier {} discarding orders ({}) based on pizzas in bag. Reason: {}", id, pizzaOrderIds, reason);
         for (Pizza pizza : pizzas) {
             MDC.put(MDC_ORDER_ID_KEY, String.valueOf(pizza.getOrderId()));
             try {
                 Order order = repository.getOrderById(pizza.getOrderId());
                 if (order != null) {
                     handleDiscard(order);
                 } else {
                     log.error("Courier {} could not find order to discard it (based on pizza in bag).", id);
                     try { // Пытаемся залогировать отмену по ID напрямую
                         repository.logStatusUpdate(pizza.getOrderId(), OrderStatus.DISCARDED);
                         log.info("Status logged as DISCARDED by ID.");
                     } catch (Exception logEx) {
                          log.error("Courier {} CRITICAL: FAILED to log status DISCARDED by ID {}.", id, pizza.getOrderId(), logEx);
                     }
                 }
             } catch (Exception findEx){
                  log.error("Courier {} error finding order {} to discard: {}", id, pizza.getOrderId(), findEx.getMessage(), findEx);
             } finally {
                  MDC.remove(MDC_ORDER_ID_KEY);
             }
         }
    }


    /**
     * Вспомогательный метод для отмены заказа и логирования статуса DISCARDED.
     * Устанавливает и очищает MDC для этой операции.
     * @param orderToDiscard Заказ для отмены.
     */
    private void handleDiscard(Order orderToDiscard) {
        if (orderToDiscard == null) return;
        MDC.put(MDC_ORDER_ID_KEY, String.valueOf(orderToDiscard.getId()));
        try {
            log.warn("Handling discard.");
            if (orderToDiscard.discard()) {
                try {
                    repository.logStatusUpdate(orderToDiscard.getId(), orderToDiscard.getStatus());
                    log.info("Status logged as DISCARDED.");
                } catch (Exception logEx) {
                    log.error("Courier {} CRITICAL: FAILED to log status DISCARDED.", id, logEx);
                }
            } else {
                log.warn("Order was already discarded or in final state ({}) when trying to handle discard.", orderToDiscard.getStatus());
            }
        } finally {
            MDC.remove(MDC_ORDER_ID_KEY);
        }
    }

    /**
     * Рассчитывает время доставки.
     */
    private long calculateDeliveryTime() {
        int minTime = courierConfig.deliveryTimeMinMs();
        int maxTime = courierConfig.deliveryTimeMaxMs();
        if (minTime <= 0 || maxTime <= 0 || minTime > maxTime) {
            log.warn("Courier {} has invalid delivery time config (min: {}, max: {}). Using default 1000ms.", id, minTime, maxTime);
            return 1000L;
        }
        return (minTime == maxTime) ? minTime : ThreadLocalRandom.current().nextInt(minTime, maxTime + 1);
    }

    @Override
    public int getId() {
        return id;
    }

    public void stop() {
         log.info("Courier {} received stop signal.", id);
         this.running = false;
    }
}