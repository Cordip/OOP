package org.example.pizzeria.order.exception;

/**
 * Исключение, выбрасываемое, когда заказ с указанным ID не найден в репозитории.
 */
public class OrderNotFoundException extends RuntimeException {
    private final int orderId;

    public OrderNotFoundException(int orderId) {
        super("Order not found with ID: " + orderId);
        this.orderId = orderId;
    }

    public int getOrderId() {
        return orderId;
    }
}