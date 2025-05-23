package org.example.pizzeria.shared.lifecycle;

import jakarta.annotation.PreDestroy;

import org.example.pizzeria.config.PizzeriaConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Управляет жизненным циклом приложения, включая запланированную остановку
 * на основе конфигурации workTimeMs.
 */
@Component
public class ApplicationLifecycleManager implements ApplicationListener<ContextRefreshedEvent> {

    private static final Logger log = LoggerFactory.getLogger(ApplicationLifecycleManager.class);

    private final PizzeriaConfig config;
    private final ApplicationContext applicationContext;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(
            runnable -> {
                Thread thread = Executors.defaultThreadFactory().newThread(runnable);
                thread.setName("AppShutdownScheduler");
                thread.setDaemon(false); // Не демон, чтобы гарантировать выполнение
                return thread;
            });
    // Флаг, чтобы планировщик сработал только один раз
    private final AtomicBoolean shutdownScheduled = new AtomicBoolean(false);

    public ApplicationLifecycleManager(PizzeriaConfig config, ApplicationContext applicationContext) {
        this.config = config;
        this.applicationContext = applicationContext;
        log.info("ApplicationLifecycleManager created.");
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        // Убедимся, что это событие корневого контекста и планировщик еще не запускался
        if (event.getApplicationContext().equals(this.applicationContext) &&
            shutdownScheduled.compareAndSet(false, true)) {

            long shutdownDelay = config.workTimeMs();

            if (shutdownDelay > 0) {
                log.info("Application context refreshed. Scheduling application shutdown in {} ms.", shutdownDelay);
                scheduler.schedule(this::initiateShutdown, shutdownDelay, TimeUnit.MILLISECONDS);
            } else {
                log.info("Automatic shutdown based on workTimeMs is disabled (value <= 0). Application will run until stopped externally.");
                shutdownScheduler(); // Останавливаем планировщик, если авто-выключение не нужно
            }
        }
    }

    private void initiateShutdown() {
        log.info("Scheduled work time ({} ms) elapsed. Initiating application shutdown...", config.workTimeMs());
        try {
            int exitCode = SpringApplication.exit(applicationContext, () -> 0); // Код 0 для успешного завершения
            log.info("SpringApplication.exit called. Exiting with code {}.", exitCode);
            // System.exit(exitCode); // Обычно не требуется, exit() должен завершать
        } catch (Exception e) {
            log.error("Error during scheduled shutdown initiation. Forcing exit.", e);
            System.exit(1); // Принудительный выход с кодом ошибки
        } finally {
             shutdownScheduler(); // Останавливаем планировщик после попытки завершения
        }
    }

    /**
     * Корректно останавливает ScheduledExecutorService.
     * Вызывается либо после срабатывания таймера, либо при штатном завершении (@PreDestroy).
     */
    @PreDestroy
    public void shutdownScheduler() {
        if (!scheduler.isShutdown()) {
             log.info("Shutting down the application shutdown scheduler.");
             scheduler.shutdown();
             try {
                  if (!scheduler.awaitTermination(2, TimeUnit.SECONDS)) { // Даем время на завершение
                       log.warn("Shutdown scheduler did not terminate gracefully after 2 seconds. Forcing shutdown.");
                       scheduler.shutdownNow();
                  }
             } catch (InterruptedException e) {
                  log.warn("Interrupted while waiting for shutdown scheduler to terminate.");
                  scheduler.shutdownNow();
                  Thread.currentThread().interrupt();
             }
        }
    }
}