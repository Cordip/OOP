package org.example.pizzeria.processing.kitchen;

import org.example.pizzeria.config.PizzeriaConfig;
import org.example.pizzeria.domain.order.Order;
import org.example.pizzeria.domain.order.OrderStatus;
import org.example.pizzeria.domain.pizza.Pizza;
import org.example.pizzeria.domain.worker.Worker;
import org.example.pizzeria.order.service.OrderRepository;
import org.example.pizzeria.processing.queue.OrderQueue;
import org.example.pizzeria.processing.warehouse.Warehouse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Пекарь - работник пиццерии.
 * Забирает заказы, меняет статус в памяти, затем логирует через репозиторий,
 * готовит, помещает на склад, меняет статус, логирует.
 * Использует SLF4j для логирования и MDC для трассировки orderId.
 */
public class Baker implements Runnable, Worker {

    private static final Logger log = LoggerFactory.getLogger(Baker.class);
    private static final String MDC_ORDER_ID_KEY = "orderId"; // Ключ для MDC

    private final int id;
    private final PizzeriaConfig.BakerConfig bakerConfig;
    private final PizzeriaConfig pizzeriaConfig;
    private final OrderQueue orderQueue;
    private final Warehouse warehouse;
    private final OrderRepository repository;

    private volatile boolean running = true; // Флаг для грациозной остановки

    private final double masteryFactor; // Предрасчитанный фактор мастерства

    public Baker(int id,
                 PizzeriaConfig.BakerConfig bakerConfig,
                 PizzeriaConfig pizzeriaConfig,
                 OrderQueue queue,
                 Warehouse warehouse,
                 OrderRepository repository) {
        this.id = id;
        this.bakerConfig = bakerConfig;
        this.pizzeriaConfig = pizzeriaConfig;
        this.orderQueue = queue;
        this.warehouse = warehouse;
        this.repository = repository;

        long baselineAverageCookTime = Math.max(1L, pizzeriaConfig.baselineAverageCookTimeMs());
        long bakerAverageTime = (long) bakerConfig.getAverageBaseTime();
        this.masteryFactor = (double) bakerAverageTime / baselineAverageCookTime;

        log.info("Baker {} initialized. AvgBaseTime: {}ms, MasteryFactor: {:.2f}",
                 id, bakerAverageTime, this.masteryFactor);
    }

    @Override
    public void run() {
        log.info("Baker {} run loop started.", id);
        Order currentOrder = null;

        try {
            while (running && !Thread.currentThread().isInterrupted()) {
                currentOrder = null;

                try {
                    log.debug("Baker {} waiting for order...", id);
                    currentOrder = orderQueue.take(); // Блокирующий вызов

                    MDC.put(MDC_ORDER_ID_KEY, String.valueOf(currentOrder.getId())); // MDC СРАЗУ после получения заказа
                    log.info("Baker {} took order. Queue size: {}", id, orderQueue.size());

                    if (currentOrder.moveToNextStatus()) { // Переход в COOKING
                        try {
                            repository.logStatusUpdate(currentOrder.getId(), currentOrder.getStatus());
                            log.info("Status logged as COOKING.");
                        } catch (Exception logEx) {
                            log.error("Baker {} FAILED to log status COOKING. Discarding order.", id, logEx);
                            handleDiscard(currentOrder);
                            continue;
                        }
                    } else {
                         log.error("Baker {} failed to change status from {} to COOKING. Discarding.", id, currentOrder.getStatus());
                         handleDiscard(currentOrder);
                         continue;
                    }

                    long cookTime = calculateCookTime(currentOrder);
                    log.info("Baker {} started cooking. Estimated time: {}ms", id, cookTime);
                    TimeUnit.MILLISECONDS.sleep(cookTime); // Может бросить InterruptedException

                    Pizza pizza = new Pizza(currentOrder);

                    log.info("Baker {} finished cooking. Putting pizza on warehouse...", id);
                    warehouse.put(pizza); // Блокирующий вызов
                    log.info("Baker {} put pizza on warehouse. Warehouse size: {}", id, warehouse.size());

                    if (currentOrder.moveToNextStatus()) { // Переход в COOKED
                        try {
                            repository.logStatusUpdate(currentOrder.getId(), currentOrder.getStatus());
                            log.info("Status logged as COOKED.");
                        } catch (Exception logEx) {
                            log.error("Baker {} FAILED to log status COOKED after cooking. Pizza might be stuck! Discarding order.", id, logEx);
                            handleDiscard(currentOrder);
                            continue;
                        }
                    } else {
                        log.error("Baker {} failed to change status from {} to COOKED. Discarding.", id, currentOrder.getStatus());
                        handleDiscard(currentOrder);
                        continue;
                    }

                    log.info("Order processing completed successfully.");

                } catch (InterruptedException e) {
                    log.warn("Baker {} interrupted during order processing.", id);
                    if (currentOrder != null) {
                        MDC.put(MDC_ORDER_ID_KEY, String.valueOf(currentOrder.getId())); // MDC для логов отмены
                        log.warn("Baker {} discarding incomplete order due to interruption.", id);
                        handleDiscard(currentOrder);
                    }
                    Thread.currentThread().interrupt();
                    running = false;
                } catch (Exception e) {
                    log.error("Baker {} encountered an unexpected error during order processing.", id, e);
                    if (currentOrder != null) {
                         MDC.put(MDC_ORDER_ID_KEY, String.valueOf(currentOrder.getId()));
                         log.error("Attempting to discard order due to unexpected error.");
                         handleDiscard(currentOrder);
                    }
                } finally {
                    if (currentOrder != null) { // Очищаем MDC после обработки заказа
                        log.trace("Clearing MDC for order {}", currentOrder.getId());
                    } else {
                         log.trace("Clearing MDC (no active order)");
                    }
                    MDC.remove(MDC_ORDER_ID_KEY); // Всегда очищаем
                    currentOrder = null;
                }
            }
        } finally {
             log.info("Baker {} run loop finished.", id);
             MDC.clear(); // Очистка MDC при завершении потока
        }
    }

