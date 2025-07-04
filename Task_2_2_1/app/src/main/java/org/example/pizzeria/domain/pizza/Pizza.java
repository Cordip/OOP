package org.example.pizzeria.domain.pizza;

import java.util.Objects;
import org.example.pizzeria.domain.order.Order;

/**
 * Представляет готовую пиццу, предназначенную для хранения на складе.
 * <p>
 * Создается из объекта {@link Order} после завершения этапа приготовления.
 */
public class Pizza {
    private final int orderId;
    private final String pizzaDetails;

    /**
     * Создает объект пиццы на основе заказа.
     *
     * @param order заказ, из которого создается пицца. Не может быть null.
     * @throws IllegalArgumentException если order равен null.
     */
    public Pizza(Order order) {
        if (order == null) {
            throw new IllegalArgumentException("Order cannot be null when creating Pizza");
        }
        this.orderId = order.getId();
        this.pizzaDetails = order.getPizzaDetails();
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