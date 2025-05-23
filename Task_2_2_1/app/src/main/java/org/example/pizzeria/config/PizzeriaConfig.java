package org.example.pizzeria.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * Конфигурация пиццерии, загружаемая из application.properties с префиксом "pizzeria".
 * Используется record для неизменяемости и валидации Jakarta Bean Validation.
 */
@ConfigurationProperties(prefix = "pizzeria")
@Validated // Включает валидацию полей, аннотированных JSR-303/Jakarta Validation
public record PizzeriaConfig(

        @Min(value = 1000, message = "Work time must be at least 1000 ms")
        long workTimeMs, // Время работы симуляции

        @Positive(message = "Order queue capacity must be positive")
        int orderQueueCapacity, // Вместимость очереди заказов

        @Positive(message = "Warehouse capacity must be positive")
        int warehouseCapacity, // Вместимость склада

        // Поле loggerType УДАЛЕНО

        String logFilePath, // Путь к файлу лога старой системы (не используется активно)

        @NotBlank(message = "Repository log path cannot be blank")
        String repositoryLogPath, // Путь к файлу репозитория заказов

        @PositiveOrZero(message = "Ingredient cooking multiplier must be non-negative")
        int ingredientCookingMultiplierMs, // Множитель времени готовки

        @PositiveOrZero(message = "Baseline average cook time must be non-negative")
        int baselineAverageCookTimeMs, // Базовое время для расчета мастерства

        @NotEmpty(message = "Bakers list cannot be empty")
        @Valid // Включает валидацию для элементов списка BakerConfig
        List<BakerConfig> bakers, // Список пекарей

        @NotEmpty(message = "Couriers list cannot be empty")
        @Valid // Включает валидацию для элементов списка CourierConfig
        List<CourierConfig> couriers // Список курьеров

        // TODO: Добавить сюда конфигурацию ротации, если нужно:
        // RepositoryLogRotationConfig repositoryLogRotation

) {

    // Вложенные классы для конфигурации работников
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

    // TODO: Добавить record для конфигурации ротации, если нужно
    /*
    public record RepositoryLogRotationConfig(
        boolean enabled,
        @Positive int maxSizeMb
    ) {}
    */
}