    /**
     * Вспомогательный метод для отмены заказа и логирования статуса DISCARDED.
     * Устанавливает и очищает MDC для этой операции.
     * @param orderToDiscard Заказ для отмены.
     */
    private void handleDiscard(Order orderToDiscard) {
        if (orderToDiscard == null) return;
        MDC.put(MDC_ORDER_ID_KEY, String.valueOf(orderToDiscard.getId())); // MDC для операции discard
        try {
            log.warn("Handling discard.");
            if (orderToDiscard.discard()) {
                try {
                    repository.logStatusUpdate(orderToDiscard.getId(), orderToDiscard.getStatus());
                    log.info("Status logged as DISCARDED.");
                } catch (Exception logEx) {
                    log.error("Baker {} CRITICAL: FAILED to log status DISCARDED.", id, logEx);
                }
            } else {
                log.warn("Order was already discarded or in final state when trying to handle discard.");
            }
        } finally {
            MDC.remove(MDC_ORDER_ID_KEY); // Очищаем MDC после операции discard
        }
    }

    /**
     * Рассчитывает время готовки с учетом сложности заказа и мастерства пекаря.
     */
    private long calculateCookTime(Order order) {
         int complexity = getComplexityUnits(order);
         int ingredientTime = complexity * pizzeriaConfig.ingredientCookingMultiplierMs();
         int minBase = bakerConfig.cookTimeMinMs();
         int maxBase = bakerConfig.cookTimeMaxMs();
         int bakerBaseTime = (minBase == maxBase) ? minBase : ThreadLocalRandom.current().nextInt(minBase, maxBase + 1);

         long rawTime = bakerBaseTime + ingredientTime;
         long finalCookTime = Math.max(100L, (long) (rawTime * this.masteryFactor)); // Минимум 100мс

         log.debug("Calculated cook time: base={}, ingredient={}, raw={}, masteryFactor={:.2f}, final={}ms",
                   bakerBaseTime, ingredientTime, rawTime, masteryFactor, finalCookTime);
         return finalCookTime;
    }

    /**
     * Рассчитывает единицы сложности пиццы.
     */
     private int getComplexityUnits(Order order) {
          if (order == null || order.getPizzaDetails() == null) { return 1; }
          return Math.max(1, order.getPizzaDetails().length() / 5); // Пример: сложность = длина описания / 5
     }

    @Override
    public int getId() {
        return id;
    }

    public void stop() {
         log.info("Baker {} received stop signal.", id);
         this.running = false;
    }
}