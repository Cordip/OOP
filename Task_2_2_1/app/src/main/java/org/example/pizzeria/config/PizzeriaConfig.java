package org.example.pizzeria.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * Конфигурация пиццерии, загружаемая из application.properties с префиксом "pizzeria".
 * Используется для определения всех операционных параметров симуляции.
 *
 * @param workTimeMs Время работы симуляции в миллисекундах.
 * @param orderQueueCapacity Вместимость очереди заказов.
 * @param warehouseCapacity Вместимость склада готовых пицц.
 * @param logFilePath Устаревший путь к файлу лога.
 * @param repositoryLogPath Путь к файлу репозитория заказов для хранения состояния.
 * @param ingredientCookingMultiplierMs Множитель времени готовки за единицу сложности пиццы.
 * @param baselineAverageCookTimeMs Базовое время для расчета мастерства пекарей.
 * @param bakers Список конфигураций пекарей.
 * @param couriers Список конфигураций курьеров.
 */
@ConfigurationProperties(prefix = "pizzeria")
@Validated // Включает валидацию полей, аннотированных JSR-303/Jakarta Validation
public record PizzeriaConfig(

        @Min(value = 1000, message = "Work time must be at least 1000 ms")
        long workTimeMs,

        @Positive(message = "Order queue capacity must be positive")
        int orderQueueCapacity,

        @Positive(message = "Warehouse capacity must be positive")
        int warehouseCapacity,

        String logFilePath,

        @NotBlank(message = "Repository log path cannot be blank")
        String repositoryLogPath,

        @PositiveOrZero(message = "Ingredient cooking multiplier must be non-negative")
        int ingredientCookingMultiplierMs,

        @PositiveOrZero(message = "Baseline average cook time must be non-negative")
        int baselineAverageCookTimeMs,

        @NotEmpty(message = "Bakers list cannot be empty")
        @Valid // Включает валидацию для элементов списка BakerConfig
        List<BakerConfig> bakers,

        @NotEmpty(message = "Couriers list cannot be empty")
        @Valid // Включает валидацию для элементов списка CourierConfig
        List<CourierConfig> couriers

) {

    /**
     * Конфигурация для одного пекаря.
     *
     * @param id Уникальный идентификатор пекаря.
     * @param cookTimeMinMs Минимальное время приготовления (мс).
     * @param cookTimeMaxMs Максимальное время приготовления (мс).
     */
    public record BakerConfig(
            @Positive(message = "Baker ID must be positive") int id,
            @Positive(message = "Baker minimum cook time must be positive") int cookTimeMinMs,
            @Positive(message = "Baker maximum cook time must be positive") int cookTimeMaxMs
    ) {
        public BakerConfig {
            if (cookTimeMinMs > cookTimeMaxMs) {
                throw new IllegalArgumentException("Baker minimum cook time (" + cookTimeMinMs
                        + ") cannot be greater than maximum cook time (" + cookTimeMaxMs + ") for Baker ID " + id);
            }
        }
        public double getAverageBaseTime() { return (cookTimeMinMs + cookTimeMaxMs) / 2.0; }
    }

    /**
     * Конфигурация для одного курьера.
     *
     * @param id Уникальный идентификатор курьера.
     * @param capacity Вместимость багажника курьера (кол-во пицц).
     * @param deliveryTimeMinMs Минимальное время доставки (мс).
     * @param deliveryTimeMaxMs Максимальное время доставки (мс).
     */
    public record CourierConfig(
            @Positive(message = "Courier ID must be positive") int id,
            @Positive(message = "Courier capacity must be positive") int capacity,
            @Positive(message = "Courier minimum delivery time must be positive") int deliveryTimeMinMs,
            @Positive(message = "Courier maximum delivery time must be positive") int deliveryTimeMaxMs
    ) {
        public CourierConfig {
            if (deliveryTimeMinMs > deliveryTimeMaxMs) {
                throw new IllegalArgumentException("Courier minimum delivery time (" + deliveryTimeMinMs
                        + ") cannot be greater than maximum delivery time (" + deliveryTimeMaxMs + ") for Courier ID " + id);
            }
        }
    }
}