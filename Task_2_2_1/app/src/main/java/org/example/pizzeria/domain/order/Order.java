package org.example.pizzeria.domain.order;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Представляет заказ в пиццерии.
 * Содержит информацию о заказе и его текущем статусе.
 * Изменение статуса выполняется через публичные методы moveToNextStatus/discard,
 * ответственность за логирование лежит на вызывающем коде.
 * Содержит package-private конструктор и метод setStatusInternal для использования
 * ТОЛЬКО классом OrderRecoveryAccessor при восстановлении состояния.
 */
public class Order {

    private static final Logger log = LoggerFactory.getLogger(Order.class);

    private final int id;
    private final String pizzaDetails;
    private volatile OrderStatus status; // volatile для видимости между потоками

    // --- Конструкторы ---

    /**
     * Публичный конструктор для СОЗДАНИЯ НОВОГО заказа в системе.
     * Устанавливает начальный статус RECEIVED.
     * @param id ID нового заказа (должен быть > 0).
     * @param pizzaDetails Описание пиццы (не null и не пустое).
     */
    public Order(int id, String pizzaDetails) {
        validateInput(id, pizzaDetails);
        this.id = id;
        this.pizzaDetails = pizzaDetails;
        this.status = OrderStatus.RECEIVED;
        log.debug("Order {} created with initial status {}", this.id, this.status);
    }

    /**
     * Конструктор для десериализации из JSON (например, при чтении лога).
     * Используется Jackson или кодом восстановления.
     * Сделан package-private для контролируемого использования.
     * Аннотация @JsonCreator указывает Jackson использовать этот конструктор.
     *
     * @param id ID заказа из JSON.
     * @param pizzaDetails Детали из JSON.
     * @param status Статус из JSON.
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
     * Устанавливает статус заказа НАПРЯМУЮ.
     * Доступен ТОЛЬКО классам в пакете (используется OrderRecoveryAccessor).
     * НЕ вызывает логирование статуса в репозиторий.
     *
     * @param newStatus Новый статус для установки.
     * @throws NullPointerException если newStatus == null.
     */
    synchronized void forceSetStatusInternal(OrderStatus newStatus) {
        Objects.requireNonNull(newStatus, "Cannot set null status via internal setter");
        if (this.status != newStatus) {
            log.trace("Order {} status FORCED internally from {} to {} by recovery accessor.", this.id, this.status, newStatus);
            this.status = newStatus;
        }
    }

    // --------------------------------------------------------------------
    // Публичные методы изменения статуса (для Baker/Courier)
    // --------------------------------------------------------------------

    /**
     * Пытается перевести заказ в следующий логический статус (в памяти).
     * Ответственность за вызов repository.logStatusUpdate ЛЕЖИТ НА ВЫЗЫВАЮЩЕМ КОДЕ.
     * Потокобезопасен.
     *
     * @return {@code true} если статус был успешно изменен, {@code false} если переход невозможен.
     */
    public synchronized boolean moveToNextStatus() {
        OrderStatus currentStatus = this.status;
        if (currentStatus == null) { // На случай десериализации без статуса
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
     * Пытается перевести заказ в статус DISCARDED (в памяти).
     * Ответственность за вызов repository.logStatusUpdate ЛЕЖИТ НА ВЫЗЫВАЮЩЕМ КОДЕ.
     * Потокобезопасен.
     *
     * @return {@code true} если статус был успешно изменен на DISCARDED, {@code false} если уже был DISCARDED.
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