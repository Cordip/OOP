package org.example.pizzeria.order.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.pizzeria.config.PizzeriaConfig;
import org.example.pizzeria.domain.order.Order;
import org.example.pizzeria.domain.order.OrderStatus;
import org.example.pizzeria.order.dto.CreateOrderRequest;
import org.example.pizzeria.order.dto.CreateOrderResponse;
import org.example.pizzeria.order.service.OrderRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.NoSuchFileException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.*;
// import static org.junit.jupiter.api.Assertions.fail; // Не используется
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class) // Выполнять тесты в указанном порядке
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD) // Пересоздавать контекст после каждого теста
class OrderControllerIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(OrderControllerIntegrationTest.class);

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private OrderRepository orderRepository;
    @Autowired private PizzeriaConfig config;
    @Autowired private ApplicationContext applicationContext; // Не используется напрямую, но может быть полезен для отладки

    @TempDir static Path tempDir; // Временная директория для тестовых файлов
    private static Path testRepoLogPath;

    @DynamicPropertySource // Динамически устанавливаем путь к логу репозитория для теста
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        testRepoLogPath = tempDir.resolve("integration_test_orders.log");
        log.info("Setting test repository log path to: {}", testRepoLogPath);
        registry.add("pizzeria.repository-log-path", testRepoLogPath::toString);
        registry.add("pizzeria.work-time-ms", () -> "999999"); // Увеличиваем время работы, чтобы симуляция не завершилась во время тестов
    }

    @BeforeEach
    void setUp() {
         log.info("Test setup: Log path is {}", testRepoLogPath);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (Files.exists(testRepoLogPath)) {
            log.info("--- Repository Log ({}) Content After Test ---", testRepoLogPath);
            try { Files.readAllLines(testRepoLogPath).forEach(line -> log.info("  {}", line)); }
            catch (IOException e) { log.error("Failed to read repository log after test: {}", e.getMessage()); }
            log.info("--- End Repository Log ---");
            try {
                 log.info("Deleting repository log file after test: {}", testRepoLogPath);
                 Files.delete(testRepoLogPath);
            } catch (NoSuchFileException e) { log.info("Log file {} was already deleted or not created.", testRepoLogPath); }
            catch (IOException e) { log.error("Failed to delete repository log file {}: {}", testRepoLogPath, e.getMessage()); }
        } else {
             log.info("--- Repository Log ({}) Not Found After Test ---", testRepoLogPath);
        }
    }

    @Test
    @org.junit.jupiter.api.Order(1)
    @DisplayName("POST /api/orders - Success")
    void testCreateOrder_Success() throws Exception {
        String pizzaDetails = "Тестовая Пепперони";
        CreateOrderRequest request = new CreateOrderRequest(pizzaDetails);
        int expectedOrderId = 1; // Ожидаем, что первый заказ получит ID = 1

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(is(expectedOrderId)))
                .andReturn();

        CreateOrderResponse responseDto = objectMapper.readValue(result.getResponse().getContentAsString(), CreateOrderResponse.class);
        int createdOrderId = responseDto.orderId();
        assertThat(createdOrderId).isEqualTo(expectedOrderId);

        // Ждем появления заказа в активном кэше репозитория
        final Order[] savedOrderHolder = new Order[1];
        await()
            .alias("Waiting for order in active cache")
            .atMost(2, TimeUnit.SECONDS)
            .pollInterval(100, TimeUnit.MILLISECONDS)
            .untilAsserted(() -> {
                Order order = orderRepository.getOrderById(createdOrderId);
                assertThat(order).isNotNull();
                savedOrderHolder[0] = order;
            });
        log.info("Order {} verified in repository cache.", createdOrderId);

        Order savedOrder = savedOrderHolder[0];
        assertThat(savedOrder.getId()).isEqualTo(createdOrderId);
        assertThat(savedOrder.getPizzaDetails()).isEqualTo(pizzaDetails);
        assertThat(savedOrder.getStatus()).isIn(OrderStatus.RECEIVED, OrderStatus.COOKING); // Статус должен быть RECEIVED или уже COOKING

        // Ждем создания лог-файла и проверяем наличие CREATE события
        await()
            .alias("Waiting for repo log file")
            .atMost(1, TimeUnit.SECONDS)
            .pollInterval(50, TimeUnit.MILLISECONDS)
            .until(() -> Files.exists(testRepoLogPath));
        log.info("Repository log file exists.");

        List<String> logLines = Files.readAllLines(testRepoLogPath);
         assertThat(logLines.stream()
                .anyMatch(line -> line.contains("\"eventType\":\"CREATE\"") && line.contains("\"id\":" + createdOrderId)))
                .withFailMessage("CREATE event for order %d not found in log file %s", createdOrderId, testRepoLogPath)
                .isTrue();
         log.info("CREATE event verified in repository log.");
    }

    @Test
    @org.junit.jupiter.api.Order(2)
    @DisplayName("POST /api/orders - Invalid Request (Blank Details)")
    void testCreateOrder_InvalidRequest() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(" "); // Невалидные детали
        mockMvc.perform(MockMvcRequestBuilders.post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(containsString("'pizzaDetails': Pizza details cannot be blank")));

        assertThat(orderRepository.count()).isZero(); // Заказ не должен быть создан
        assertThat(Files.exists(testRepoLogPath))
            .withFailMessage("Repository log file %s should exist after repository initialization.", testRepoLogPath)
            .isTrue();
        assertThat(Files.size(testRepoLogPath)) // Лог-файл должен быть пуст
            .withFailMessage("Repository log file %s should be empty as no valid order was created, but has size %d.", testRepoLogPath, Files.exists(testRepoLogPath) ? Files.size(testRepoLogPath) : -1)
            .isZero();
    }

    @Test
    @org.junit.jupiter.api.Order(3)
    @DisplayName("GET /api/orders/{id} - Success (Checks recent status)")
    void testGetOrderStatus_Success() throws Exception {
        int orderId = createOrderViaApi("Статус Тест API"); // Создаем заказ

        // Ждем и проверяем, что статус заказа корректный (RECEIVED или COOKING)
        await()
            .alias("Waiting for valid status for order")
            .atMost(2, TimeUnit.SECONDS)
            .pollInterval(100, TimeUnit.MILLISECONDS)
            .untilAsserted(() ->
                mockMvc.perform(MockMvcRequestBuilders.get("/api/orders/{id}", orderId)
                                .accept(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.orderId").value(orderId))
                        .andExpect(jsonPath("$.status").value(anyOf(is(OrderStatus.RECEIVED.name()), is(OrderStatus.COOKING.name()))))
        );
        log.info("Verified initial status for order {} via API.", orderId);
    }

    @Test
    @org.junit.jupiter.api.Order(4)
    @DisplayName("GET /api/orders/{id} - Not Found")
    void testGetOrderStatus_NotFound() throws Exception {
         int nonExistentId = 999;
         mockMvc.perform(MockMvcRequestBuilders.get("/api/orders/{id}", nonExistentId)
                         .accept(MediaType.APPLICATION_JSON))
                 .andExpect(status().isNotFound())
                 .andExpect(jsonPath("$.status").value(404))
                 .andExpect(jsonPath("$.message").value("Order not found with ID: " + nonExistentId));
    }

    @Test
    @org.junit.jupiter.api.Order(5)
    @DisplayName("GET /api/orders/{id} - Invalid ID (Negative)")
    void testGetOrderStatus_InvalidId() throws Exception {
         int invalidId = -5;
         mockMvc.perform(MockMvcRequestBuilders.get("/api/orders/{id}", invalidId)
                         .accept(MediaType.APPLICATION_JSON))
                 .andExpect(status().isBadRequest())
                 .andExpect(jsonPath("$.status").value(400))
                 .andExpect(jsonPath("$.message").value(containsString("Order ID must be positive")));
    }

    @Test
    @org.junit.jupiter.api.Order(10)
    @DisplayName("Full Order Lifecycle via API: POST -> ... -> Final State (DELIVERED)")
    void testOrderLifecycle_ViaApi() throws Exception {
        int orderId = createOrderViaApi("Жизненный Цикл Пицца");
        log.info("Lifecycle Test: Order created with ID: {}", orderId);

        // Определяем максимальное время ожидания на основе конфигурации пекарей и курьеров
        long maxBakerTime = config.bakers().stream().mapToLong(b -> b.cookTimeMaxMs()).max().orElse(2000L);
        long maxCourierTime = config.couriers().stream().mapToLong(c -> c.deliveryTimeMaxMs()).max().orElse(3000L);
        long maxWaitTimeSeconds = TimeUnit.MILLISECONDS.toSeconds(maxBakerTime + maxCourierTime + 7000); // + запас
        maxWaitTimeSeconds = Math.min(maxWaitTimeSeconds, 45); // Ограничиваем сверху, чтобы тест не был слишком долгим

        // Ждем появления заказа в активном кэше
        await()
            .alias("Waiting for order in active cache (lifecycle)")
            .atMost(2, TimeUnit.SECONDS)
            .untilAsserted(() -> assertThat(orderRepository.getOrderById(orderId)).isNotNull());
        log.info("Lifecycle Test: Order {} appeared in active cache.", orderId);

        // Ждем, пока заказ ИСЧЕЗНЕТ из активного кэша (т.е. достигнет финального статуса)
        log.info("Lifecycle Test: Waiting up to {}s for order {} to leave active cache (reach final state)...", maxWaitTimeSeconds, orderId);
        await()
            .alias("Waiting for order to leave active cache (lifecycle)")
            .atMost(maxWaitTimeSeconds, TimeUnit.SECONDS)
            .pollInterval(500, TimeUnit.MILLISECONDS)
            .until(() -> orderRepository.getOrderById(orderId) == null);
        log.info("Lifecycle Test: Order {} left active cache.", orderId);

        // Проверяем лог на наличие статуса DELIVERED и корректную последовательность статусов
        assertThat(Files.exists(testRepoLogPath))
             .withFailMessage("Repository log file should exist after order reached final state")
             .isTrue();

        List<String> finalLogLines = Files.readAllLines(testRepoLogPath);
        assertThat(finalLogLines.stream() // Проверка наличия DELIVERED
                .anyMatch(line -> line.contains("\"orderId\":" + orderId)
                              && line.contains("\"eventType\":\"STATUS_UPDATE\"")
                              && line.contains("\"newStatus\":\"" + OrderStatus.DELIVERED.name() + "\"")))
                .withFailMessage("DELIVERED status update not found in log for order " + orderId)
                .isTrue();

        assertThat(finalLogLines.stream() // Проверка последовательности статусов
                .filter(line -> line.contains("\"orderId\":" + orderId) || (line.contains("\"eventType\":\"CREATE\"") && line.contains("\"id\":"+orderId)))
                .filter(line -> line.contains("\"eventType\":\"STATUS_UPDATE\"") || line.contains("\"eventType\":\"CREATE\""))
                .map(this::extractStatusOrType) // Вспомогательный метод для извлечения статуса/типа
                .filter(Objects::nonNull)
                .toList())
                .withFailMessage("Incorrect status sequence found in log for order " + orderId)
                .containsSubsequence( // Проверяем, что эти статусы идут в таком порядке (могут быть и другие между ними)
                        "CREATE",
                        OrderStatus.COOKING.name(),
                        OrderStatus.COOKED.name(),
                        OrderStatus.DELIVERING.name(),
                        OrderStatus.DELIVERED.name()
                );
        log.info("Lifecycle Test: Repository log sequence verified for order {}.", orderId);
    }

    // Вспомогательный метод для создания заказа через API
    private int createOrderViaApi(String pizzaDetails) throws Exception {
          CreateOrderRequest request = new CreateOrderRequest(pizzaDetails);
          MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/api/orders")
                          .contentType(MediaType.APPLICATION_JSON)
                          .content(objectMapper.writeValueAsString(request)))
                  .andExpect(status().isCreated())
                  .andReturn();
          CreateOrderResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), CreateOrderResponse.class);
          return response.orderId();
     }

     // Вспомогательный метод для извлечения статуса или типа из строки лога JSON
     private String extractStatusOrType(String logLine) {
          try {
              var node = objectMapper.readTree(logLine);
              if (node.has("eventType")) {
                  String eventType = node.get("eventType").asText();
                  if ("CREATE".equals(eventType)) {
                      return "CREATE";
                  } else if ("STATUS_UPDATE".equals(eventType) && node.has("newStatus")) {
                      return node.get("newStatus").asText();
                  }
              }
          } catch (Exception e) { /* Игнорируем ошибки парсинга, если строка не является нужным событием */ }
          return null;
     }
}