package org.example.pizzeria.processing.service.impl;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import org.example.pizzeria.domain.worker.Worker;
import org.example.pizzeria.processing.service.WorkerManagerService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Сервис, отвечающий за запуск, управление и остановку потоков работников (пекари, курьеры).
 * Использует базовые Thread и Runnable.
 */
@Service
public class WorkerManagerServiceImpl implements WorkerManagerService {

    private static final Logger log = LoggerFactory.getLogger(WorkerManagerServiceImpl.class);
    private static final long WORKER_JOIN_TIMEOUT_MS = 2000;

    private final List<Runnable> workers;
    private final List<Thread> workerThreads = new ArrayList<>();

    /**
     * Конструктор для внедрения списка всех работников.
     * @param allWorkers Список Runnable, содержащий всех пекарей и курьеров.
     */
    public WorkerManagerServiceImpl(@Qualifier("allWorkers") List<Runnable> allWorkers) {
        this.workers = allWorkers;
        log.info("WorkerManagerService created with {} workers injected.", allWorkers.size());
    }

    /**
     * Запускает потоки для всех работников после инициализации Spring контекста.
     */
    @PostConstruct
    public void startWorkers() {
        log.info("Starting worker threads...");
        if (workers == null || workers.isEmpty()) {
            log.warn("No workers configured or injected. Nothing to start.");
            return;
        }

        synchronized (workerThreads) {
             workerThreads.clear();
             for (Runnable worker : workers) {
                 String threadName = generateThreadName(worker);
                 Thread thread = new Thread(worker, threadName);
                 workerThreads.add(thread);
                 thread.start();
                 log.info("Started thread: {}", threadName);
             }
        }
        log.info("All worker threads ({}) started.", workerThreads.size());
    }

    /**
     * Генерирует имя для потока работника.
     */
    private String generateThreadName(Runnable worker) {
        if (worker instanceof Worker workerInterface) {
            return worker.getClass().getSimpleName() + "-" + workerInterface.getId();
        } else {
            return worker.getClass().getSimpleName() + "-" + worker.hashCode();
        }
    }

    /**
     * Останавливает все потоки работников перед уничтожением бина.
     */
    @PreDestroy
    public void stopWorkers() {
        log.info("Stopping all worker threads...");
        List<Thread> threadsToStop;
        synchronized (workerThreads) {
             threadsToStop = new ArrayList<>(workerThreads);
        }

        if (threadsToStop.isEmpty()) {
            log.info("No worker threads are currently running.");
            return;
        }

        log.info("Interrupting {} worker threads...", threadsToStop.size());
        for (Thread thread : threadsToStop) {
            if (thread != null && thread.isAlive()) {
                 log.debug("Interrupting thread: {}", thread.getName());
                 thread.interrupt();
            }
        }
        log.info("Interrupt signals sent to all workers.");

        log.info("Waiting for worker threads to join (timeout: {}ms)...", WORKER_JOIN_TIMEOUT_MS);
        for (Thread thread : threadsToStop) {
             if (thread == null) continue;
             try {
                 thread.join(WORKER_JOIN_TIMEOUT_MS);
                 if (thread.isAlive()) {
                      log.warn("Worker thread {} did not finish within {}ms timeout!", thread.getName(), WORKER_JOIN_TIMEOUT_MS);
                 } else {
                      log.info("Worker thread {} finished.", thread.getName());
                 }
             } catch (InterruptedException e) {
                  log.warn("Interrupted while waiting for worker thread {} to join.", thread.getName());
                  Thread.currentThread().interrupt();
             } catch (Exception e) {
                  log.error("Error joining worker thread {}", thread.getName(), e);
             }
        }
        log.info("Finished stopping worker threads.");
         synchronized (workerThreads) {
             workerThreads.clear();
         }
    }

    @Override
    public int getActiveWorkerThreadCount() {
         synchronized(workerThreads) {
              return (int) workerThreads.stream().filter(t -> t != null && t.isAlive()).count();
         }
    }
}