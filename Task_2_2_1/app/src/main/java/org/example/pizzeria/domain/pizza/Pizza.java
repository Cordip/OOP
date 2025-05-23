package org.example.pizzeria.domain.pizza;

import java.util.Objects;
import org.example.pizzeria.domain.order.Order;

/**
 * Готовая пицца, предназначенная для хранения на складе.
 */
public class Pizza {
    private final int orderId;
    private final String pizzaDetails;

    public Pizza(Order order) {
        if (order == null) {
            throw new IllegalArgumentException("Order cannot be null when creating Pizza");
        }
        this.orderId = order.getId();
        this.pizzaDetails = order.getPizzaDetails();
        // Статус заказа меняется на COOKED *после* успешного помещения пиццы на склад.
    }

    public int getOrderId() {
        return orderId;
    }

    public String getPizzaDetails() {
        return pizzaDetails;
    }

    @Override
    public String toString() {
        return "Pizza{" +
               "orderId=" + orderId +
               ", pizzaDetails='" + pizzaDetails + '\'' +
               '}';
    }

     @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pizza pizza = (Pizza) o;
        return orderId == pizza.orderId; // Пиццы равны, если относятся к одному заказу.
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderId);
    }
}