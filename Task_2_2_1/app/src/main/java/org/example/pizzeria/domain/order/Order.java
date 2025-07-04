package org.example.pizzeria.domain.order;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Представляет заказ в пиццерии.
 * <p>
 * Этот класс является центральной сущностью домена, отслеживающей жизненный цикл заказа
 * от его получения до доставки. Класс потокобезопасен.
 */
public class Order {

    private static final Logger log = LoggerFactory.getLogger(Order.class);

    private final int id;
    private final String pizzaDetails;
    private volatile OrderStatus status; // volatile для видимости изменений между потоками

    /**
     * Создает новый заказ с начальным статусом {@link OrderStatus#RECEIVED}.
     *
     * @param id           уникальный идентификатор заказа.
     * @param pizzaDetails описание пиццы.
     */
    public Order(int id, String pizzaDetails) {
        validateInput(id, pizzaDetails);
        this.id = id;
        this.pizzaDetails = pizzaDetails;
        this.status = OrderStatus.RECEIVED;
        log.debug("Order {} created with initial status {}", this.id, this.status);
    }

    /**
     * Конструктор для десериализации из JSON. Используется Jackson и системой восстановления.
     */
    @JsonCreator
    Order(@JsonProperty("id") int id,
          @JsonProperty("pizzaDetails") String pizzaDetails,
          @JsonProperty("status") OrderStatus status) {
        validateInput(id, pizzaDetails);
        this.id = id;
        this.pizzaDetails = pizzaDetails;
        this.status = status;
        log.trace("Order {} deserialized with status {}", this.id, this.status);
    }

    private void validateInput(int id, String pizzaDetails) {
        if (id <= 0) {
            throw new IllegalArgumentException("Order ID must be positive, but was " + id);
        }
        if (pizzaDetails == null || pizzaDetails.trim().isEmpty()) {
            throw new IllegalArgumentException("Pizza details cannot be null or empty for Order ID " + id);
        }
    }

    // --- Геттеры ---
    public int getId() { return id; }
    public String getPizzaDetails() { return pizzaDetails; }
    public OrderStatus getStatus() { return status; }

    /**
     * Принудительно устанавливает статус заказа.
     * <p>
     * <b>Внимание:</b> Этот метод предназначен только для внутреннего использования
     * системой восстановления состояния (через {@link OrderRecoveryAccessor}).
     *
     * @param newStatus новый статус для установки.
     */
    synchronized void forceSetStatusInternal(OrderStatus newStatus) {
        Objects.requireNonNull(newStatus, "Cannot set null status via internal setter");
        if (this.status != newStatus) {
            log.trace("Order {} status FORCED internally from {} to {} by recovery accessor.", this.id, this.status, newStatus);
            this.status = newStatus;
        }
    }

    /**
     * Переводит заказ в следующий логический статус.
     * <p>
     * Например, из {@code RECEIVED} в {@code COOKING}. Не выполняет переход
     * из финальных статусов. Ответственность за персистентное сохранение
     * нового статуса лежит на вызывающем коде.
     *
     * @return {@code true} если статус был успешно изменен, иначе {@code false}.
     */
    public synchronized boolean moveToNextStatus() {
        OrderStatus currentStatus = this.status;
        if (currentStatus == null) {
            log.error("Cannot move order {} from null status!", this.id);
            return false;
        }

        OrderStatus nextStatus;
        switch (currentStatus) {
            case RECEIVED:   nextStatus = OrderStatus.COOKING; break;
            case COOKING:    nextStatus = OrderStatus.COOKED; break;
            case COOKED:     nextStatus = OrderStatus.DELIVERING; break;
            case DELIVERING: nextStatus = OrderStatus.DELIVERED; break;
            case DELIVERED:
            case DISCARDED:
                log.trace("Attempted to move order {} from final status {}.", this.id, currentStatus);
                return false; // Нет перехода из финальных статусов
            default:
                log.error("moveToNextStatus called on unexpected status {} for order {}", currentStatus, this.id);
                return false;
        }

        this.status = nextStatus;
        log.debug("Order {} status changed by worker from {} to {}", this.id, currentStatus, this.status);
        return true;
    }

    /**
     * Переводит заказ в статус {@link OrderStatus#DISCARDED}.
     * <p>
     * Используется для отмены заказа на любом этапе.
     *
     * @return {@code true} если статус был успешно изменен, иначе {@code false} (если уже был отменен).
     */
    public synchronized boolean discard() {
        if (this.status == OrderStatus.DISCARDED) {
            log.warn("Attempted to discard order {} which is already DISCARDED.", this.id);
            return false;
        }

        OrderStatus oldStatus = this.status;
        this.status = OrderStatus.DISCARDED;
        log.debug("Order {} status changed by worker from {} to {}", this.id, oldStatus, this.status);
        return true;
    }

    @Override
    public String toString() { return "Order{id=" + id + ", pizzaDetails='" + pizzaDetails + '\'' + ", status=" + status + '}'; }
    @Override
    public boolean equals(Object o) { if (this == o) return true; if (o == null || getClass() != o.getClass()) return false; Order order = (Order) o; return id == order.id; }
    @Override
    public int hashCode() { return Objects.hash(id); }
}