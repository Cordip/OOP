package org.example.pizzeria.order.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.example.pizzeria.config.PizzeriaConfig;
import org.example.pizzeria.domain.order.Order;
import org.example.pizzeria.domain.order.OrderRecoveryAccessor;
import org.example.pizzeria.domain.order.OrderStatus;
import org.example.pizzeria.order.service.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Реализация репозитория заказов {@link OrderRepository} на основе файла.
 * Использует append-only JSON лог для персистентности и два кэша в памяти
 * (для активных и финальных заказов) для быстрого доступа.
 * <p>
 * Ключевые особенности:
 * <ul>
 *     <li>Использует {@link OrderStatus#isFinal()} для определения финальных статусов.</li>
 *     <li>Использует {@link OrderRecoveryAccessor} для безопасного создания и установки статуса
 *         объектов {@link Order} при восстановлении состояния из лога.</li>
 *     <li>Реализует простую ротацию лог-файла по размеру при старте.</li>
 *     <li>Интегрирован с Micrometer для предоставления метрик операций.</li>
 *     <li>Использует SLF4j для логирования.</li>
 *     <li>Потокобезопасность кэшей и записи в файл обеспечивается через {@code synchronized} блоки.</li>
 * </ul>
 * Является Spring-бином (@Repository).
 */
@Repository("fileOrderRepository")
public class FileOrderRepository implements OrderRepository, AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(FileOrderRepository.class);

    private static final String EVENT_TYPE = "eventType";
    private static final String EVENT_ORDER = "order"; // Для события CREATE
    private static final String EVENT_ORDER_ID = "orderId"; // Для события STATUS_UPDATE
    private static final String EVENT_NEW_STATUS = "newStatus"; // Для события STATUS_UPDATE
    private static final String EVENT_TIMESTAMP = "timestamp";
    private static final String TYPE_CREATE = "CREATE";
    private static final String TYPE_STATUS_UPDATE = "STATUS_UPDATE";

    private final Path logFilePath;
    private final PizzeriaConfig config;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    /** Кэш активных заказов (полные объекты Order). Ключ - ID заказа. */
    private final Map<Integer, Order> activeOrderCache = new HashMap<>();
    /** Кэш финальных статусов заказов (только статус). Ключ - ID заказа. */
    private final Map<Integer, OrderStatus> finalizedOrderStatusCache = new HashMap<>();
    private final Object cacheLock = new Object(); // Блокировка для синхронизации доступа к кэшам

    private PrintWriter logWriter;
    private final Object fileLock = new Object(); // Блокировка для синхронизации доступа к файлу лога
    private boolean closed = false;
    private final Object stateLock = new Object(); // Блокировка для управления состоянием загрузки
    private volatile boolean isLoading = false; // Флаг, активный во время загрузки состояния из лога
    private final AtomicInteger nextId = new AtomicInteger(1); // Потокобезопасный счетчик ID

    private final Counter addOrderCounter;
    private final Counter logStatusUpdateCounter;
    private final Counter getOrderCounter;
    private final Counter fileWriteCounter;
    private final Counter fileReadCounter;

    private static final TypeReference<Map<String, Object>> MAP_TYPE_REFERENCE = new TypeReference<>() {};

    @Autowired
    public FileOrderRepository(PizzeriaConfig config, ObjectMapper objectMapper, MeterRegistry meterRegistry) {
        this.config = Objects.requireNonNull(config, "PizzeriaConfig cannot be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "ObjectMapper cannot be null");
        this.meterRegistry = Objects.requireNonNull(meterRegistry, "MeterRegistry cannot be null");
        this.logFilePath = Paths.get(config.repositoryLogPath());

        this.addOrderCounter = meterRegistry.counter("pizzeria.repository.operations.total", "type", "addOrder");
        this.logStatusUpdateCounter = meterRegistry.counter("pizzeria.repository.operations.total", "type", "logStatusUpdate");
        this.getOrderCounter = meterRegistry.counter("pizzeria.repository.operations.total", "type", "getOrder");
        this.fileWriteCounter = meterRegistry.counter("pizzeria.repository.io.total", "type", "write");
        this.fileReadCounter = meterRegistry.counter("pizzeria.repository.io.total", "type", "read");

        log.info("FileOrderRepository bean created. Log path: {}. Metrics registered.", this.logFilePath);
    }

    /**
     * Инициализация репозитория.
     * Выполняет создание директории, ротацию лога, загрузку состояния из лога
     * и открытие файла для записи.
     */
    @PostConstruct
    public void initialize() {
        log.info("FileOrderRepository @PostConstruct: Initializing...");
        try {
            Path parentDir = logFilePath.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                log.info("Creating directory for log file: {}", parentDir);
                Files.createDirectories(parentDir);
            }
            rotateLogFileIfNeeded();
            loadFromLog();
            openLogWriter();
            log.info("FileOrderRepository @PostConstruct: Initialization complete. Next order ID: {}", nextId.get());
        } catch (IOException e) {
            log.error("FATAL: Failed to initialize FileOrderRepository due to IO error", e);
            throw new UncheckedIOException("Failed to initialize FileOrderRepository", e);
        } catch (Exception e) {
            log.error("FATAL: Unexpected error during FileOrderRepository initialization", e);
            throw new RuntimeException("Unexpected error during FileOrderRepository initialization", e);
        }
    }

    /**
     * Проверяет размер текущего лог-файла и выполняет ротацию (переименование),
     * если размер превышает сконфигурированный лимит.
     */
    private void rotateLogFileIfNeeded() {
        // TODO: Использовать значения из PizzeriaConfig для enabled и maxSizeMb
        boolean rotationEnabled = true;
        long maxSizeMb = 10;

        if (!rotationEnabled) {
            log.info("Log rotation is disabled.");
            return;
        }
        if (!Files.exists(logFilePath)) {
             log.debug("Log file {} does not exist. No rotation needed.", logFilePath);
            return;
        }

        try {
            long fileSize = Files.size(logFilePath);
            long maxSizeInBytes = maxSizeMb * 1024 * 1024;

            if (fileSize >= maxSizeInBytes) {
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                Path archivePath = logFilePath.resolveSibling(logFilePath.getFileName().toString() + "." + timestamp + ".archive");
                log.warn("Log file size {} bytes >= limit {} bytes. Rotating to {}", fileSize, maxSizeInBytes, archivePath);

                synchronized (fileLock) {
                    if (logWriter != null) {
                        logWriter.close();
                        logWriter = null;
                        closed = true; // Помечаем как закрытый на время ротации
                        log.debug("Closed log writer before rotation.");
                    }
                }
                Files.move(logFilePath, archivePath, StandardCopyOption.REPLACE_EXISTING);
                log.info("Log file rotated successfully to {}", archivePath);
            } else {
                log.debug("Log file size {} bytes is within limit {} bytes. No rotation needed.", fileSize, maxSizeInBytes);
            }
        } catch (IOException e) {
            log.error("Error during log rotation for {}. Continuing initialization...", logFilePath, e);
        }
    }

    /**
     * Загружает состояние заказов из лог-файла.
     * Читает файл построчно, парсит JSON события, восстанавливает объекты Order
     * с помощью {@link OrderRecoveryAccessor} и распределяет их по кэшам.
     * Устанавливает счетчик {@code nextId}.
     */
    private void loadFromLog() {
        synchronized (stateLock) {
            this.isLoading = true;
        }
        log.info("Starting state loading process from {} (isLoading = true)...", logFilePath);

        if (!Files.exists(logFilePath)) {
             synchronized (stateLock) { this.isLoading = false; }
             log.info("Log file does not exist. Starting with empty state.");
             nextId.set(1);
             log.info("Finished state loading process (isLoading = false).");
            return;
        }

        Map<Integer, Order> tempOrderMap = new HashMap<>();
        int linesProcessed = 0;
        int maxIdFound = 0;
        long startTime = System.currentTimeMillis();

        try (BufferedReader reader = Files.newBufferedReader(logFilePath, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                 linesProcessed++;
                 fileReadCounter.increment();
                 if (line.trim().isEmpty()) continue;

                 try {
                     Map<String, Object> event = objectMapper.readValue(line, MAP_TYPE_REFERENCE);
                     String eventType = (String) event.get(EVENT_TYPE);
                     if (eventType == null) {
                         log.warn("Log line {} is missing eventType. Skipping.", linesProcessed);
                         continue;
                     }

                     if (TYPE_CREATE.equals(eventType)) {
                         @SuppressWarnings("unchecked")
                         Map<String, Object> orderData = (Map<String, Object>) event.get(EVENT_ORDER);
                         if (orderData != null) {
                             try {
                                 Order order = OrderRecoveryAccessor.createOrderFromData(orderData, objectMapper);
                                 if (tempOrderMap.putIfAbsent(order.getId(), order) == null) {
                                     maxIdFound = Math.max(maxIdFound, order.getId());
                                 } else {
                                     log.warn("Duplicate CREATE event for order ID {} on line {}. Ignoring subsequent CREATE.", order.getId(), linesProcessed);
                                 }
                             } catch (IllegalArgumentException e) {
                                 log.error("Failed to create order from data on line {}: {}", linesProcessed, e.getMessage(), e);
                             }
                         } else {
                              log.warn("CREATE event on line {} is missing order data. Skipping.", linesProcessed);
                         }
                     } else if (TYPE_STATUS_UPDATE.equals(eventType)) {
                         Object orderIdObj = event.get(EVENT_ORDER_ID);
                         String statusStr = (String) event.get(EVENT_NEW_STATUS);
                         if (orderIdObj instanceof Integer && statusStr != null) {
                             Integer orderId = (Integer) orderIdObj;
                             maxIdFound = Math.max(maxIdFound, orderId);
                             Order order = tempOrderMap.get(orderId);
                             if (order != null) {
                                 try {
                                     OrderStatus targetStatus = OrderStatus.valueOf(statusStr);
                                     OrderRecoveryAccessor.forceSetStatus(order, targetStatus);
                                     log.trace("Order {} status FORCED internally to {} from log via Accessor", orderId, targetStatus);
                                 } catch (IllegalArgumentException e) {
                                     log.warn("Invalid status '{}' found in log for order {} on line {}. Status not updated.", statusStr, orderId, linesProcessed);
                                 } catch (Exception e) {
                                     log.error("Error forcing status for order {} on line {}: {}", orderId, linesProcessed, e.getMessage(), e);
                                 }
                             } else {
                                  log.warn("STATUS_UPDATE event on line {} for unknown order ID {}. Ignoring.", linesProcessed, orderId);
                             }
                         } else {
                              log.warn("STATUS_UPDATE event on line {} is missing valid orderId or newStatus. Skipping.", linesProcessed);
                         }
                     } else {
                         log.warn("Unknown event type '{}' on log line {}. Skipping.", eventType, linesProcessed);
                     }
                 } catch (Exception e) {
                     log.error("Error processing log line {}: {}. Skipping line.", linesProcessed, line, e);
                 }
            }

            int activeCount = 0;
            int finalizedCount = 0;
            synchronized (cacheLock) {
                activeOrderCache.clear();
                finalizedOrderStatusCache.clear();
                for (Order order : tempOrderMap.values()) {
                    OrderStatus currentStatus = order.getStatus();
                    if (currentStatus == null) {
                        log.error("Order {} has null status after loading! Placing into finalized as DISCARDED.", order.getId());
                        finalizedOrderStatusCache.put(order.getId(), OrderStatus.DISCARDED);
                        finalizedCount++;
                    } else if (currentStatus.isFinal()) {
                        finalizedOrderStatusCache.put(order.getId(), currentStatus);
                        finalizedCount++;
                    } else {
                        activeOrderCache.put(order.getId(), order);
                        activeCount++;
                    }
                }
            }
            log.info("Distributed loaded orders: {} active, {} finalized.", activeCount, finalizedCount);

        } catch (IOException e) {
            log.error("Critical I/O error occurred while reading log file: {}. State might be incomplete.", logFilePath, e);
            if (maxIdFound == 0) nextId.set(1); else nextId.set(maxIdFound + 1);
        } finally {
            nextId.set(maxIdFound + 1);
            synchronized (stateLock) {
                this.isLoading = false;
            }
            long endTime = System.currentTimeMillis();
            log.info("Finished state loading process (isLoading = false). Processed {} lines. Next order ID set to {}. Total time: {} ms.",
                    linesProcessed, nextId.get(), (endTime - startTime));
        }
    }

    /**
     * Открывает {@link PrintWriter} для дозаписи в лог-файл.
     */
    private void openLogWriter() throws IOException {
        synchronized (fileLock) {
            if (this.logWriter != null && !this.closed) {
                log.warn("Log writer seems to be already open. Closing existing one before reopening.");
                try { this.logWriter.close(); } catch (Exception e) { log.error("Error closing previous writer", e); }
                this.logWriter = null;
            }
            OutputStream fos = Files.newOutputStream(logFilePath, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            this.logWriter = new PrintWriter(new OutputStreamWriter(fos, StandardCharsets.UTF_8), true); // autoFlush=true
            this.closed = false;
            log.info("Log file writer opened successfully for: {}", logFilePath);
        }
    }

    @Override
    public void addOrder(Order order) {
        Objects.requireNonNull(order, "Order cannot be null");
        addOrderCounter.increment();

        if (order.getStatus() != OrderStatus.RECEIVED) {
             log.error("Order {} provided to addOrder was not in expected RECEIVED state (was {})! This indicates an issue in order creation.", order.getId(), order.getStatus());
        }

        Map<String, Object> event = new HashMap<>();
        event.put(EVENT_TYPE, TYPE_CREATE);
        event.put(EVENT_TIMESTAMP, Instant.now().toString());
        event.put(EVENT_ORDER, order);

        try {
            logEvent(event); // Запись в лог

            synchronized (cacheLock) { // Добавление в кэш
                 if (activeOrderCache.containsKey(order.getId()) || finalizedOrderStatusCache.containsKey(order.getId())) {
                      log.error("Duplicate order ID {} detected when adding to cache. Order NOT added.", order.getId());
                      throw new IllegalStateException("Duplicate order ID " + order.getId() + " found in cache during addOrder.");
                 }
                 activeOrderCache.put(order.getId(), order);
            }
            log.info("Order added to repository and active cache.");

        } catch (RuntimeException e) {
            log.error("Failed to process or log CREATE event for order {}. Order NOT added to cache.", order.getId(), e);
             throw e;
        }
    }

    @Override
    public void logStatusUpdate(int orderId, OrderStatus newStatus) {
        synchronized (stateLock) {
            if (isLoading) {
                log.trace("Ignoring status update logging during loading phase for order {}", orderId);
                return;
            }
        }
        logStatusUpdateCounter.increment();
        log.info("Processing status update logging to {}", newStatus);

        Map<String, Object> event = new HashMap<>();
        event.put(EVENT_TYPE, TYPE_STATUS_UPDATE);
        event.put(EVENT_TIMESTAMP, Instant.now().toString());
        event.put(EVENT_ORDER_ID, orderId);
        event.put(EVENT_NEW_STATUS, newStatus.name());

        try {
            logEvent(event); // Запись в лог

            synchronized (cacheLock) { // Обновление кэшей
                if (newStatus.isFinal()) {
                    Order removedOrder = activeOrderCache.remove(orderId);
                    finalizedOrderStatusCache.put(orderId, newStatus);
                    if (removedOrder != null) {
                        log.debug("Order moved from active to finalized cache with status {}", newStatus);
                    } else {
                         if (finalizedOrderStatusCache.containsKey(orderId)) {
                            log.warn("Updated status for already finalized order {} to {}", orderId, newStatus);
                         } else {
                            log.error("Order {} not found in any cache when logging final status {}!", orderId, newStatus);
                         }
                    }
                } else {
                    Order orderInCache = activeOrderCache.get(orderId);
                    if (orderInCache != null) {
                         if (orderInCache.getStatus() != newStatus) {
                             log.error("CRITICAL: Status mismatch for order {} in active cache! Logged status: {}, Cache status: {}. Indicates inconsistency in calling code!",
                                     orderId, newStatus, orderInCache.getStatus());
                         } else {
                              log.debug("Order {} status confirmed as {} in active cache after logging.", orderId, newStatus);
                         }
                    } else {
                         if (finalizedOrderStatusCache.containsKey(orderId)) {
                              log.error("Attempted to log non-final status {} for order {} which is already finalized!", orderId, newStatus);
                         } else {
                              log.error("Order {} not found in active cache when logging non-final status {}!", orderId, newStatus);
                         }
                    }
                }
            }

        } catch (RuntimeException e) {
            log.error("FAILED to log status update to {}: {}", newStatus, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Сериализует и записывает событие в лог-файл. Синхронизирован.
     */
    private void logEvent(Map<String, Object> eventData) {
        synchronized (fileLock) {
            if (closed) {
                log.error("Attempted to write to log file, but repository is closed.");
                throw new IllegalStateException("Repository log writer is closed.");
            }
            if (logWriter == null) {
                 log.error("Attempted to write to log file, but writer is null (not initialized?).");
                throw new IllegalStateException("Repository log writer is not initialized.");
            }
            try {
                String jsonEvent = objectMapper.writeValueAsString(eventData);
                logWriter.println(jsonEvent);
                fileWriteCounter.increment();
                if (logWriter.checkError()) {
                    log.error("PrintWriter error flag set after writing log event. Disk full or other IO error?");
                    throw new UncheckedIOException("Failed to write to repository log file (PrintWriter error flag set)", new IOException("PrintWriter error"));
                }
                 log.trace("Logged event: {}", jsonEvent);
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize log event data: {}", eventData, e);
                throw new RuntimeException("Failed to serialize log event", e);
            } catch (UncheckedIOException | IllegalStateException e) {
                 throw e;
            } catch (Exception e) {
                 log.error("Unexpected error writing log event", e);
                 throw new RuntimeException("Unexpected error writing log event", e);
            }
        }
    }

    @Override
    public Order getOrderById(int orderId) {
        getOrderCounter.increment();
        log.debug("Getting order by ID");

        synchronized (cacheLock) {
            Order order = activeOrderCache.get(orderId);
            if (order != null) {
                 log.trace("Order found in active cache.");
                 return order; // TODO: Consider returning a copy for true isolation
            }

            OrderStatus finalStatus = finalizedOrderStatusCache.get(orderId);
            if (finalStatus != null) {
                log.trace("Order found in finalized cache with status {}. Returning null as per getOrderById contract.", finalStatus);
                return null;
            }
        }
        log.debug("Order not found in any cache.");
        return null;
    }

    @Override
    public Collection<Order> getAllOrders() {
        log.debug("Getting all active orders.");
        synchronized (cacheLock) {
            // TODO: Consider creating copies of Order objects for full isolation.
            return new ArrayList<>(activeOrderCache.values());
        }
    }

    @Override
    public int count() {
        synchronized (cacheLock) {
            return activeOrderCache.size() + finalizedOrderStatusCache.size();
        }
    }

    @Override
    public int getNextOrderId() {
        return nextId.getAndIncrement();
    }

    /**
     * Закрывает {@link PrintWriter} для лог-файла.
     */
    @PreDestroy
    @Override
    public void close() {
        synchronized (fileLock) {
            if (!closed) {
                log.info("FileOrderRepository @PreDestroy: Closing resources for: {}", logFilePath);
                if (logWriter != null) {
                    try {
                        logWriter.close();
                        if (logWriter.checkError()) {
                             log.error("Error flag set on log writer during close() for: {}. Data might be lost.", logFilePath);
                        }
                    } catch (Exception e) {
                         log.error("Exception while closing log writer for: {}", logFilePath, e);
                    } finally {
                        logWriter = null;
                    }
                }
                closed = true;
                log.info("FileOrderRepository resources closed.");
            } else {
                 log.debug("FileOrderRepository already closed.");
            }
       }
    }

    @Override
    public Optional<OrderStatus> findOrderStatusById(int orderId) {
        getOrderCounter.increment();
        log.debug("Finding order status by ID");

        synchronized (cacheLock) {
            Order order = activeOrderCache.get(orderId);
            if (order != null) {
                log.trace("Order found in active cache. Status: {}", order.getStatus());
                return Optional.of(order.getStatus());
            }

            OrderStatus finalStatus = finalizedOrderStatusCache.get(orderId);
            if (finalStatus != null) {
                log.trace("Order found in finalized cache with status {}.", finalStatus);
                return Optional.of(finalStatus);
            }
        }
        log.debug("Order not found in any cache for status check.");
        return Optional.empty();
    }
}