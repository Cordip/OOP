package org.example.pizzeria.order.api;

import java.util.Optional;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

import org.example.pizzeria.domain.order.Order;
import org.example.pizzeria.domain.order.OrderStatus;
import org.example.pizzeria.order.dto.CreateOrderRequest;
import org.example.pizzeria.order.dto.CreateOrderResponse;
import org.example.pizzeria.order.dto.OrderStatusResponse;
import org.example.pizzeria.order.exception.OrderNotFoundException;
import org.example.pizzeria.order.service.OrderRepository;
import org.example.pizzeria.order.service.OrderAcceptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * REST контроллер для управления заказами пиццы.
 * Использует SLF4j для логирования и MDC для трассировки orderId.
 */
@RestController
@RequestMapping("/api/orders")
@Validated // Разрешает валидацию параметров методов
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);
    private static final String MDC_ORDER_ID_KEY = "orderId"; // Ключ для MDC

    private final OrderAcceptor orderAcceptor;
    private final OrderRepository orderRepository;

    @Autowired
    public OrderController(OrderAcceptor orderAcceptor, OrderRepository orderRepository) {
        this.orderAcceptor = orderAcceptor;
        this.orderRepository = orderRepository;
        log.info("OrderController created and dependencies injected.");
    }

    /**
     * Принимает новый заказ на пиццу.
     * Устанавливает MDC orderId после успешного создания.
     *
     * @param request DTO с деталями заказа.
     * @return ResponseEntity с DTO созданного заказа и статусом 201 Created.
     * @throws InterruptedException  Если поток прерван при добавлении в очередь.
     * @throws IllegalStateException Если сервис не принимает заказы или произошла ошибка при сохранении.
     * @throws Exception             Другие возможные ошибки при обработке.
     */
    @PostMapping
    public ResponseEntity<CreateOrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request)
            throws InterruptedException, IllegalStateException, Exception {

        log.info("Received request to create order with details: '{}'", request.pizzaDetails());

        Integer orderId = null;
        try {
            orderId = orderAcceptor.getNextOrderId();
            MDC.put(MDC_ORDER_ID_KEY, String.valueOf(orderId)); // Устанавливаем MDC СРАЗУ после получения ID
            log.info("Generated new order ID.");

            Order newOrder = new Order(orderId, request.pizzaDetails());
            // Статус RECEIVED будет установлен внутри addOrder

            try {
                orderRepository.addOrder(newOrder); // Внутри addOrder будет лог с MDC
            } catch (IllegalArgumentException | IllegalStateException e) {
                log.warn("Failed to add order to repository: {}", e.getMessage());
                throw e; // Перебрасываем для ControllerAdvice -> 4xx/5xx
            } catch (Exception e) {
                log.error("Unexpected error adding order to repository", e);
                throw new IllegalStateException("Failed to save order state", e); // ControllerAdvice -> 5xx
            }

            orderAcceptor.acceptOrder(newOrder); // Внутри acceptOrder может быть свой лог с MDC

            CreateOrderResponse response = new CreateOrderResponse(orderId);
            log.info("Order created successfully and placed in queue.");
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (InterruptedException e) {
             if (orderId != null) {
                MDC.put(MDC_ORDER_ID_KEY, String.valueOf(orderId)); // Убедимся, что MDC установлен для лога ошибки
             }
             log.warn("Order creation process interrupted.", e);
             Thread.currentThread().interrupt(); // Восстанавливаем флаг прерывания
             throw e;
        } catch (Exception e) {
             if (orderId != null) {
                MDC.put(MDC_ORDER_ID_KEY, String.valueOf(orderId)); // Убедимся, что MDC установлен для лога ошибки
             }
             log.error("Error during order creation process: {}", e.getMessage(), e); // Логгируем ошибку здесь
             throw e; // Перебрасываем для обработки ControllerAdvice
        } finally {
             MDC.remove(MDC_ORDER_ID_KEY); // Очищаем MDC в любом случае
             log.trace("MDC cleared for order creation request.");
        }
    }

    /**
     * Возвращает статус заказа по его ID.
     * Устанавливает и очищает MDC orderId.
     *
     * @param orderId ID заказа (должен быть положительным).
     * @return ResponseEntity с DTO статуса заказа и статусом 200 OK.
     * @throws OrderNotFoundException Если заказ с указанным ID не найден.
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderStatusResponse> getOrderStatus(
            @PathVariable @Min(value = 1, message = "Order ID must be positive") int orderId
    ) {
        MDC.put(MDC_ORDER_ID_KEY, String.valueOf(orderId));
        try {
            log.info("Received request for status.");

            Optional<OrderStatus> statusOptional = orderRepository.findOrderStatusById(orderId);

            if (statusOptional.isEmpty()) {
                log.warn("Order not found for status check.");
                throw new OrderNotFoundException(orderId);
            }

            OrderStatusResponse response = new OrderStatusResponse(orderId, statusOptional.get().name());
            log.info("Returning status: {}", response.status());
            return ResponseEntity.ok(response);

        } catch (OrderNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error getting order status: {}", e.getMessage(), e);
            throw new RuntimeException("Internal error retrieving order status", e);
        } finally {
            MDC.remove(MDC_ORDER_ID_KEY);
            log.trace("MDC cleared for get status request.");
        }
    }
}