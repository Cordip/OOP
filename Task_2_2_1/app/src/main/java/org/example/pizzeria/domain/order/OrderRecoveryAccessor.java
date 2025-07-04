package org.example.pizzeria.domain.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.Objects;

/**
 * Утилитарный класс, предоставляющий контролируемый доступ для создания
 * и установки статуса объектов {@link Order} ИСКЛЮЧИТЕЛЬНО в целях
 * восстановления состояния из хранилища (например, при загрузке из лога репозиторием).
 * Использует package-private доступ к конструктору и методам Order.
 * Все методы статические.
 */
public final class OrderRecoveryAccessor {

    private OrderRecoveryAccessor() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Создает экземпляр Order при восстановлении из данных хранилища (например, JSON).
     * Вызывает package-private конструктор Order, предназначенный для десериализации.
     *
     * @param orderData Карта данных, представляющая сериализованный Order.
     * @param objectMapper Экземпляр ObjectMapper для конвертации.
     * @return Новый экземпляр Order, восстановленный из данных.
     * @throws NullPointerException если orderData или objectMapper null.
     * @throws IllegalArgumentException если конвертация не удалась или данные некорректны.
     */
    public static Order createOrderFromData(Map<String, Object> orderData, ObjectMapper objectMapper) {
        Objects.requireNonNull(orderData, "Order data map cannot be null");
        Objects.requireNonNull(objectMapper, "ObjectMapper cannot be null");
        try {
            // objectMapper использует @JsonCreator конструктор Order(id, details, status).
            return objectMapper.convertValue(orderData, Order.class);
        } catch (Exception e) {
            // Логирование ошибки лучше делать в вызывающем коде (репозитории).
            throw new IllegalArgumentException("Failed to create Order from provided data: " + e.getMessage(), e);
        }
    }


    /**
     * Принудительно устанавливает статус заказа через package-private метод Order.
     * Использовать ТОЛЬКО при восстановлении состояния.
     *
     * @param order Экземпляр Order, статус которого нужно установить.
     * @param status Новый статус.
     * @throws NullPointerException если order или status null.
     */
    public static void forceSetStatus(Order order, OrderStatus status) {
        Objects.requireNonNull(order, "Order cannot be null for setting status").forceSetStatusInternal(
                Objects.requireNonNull(status, "Status cannot be null for setting status")
        );
    }
}